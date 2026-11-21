package ai.gsmc.fundamentals.survey;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A dish and its console that task a satellite and take its downlink, as Landsat's ground stations do: the satellite images the
 * target on its next pass and sends the frame down. Paper takes a picture, a spyglass sets the zoom, a filled map the target and a
 * clock a recording, which drops a frame into an adjacent container every so often for as long as that container has paper.
 */
public class GroundStationBlockEntity extends SurveyBlockEntity {

    /** Minutes between frames while recording; 0 is not recording. */
    public static final int[] INTERVALS = {0, 1, 5, 20};

    private UUID owner;
    private int satellite = -1, scale, interval, countdown;
    private boolean targeted;
    private int targetX, targetZ;
    private boolean inView;
    private String status = "";

    public GroundStationBlockEntity(BlockPos pos, BlockState state) {
        super(Survey.groundStationEntity(), pos, state);
    }

    public int scale() { return scale; }
    public int interval() { return INTERVALS[interval]; }
    public int targetX() { return targeted ? targetX : worldPosition.getX(); }
    public int targetZ() { return targeted ? targetZ : worldPosition.getZ(); }

    /** The satellite this station is tasking, if its owner still has it. */
    public Optional<Satellites.Satellite> satellite(ServerLevel level) {
        return owner == null ? Optional.empty()
                : Satellites.get(level.getServer()).byId(satellite).filter(s -> s.owner().equals(owner));
    }

    /** Tasks the player's next satellite: the first, or the one after the one tasked now. */
    public Optional<Satellites.Satellite> cycle(ServerLevel level, Player player) {
        List<Satellites.Satellite> owned = Satellites.get(level.getServer()).owned(player.getUUID());
        if (owned.isEmpty()) return Optional.empty();
        int now = player.getUUID().equals(owner) ? owned.indexOf(satellite(level).orElse(null)) : -1;
        Satellites.Satellite next = owned.get((now + 1) % owned.size());
        owner = player.getUUID();
        satellite = next.id();
        changed();
        return Optional.of(next);
    }

    public void zoom() {
        scale = (scale + 1) % (MapItemSavedData.MAX_SCALE + 1);
        changed();
    }

    public void target(int x, int z) {
        targeted = true;
        targetX = x;
        targetZ = z;
        changed();
    }

    public void overhead() {
        targeted = false;
        changed();
    }

    public void cycleRecording() {
        interval = (interval + 1) % INTERVALS.length;
        countdown = INTERVALS[interval] * 60;
        status = "";
        changed();
    }

    private void changed() {
        setChanged();
        if (!(level instanceof ServerLevel server)) return;
        boolean was = inView;
        look(server);
        server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        if (was != inView) server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private void look(ServerLevel level) {
        inView = satellite(level).filter(s -> s.sees(targetX(), targetZ())).isPresent();
    }

    /**
     * A frame of the target at the station's zoom, or nothing, with {@link #status()} saying why: no open sky, not on the Overworld, no
     * satellite tasked or the target beyond its view.
     */
    public ItemStack image(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return refuse("earth");
        if (!level.canSeeSky(worldPosition.above())) return refuse("sky");
        Optional<Satellites.Satellite> tasked = satellite(level);
        if (tasked.isEmpty()) return refuse("none");
        Satellites.Satellite sat = tasked.get();
        int x = targetX(), z = targetZ();
        if (!sat.sees(x, z)) return refuse("view");
        status = "";
        return photograph(level, sat, x, z, scale);
    }

    private ItemStack refuse(String why) {
        status = why;
        return ItemStack.EMPTY;
    }

    public String status() { return status; }

    /** A locked map centred on the target: a frame is a picture of a moment, so it is not redrawn as the holder walks about. */
    static ItemStack photograph(ServerLevel level, Satellites.Satellite satellite, int x, int z, int scale) {
        // built from its saved form, since a fresh map snaps its centre to the map grid and the target would fall anywhere in it
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", Level.OVERWORLD.location().toString());
        tag.putInt("xCenter", x);
        tag.putInt("zCenter", z);
        tag.putByte("scale", (byte) scale);
        tag.putBoolean("trackingPosition", true);
        tag.putBoolean("locked", true);
        MapItemSavedData data = MapItemSavedData.load(tag, level.registryAccess());
        AerialCameraItem.develop(level, data);
        var id = level.getFreeMapId();
        level.setMapData(id, data);
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponents.MAP_ID, id);
        map.set(DataComponents.ITEM_NAME, Component.translatable("item.fundamentals.satellite_image", satellite.label()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("item.fundamentals.satellite_image.frame", x, z, 128 << scale, level.getDayTime() / 24000)
                .withStyle(ChatFormatting.DARK_AQUA));
        map.set(DataComponents.LORE, new ItemLore(AerialCameraItem.margin(level, new BlockPos(x, 0, z), 1 << scale, lore)));
        return map;
    }

    /** One recorded frame: paper from an adjacent container, the map back into it. Recording stops when the paper runs out. */
    public boolean record(ServerLevel level) {
        for (Direction side : Direction.values()) {
            IItemHandler store = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
            if (store == null) continue;
            for (int slot = 0; slot < store.getSlots(); slot++) {
                if (!store.extractItem(slot, 1, true).is(Items.PAPER)) continue;
                ItemStack frame = image(level);
                if (frame.isEmpty()) return false;
                store.extractItem(slot, 1, false);
                ItemStack left = ItemHandlerHelper.insertItem(store, frame, false);
                if (!left.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, left);
                return true;
            }
        }
        interval = 0;
        status = "paper";
        return false;
    }

    @Override
    protected void read(ServerLevel level) {
        look(level);
        if (interval == 0 || --countdown > 0) return;
        countdown = INTERVALS[interval] * 60;
        record(level);
    }

    /** Full while the tasked satellite has the target in view. */
    @Override
    public double fraction() {
        return inView ? 1 : 0;
    }

    @Override
    protected void describe(List<Component> lines) {
        String key = "goggles.fundamentals.ground_station.";
        lines.add(Component.translatable(key + (satellite < 0 ? "untasked" : inView ? "in_view" : "out_of_view"))
                .withStyle(inView ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable(key + (targeted ? "target" : "overhead"), targetX(), targetZ(), 128 << scale, scale)
                .withStyle(ChatFormatting.GRAY));
        if (interval > 0) {
            lines.add(Component.translatable(key + "recording", INTERVALS[interval], countdown).withStyle(ChatFormatting.GOLD));
        } else if (status.equals("paper")) {
            lines.add(Component.translatable(key + "paper").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    protected void save(CompoundTag tag) {
        if (owner != null) tag.putUUID("owner", owner);
        tag.putInt("satellite", satellite);
        tag.putInt("scale", scale);
        tag.putInt("interval", interval);
        tag.putInt("countdown", countdown);
        tag.putBoolean("targeted", targeted);
        tag.putInt("target_x", targetX);
        tag.putInt("target_z", targetZ);
        tag.putBoolean("in_view", inView);
        tag.putString("status", status);
    }

    @Override
    protected void load(CompoundTag tag) {
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        satellite = tag.contains("satellite") ? tag.getInt("satellite") : -1;
        scale = tag.getInt("scale");
        interval = Math.floorMod(tag.getInt("interval"), INTERVALS.length);
        countdown = tag.getInt("countdown");
        targeted = tag.getBoolean("targeted");
        targetX = tag.getInt("target_x");
        targetZ = tag.getInt("target_z");
        inView = tag.getBoolean("in_view");
        status = tag.getString("status");
    }
}
