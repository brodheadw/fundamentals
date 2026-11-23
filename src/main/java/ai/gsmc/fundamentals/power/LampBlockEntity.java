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
 * discharge lamp (sodium, mercury) strikes at once but glows only dimly until its metal is boiled; cut off while hot, the
 * arc tube's pressure is too high to restrike until it has cooled. A filament bulb lights at once and follows the voltage.
 */
public class LampBlockEntity extends ElectricBlockEntity implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    public static final int MIN_VOLTS = 100, STRIKE_MIN = 8, STRIKE_MAX = 32, RATED_VOLTS = 120, FILAMENT_MIN_VOLTS = 20;

    /** Fluorescent: ticks of the starter still to go, 0 once struck, -1 when out. An arc: warmth 0 to its kind's warm. */
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
        return canWork() && getData().getVoltage() >= (kind().start == LampBlock.Start.FILAMENT ? FILAMENT_MIN_VOLTS : MIN_VOLTS);
    }

    /** Past the starter's flicker (a tube), fully warm (an arc), or simply fed (a filament). */
    public boolean steady() {
        return live() && switch (kind().start) {
            case STARTER -> stage == 0;
            case WARMS -> stage >= kind().warm;
            case FILAMENT -> true;
        };
    }

    @Override
    public float resistance() {
        return kind().ohms;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) return;
        int light = switch (kind().start) {
            case STARTER -> fluorescent(server);
            case WARMS -> arc();
            case FILAMENT -> filament();
        };
        BlockState state = getBlockState();
        // the diffuser passes only so much of it
        light = light * LampBlock.passes(state.getValue(LampBlock.COLOR)) / 15;
        if (state.getValue(LampBlock.LIGHT) != light) level.setBlock(worldPosition, state.setValue(LampBlock.LIGHT, light), 3);
    }

    private int fluorescent(ServerLevel server) {
        if (!live()) {
            stage = -1;
            return 0;
        }
        if (stage < 0) stage = STRIKE_MIN + server.random.nextInt(STRIKE_MAX - STRIKE_MIN);
        if (stage == 0) return kind().full;
        stage--;
        if (stage % 4 == 0) server.playSound(null, worldPosition, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.15F, 1.8F);
        return stage == 0 || server.random.nextFloat() < 0.35F ? kind().full : 0;
    }

    private int filament() {
        if (!live()) return 0;
        // a filament's light falls off far faster than its voltage: about the 3.4th power, so half the volts gives a tenth the light
        double v = Math.min(1.0, getData().getVoltage() / (double) RATED_VOLTS);
        return Math.max(1, (int) Math.round(kind().full * Math.pow(v, 3.4)));
    }

    private int arc() {
        LampBlock.Kind kind = kind();
        if (!live()) {
            if (stage > 0) cooling = kind.cool;
            stage = 0;
            if (cooling > 0) cooling--;
            return 0;
        }
        if (cooling > 0) {
            cooling--;
            return 0;
        }
        if (stage < kind.warm) stage++;
        return kind.glow + (kind.full - kind.glow) * stage / kind.warm;
    }

    @Override
    public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        int volts = getData().getVoltage();
        String status = !live() ? cooling > 0 ? "cooling" : "unpowered" : switch (kind().start) {
            case STARTER -> stage == 0 ? "lit" : "striking";
            case WARMS -> stage < kind().warm ? "warming" : "lit";
            case FILAMENT -> "lit";
        };
        ai.gsmc.fundamentals.goggles.Readout.status(tooltip, getBlockState().getBlock().getName(), status);
        ai.gsmc.fundamentals.goggles.Readout.bar(tooltip, "voltage", volts, 2 * MIN_VOLTS, volts >= MIN_VOLTS, volts + " V");
        if (!live()) ai.gsmc.fundamentals.goggles.Readout.needs(tooltip, "volts", kind().start == LampBlock.Start.FILAMENT ? FILAMENT_MIN_VOLTS : MIN_VOLTS);
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
