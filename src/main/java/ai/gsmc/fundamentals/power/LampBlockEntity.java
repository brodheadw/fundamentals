package ai.gsmc.fundamentals.power;

import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A lamp's life on the grid. A fluorescent tube will not light on its own: the starter heats the electrodes and breaks the
 * circuit, again and again, until the ballast's kick strikes the arc; so a tube flickers a second or so before it holds. A
 * sodium lamp strikes at once but glows only dimly pink until its sodium is boiled, and is fully bright in ten seconds; cut
 * off while hot, the arc tube's pressure is too high to restrike until it has cooled.
 */
public class LampBlockEntity extends ElectricBlockEntity implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    public static final int MIN_VOLTS = 100, STRIKE_MIN = 8, STRIKE_MAX = 32, WARM = 200, COOL = 100;

    /** Fluorescent: ticks of the starter still to go, 0 once struck, -1 when out. Sodium: warmth 0 to WARM. */
    private int stage = -1;
    /** Sodium only: ticks it must still cool before it will strike again. */
    private int cooling;

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(Electricity.lampEntity(), pos, state);
    }

    LampBlock.Kind kind() {
        return ((LampBlock) getBlockState().getBlock()).kind;
    }

    public boolean live() {
        return canWork() && getData().getVoltage() >= MIN_VOLTS;
    }

    /** Past the starter's flicker (a tube) or fully warm (sodium). */
    public boolean steady() {
        return live() && (kind() == LampBlock.Kind.SODIUM ? stage >= WARM : stage == 0);
    }

    @Override
    public float resistance() {
        return kind().ohms;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) return;
        int light = kind() == LampBlock.Kind.SODIUM ? sodium() : fluorescent(server);
        BlockState state = getBlockState();
        if (state.getValue(LampBlock.LIGHT) != light) level.setBlock(worldPosition, state.setValue(LampBlock.LIGHT, light), 3);
    }

    private int fluorescent(ServerLevel server) {
        if (!live()) {
            stage = -1;
            return 0;
        }
        if (stage < 0) stage = STRIKE_MIN + server.random.nextInt(STRIKE_MAX - STRIKE_MIN);
        if (stage == 0) return 15;
        stage--;
        if (stage % 4 == 0) server.playSound(null, worldPosition, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.15F, 1.8F);
        return stage == 0 || server.random.nextFloat() < 0.35F ? 15 : 0;
    }

    private int sodium() {
        if (!live()) {
            if (stage > 0) cooling = COOL;
            stage = 0;
            if (cooling > 0) cooling--;
            return 0;
        }
        if (cooling > 0) {
            cooling--;
            return 0;
        }
        if (stage < WARM) stage++;
        return 2 + 13 * stage / WARM;
    }

    @Override
    public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        int volts = getData().getVoltage();
        boolean sodium = kind() == LampBlock.Kind.SODIUM;
        String status = !live() ? cooling > 0 ? "cooling" : "unpowered" : sodium ? stage < WARM ? "warming" : "lit" : stage == 0 ? "lit" : "striking";
        ai.gsmc.fundamentals.goggles.Readout.status(tooltip, getBlockState().getBlock().getName(), status);
        ai.gsmc.fundamentals.goggles.Readout.bar(tooltip, "voltage", volts, 2 * MIN_VOLTS, volts >= MIN_VOLTS, volts + " V");
        if (!live()) ai.gsmc.fundamentals.goggles.Readout.needs(tooltip, "volts", MIN_VOLTS);
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Stage", stage);
        tag.putInt("Cooling", cooling);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        stage = tag.contains("Stage") ? tag.getInt("Stage") : -1;
        cooling = tag.getInt("Cooling");
    }
}
