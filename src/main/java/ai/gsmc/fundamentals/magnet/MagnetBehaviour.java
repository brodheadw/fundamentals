package ai.gsmc.fundamentals.magnet;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class MagnetBehaviour extends BlockEntityBehaviour {

    public static final BehaviourType<MagnetBehaviour> TYPE = new BehaviourType<>("fundamentals_magnet");
    public static final int INTERVAL = 100;

    private final Runnable onOutputChanged;
    private MagnetCharge charge = MagnetCharge.UNGRADED;
    private float output = 1;
    private int celsius = Integer.MIN_VALUE;

    public MagnetBehaviour(SmartBlockEntity be, Runnable onOutputChanged) {
        super(be);
        this.onOutputChanged = onOutputChanged;
    }

    public static float output(BlockEntity be) {
        MagnetBehaviour magnet = be == null ? null : BlockEntityBehaviour.get(be, TYPE);
        return magnet == null ? 1 : magnet.output;
    }

    public static float field(BlockEntity be) {
        MagnetBehaviour magnet = be == null ? null : BlockEntityBehaviour.get(be, TYPE);
        return magnet == null ? 1 : magnet.output / magnet.charge.grade().strength;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public MagnetCharge charge() {
        return charge;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!getWorld().isClientSide) {
            check();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClientSide && (getWorld().getGameTime() + getPos().hashCode()) % INTERVAL == 0) {
            check();
        }
    }

    public void check() {
        MagnetCharge before = blockEntity.components().getOrDefault(Magnets.CHARGE, MagnetCharge.UNGRADED);
        double here = Heat.at(getWorld(), getPos());
        MagnetCharge after = before.heatedTo(here);
        if (after != before) {
            blockEntity.setComponents(DataComponentMap.builder().addAll(blockEntity.components()).set(Magnets.CHARGE, after).build());
            blockEntity.setChanged();
        }
        float was = output;
        int wasCelsius = celsius;
        charge = after;
        output = after.output(here);
        celsius = (int) Math.round(here);
        if (output != was) {
            onOutputChanged.run();
        }
        if (output != was || celsius != wasCelsius || after != before) {
            blockEntity.sendData();
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        if (clientPacket) {
            nbt.putString("MagnetGrade", charge.grade().getSerializedName());
            nbt.putFloat("MagnetField", charge.field());
            nbt.putFloat("MagnetOutput", output);
            nbt.putInt("MagnetCelsius", celsius);
        }
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        if (clientPacket && nbt.contains("MagnetGrade")) {
            MagnetGrade grade = MagnetGrade.CODEC.byName(nbt.getString("MagnetGrade"));
            charge = new MagnetCharge(grade == null ? MagnetGrade.DY_NDFEB : grade, nbt.getFloat("MagnetField"));
            output = nbt.getFloat("MagnetOutput");
            celsius = nbt.getInt("MagnetCelsius");
        }
    }

    public void addToGoggleTooltip(List<Component> tooltip) {
        MagnetGrade grade = charge.grade();
        tooltip.add(indent(Component.translatable("goggles.fundamentals.magnet.grade", Magnets.name(grade), grade.maxOperating).withStyle(ChatFormatting.GRAY)));
        if (charge.demagnetised()) {
            tooltip.add(indent(Component.translatable("goggles.fundamentals.magnet.demagnetised", grade.curie).withStyle(ChatFormatting.RED)));
            return;
        }
        ChatFormatting colour = celsius > grade.damage ? ChatFormatting.RED : celsius > grade.maxOperating ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        tooltip.add(indent(Component.translatable("goggles.fundamentals.magnet.output", celsius, Math.round(output * 100)).withStyle(colour)));
        if (charge.field() < 1) {
            tooltip.add(indent(Component.translatable("goggles.fundamentals.magnet.field", Math.round(charge.field() * 100), grade.damage).withStyle(ChatFormatting.GOLD)));
        }
    }

    private static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }
}
