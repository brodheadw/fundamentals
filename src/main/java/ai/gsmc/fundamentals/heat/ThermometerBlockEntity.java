package ai.gsmc.fundamentals.heat;

import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.uses.Uses;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.List;

public class ThermometerBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final int PERIOD = 10;
    private static final DustParticleOptions SPIRIT_SPRAY = new DustParticleOptions(new Vector3f(0.9F, 0.1F, 0.1F), 0.8F);

    private double celsius;
    private boolean read;
    private int cooldown;
    private float dial, prevDial;

    public ThermometerBlockEntity(BlockPos pos, BlockState state) {
        super(Thermometers.entity(), pos, state);
    }

    public Thermometer kind() {
        return ((ThermometerBlock) getBlockState().getBlock()).kind();
    }

    public Direction facing() {
        return getBlockState().getValue(ThermometerBlock.FACING);
    }

    public BlockPos sensed() {
        return worldPosition.relative(facing().getOpposite());
    }

    public double celsius() {
        return celsius;
    }

    public float dial(float partialTick) {
        return prevDial + (dial - prevDial) * partialTick;
    }

    public int signal() {
        return read ? kind().signal(celsius) : 0;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, ThermometerBlockEntity gauge) {
        if (gauge.cooldown-- > 0) {
            return;
        }
        gauge.cooldown = PERIOD;
        double now = Heat.at(level, gauge.sensed());
        Thermometer kind = gauge.kind();
        if (kind.bursts && now > kind.max) {
            gauge.burst((ServerLevel) level, kind);
            return;
        }
        if (gauge.read && Math.abs(now - gauge.celsius) < 0.5) {
            return;
        }
        int before = gauge.signal();
        gauge.celsius = now;
        gauge.read = true;
        gauge.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        if (gauge.signal() != before) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, ThermometerBlockEntity gauge) {
        Thermometer kind = gauge.kind();
        float target = (float) kind.fraction(gauge.celsius);
        if (gauge.celsius > kind.max) {
            target += 0.015F + level.random.nextFloat() * 0.02F;
        }
        gauge.prevDial = gauge.dial;
        gauge.dial += (target - gauge.dial) * 0.125F;
    }

    private void burst(ServerLevel level, Thermometer kind) {
        BlockPos pos = worldPosition;
        level.destroyBlock(pos, false);
        if (kind == Thermometer.MERCURY) {
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            Block.popResource(level, pos, new ItemStack(Uses.mercury()));
            Hazards.mercuryVapour(level, pos);
        } else {
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6F, 1.6F);
            level.sendParticles(SPIRIT_SPRAY, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.0);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Thermometer kind = kind();
        String key = "goggles.fundamentals.thermometer.";
        tooltip.add(indent(getBlockState().getBlock().getName().withStyle(ChatFormatting.GRAY)));
        if (celsius > kind.max) {
            tooltip.add(indent(Component.translatable(key + "over", whole(kind.max)).withStyle(ChatFormatting.RED)));
        } else if (celsius < kind.min) {
            tooltip.add(indent(Component.translatable(key + "under", whole(kind.min)).withStyle(ChatFormatting.AQUA)));
        } else {
            tooltip.add(indent(Component.translatable("heat.fundamentals.readout", whole(celsius), whole(Heat.fahrenheit(celsius)))));
        }
        tooltip.add(indent(Component.translatable(key + "range", whole(kind.min), whole(kind.max)).withStyle(ChatFormatting.DARK_GRAY)));
        return true;
    }

    private static String whole(double value) {
        return String.format("%,.0f", value);
    }

    private static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("celsius", celsius);
        tag.putBoolean("read", read);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        celsius = tag.getDouble("celsius");
        read = tag.getBoolean("read");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
