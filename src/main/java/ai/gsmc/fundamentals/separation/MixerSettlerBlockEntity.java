package ai.gsmc.fundamentals.separation;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class MixerSettlerBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY_PER_CASING = 250;
    public static final int BATCH_PER_CASING = 10;
    public static final int PERIOD = 100;
    public static final int EQUILIBRATION_PER_STAGE = 40;
    static final int STIR_TICKS = 30;
    private static final int STALL_INTERVAL = 10;

    final Tank organic = new Tank();
    final Tank aqueous = new Tank();
    final Tank out = new Tank();
    final Tank waste = new Tank();
    @Nullable
    BlockPos controller;
    int across = 1;
    int along = 1;
    int tall = 1;
    int stirring;
    int cooldown;
    int settled;
    int crud;
    boolean dirty;
    static int formations;
    @Nullable
    private Battery battery;
    private int batteryFormations;
    @Nullable
    private Optional<Stall> stall;

    public MixerSettlerBlockEntity(BlockPos pos, BlockState state) {
        super(Separation.mixerSettlerEntity(), pos, state);
    }

    class Tank extends FluidTank {
        Tank() {
            super(CAPACITY_PER_CASING);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
            dirty = true;
        }
    }

    public FluidStack organic() { return organic.getFluid(); }
    public FluidStack aqueous() { return aqueous.getFluid(); }
    public boolean stirring() { return stirring > 0; }
    public int across() { return across; }
    public int along() { return along; }
    public int tall() { return tall; }
    public int volume() { return across * along * tall; }
    public int capacity() { return CAPACITY_PER_CASING * volume(); }
    public int phaseCapacity() { return capacity() / 2; }
    public int batch() { return BATCH_PER_CASING * volume(); }
    public boolean isController() { return controller == null || worldPosition.equals(controller); }
    public boolean isStage() { MixerSettlerBlockEntity s = stage(); return s.across >= 3 && s.along >= 3; }
    public BlockPos controllerPos() { return controller == null ? worldPosition : controller; }

    public float aqueousFill() { return (float) aqueous.getFluidAmount() / phaseCapacity(); }
    public float organicFill() { return (float) organic.getFluidAmount() / phaseCapacity(); }

    public double surface() {
        MixerSettlerBlockEntity stage = stage();
        return VatGeometry.get().surface(stage.tall, stage.aqueousFill(), stage.organicFill());
    }

    public double floorY() {
        return stage().worldPosition.getY();
    }

    public Direction facing() {
        return getBlockState().getValue(MixerSettlerBlock.FACING);
    }

    Direction right() {
        return facing().getClockWise();
    }

    @Nullable
    MixerSettlerBlockEntity casingAt(BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MixerSettlerBlockEntity casing && casing.facing() == facing() ? casing : null;
    }

    public MixerSettlerBlockEntity stage() {
        if (controller != null) {
            MixerSettlerBlockEntity head = casingAt(controller);
            if (head == null || !head.isController() || !head.covers(worldPosition)) {
                becomeSingle();
            }
        }
        return controller == null ? this : casingAt(controller);
    }

    private boolean covers(BlockPos pos) {
        BlockPos rel = pos.subtract(worldPosition);
        Direction right = right();
        int a = rel.getX() * right.getStepX() + rel.getZ() * right.getStepZ();
        int l = rel.getX() * facing().getStepX() + rel.getZ() * facing().getStepZ();
        return a >= 0 && a < across && l >= 0 && l < along && rel.getY() >= 0 && rel.getY() < tall;
    }

    BlockPos cell(BlockPos origin, int a, int l, int u) {
        return origin.relative(right(), a).relative(facing(), l).above(u);
    }

    void becomeSingle() {
        formations++;
        controller = null;
        across = along = tall = 1;
        resize();
        setChanged();
    }

    void resize() {
        organic.setCapacity(phaseCapacity());
        aqueous.setCapacity(phaseCapacity());
        out.setCapacity(capacity());
        waste.setCapacity(phaseCapacity());
    }

    void empty() {
        organic.setFluid(FluidStack.EMPTY);
        aqueous.setFluid(FluidStack.EMPTY);
        out.setFluid(FluidStack.EMPTY);
        waste.setFluid(FluidStack.EMPTY);
        stirring = 0;
    }

    public AABB stageBox() {
        return AABB.encapsulatingFullBlocks(worldPosition, cell(worldPosition, across - 1, along - 1, tall - 1));
    }

    @Nullable
    MixerSettlerBlockEntity nextStage(boolean ahead) {
        MixerSettlerBlockEntity me = stage();
        MixerSettlerBlockEntity other = casingAt(ahead ? me.worldPosition.relative(facing(), me.along) : me.worldPosition.relative(facing(), -1));
        if (other == null || !other.isStage()) {
            return null;
        }
        MixerSettlerBlockEntity next = other.stage();
        BlockPos expected = ahead ? me.worldPosition.relative(facing(), me.along) : me.worldPosition.relative(facing(), -next.along);
        boolean aligned = next.worldPosition.equals(expected) && next.across == me.across && next.along == me.along && next.tall == me.tall;
        return aligned ? next : null;
    }

    public boolean isHead() { return nextStage(false) == null; }
    public boolean isTail() { return nextStage(true) == null; }
    public Battery battery() {
        if (level == null || level.isClientSide) {
            return Battery.of(this);
        }
        if (battery == null || batteryFormations != formations || battery.stages().stream().anyMatch(BlockEntity::isRemoved)) {
            battery = Battery.of(this);
            batteryFormations = formations;
        }
        return battery;
    }

    public BlockPos mixerPos() {
        return cell(worldPosition, across / 2, 0, tall - 1).above();
    }

    public static final float MAX_MIXER_SPEED = 128;

    public boolean isStirred() {
        return level.getBlockEntity(mixerPos()) instanceof MechanicalMixerBlockEntity mixer && mixer.getSpeed() != 0 && mixer.isSpeedRequirementFulfilled();
    }

    public boolean isOverStirred() {
        return level.getBlockEntity(mixerPos()) instanceof MechanicalMixerBlockEntity mixer && Math.abs(mixer.getSpeed()) > MAX_MIXER_SPEED;
    }

    public boolean hasSignal() {
        for (int a = 0; a < across; a++) {
            for (int l = 0; l < along; l++) {
                for (int u = 0; u < tall; u++) {
                    if (level.hasNeighborSignal(cell(worldPosition, a, l, u))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    public IFluidHandler handler(@Nullable Direction side) {
        if (side == null || !isStage()) {
            return null;
        }
        MixerSettlerBlockEntity stage = stage();
        MixerSettlerBlockEntity beyond = casingAt(worldPosition.relative(side));
        if (beyond != null && beyond.stage() == stage) {
            return null;
        }
        if (side == Direction.UP) {
            return new Port(stage.organic, true);
        }
        boolean head = stage.isHead();
        boolean tail = stage.isTail();
        if (side == Direction.DOWN) {
            return head ? new Port(stage.waste, null) : null;
        }
        if (head && side == facing().getOpposite() || tail && side == facing()) {
            return new Port(stage.aqueous, false);
        }
        if ((head || tail) && side.getAxis().isHorizontal() && side.getAxis() != facing().getAxis()) {
            return new Port(stage.out, null);
        }
        return null;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, MixerSettlerBlockEntity casing) {
        if (!casing.isController() || !casing.isStage()) {
            return;
        }
        if (casing.stirring > 0 && --casing.stirring == 0) {
            casing.dirty = true;
        }
        Battery battery = casing.battery();
        boolean working = casing.isStirred() && battery.isSwitchedOn();
        if (working) {
            casing.driveMixer();
            if (level.getGameTime() % 5 == 0) {
                casing.bubble((ServerLevel) level);
            }
        }
        if (level.getGameTime() % 20 == 0) {
            casing.flowOrganicForward();
            casing.showLinks();
        }
        if (casing.dirty && level.getGameTime() % 10 == 0) {
            casing.dirty = false;
            level.sendBlockUpdated(pos, state, state, 3);
        }
        if (battery.head() == casing) {
            if (casing.stall == null || level.getGameTime() % STALL_INTERVAL == 0) {
                casing.stall = battery.stall();
            }
            Optional<Stall> stall = casing.stall;
            if (stall.isEmpty()) {
                casing.settled++;
            } else if (stall.get().key().equals("lever") || stall.get().key().equals("idle")) {
                casing.settled = 0;
            } else {
                casing.settled = Math.max(0, casing.settled - 2);
            }
            if (level.getGameTime() % 20 == 0) {
                casing.dirty = true;
            }
            if (stall.isEmpty() && casing.settled >= battery.equilibration() && ++casing.cooldown >= PERIOD) {
                casing.cooldown = 0;
                casing.stall = battery.stall();
                if (casing.stall.isEmpty()) {
                    battery.runCut();
                }
            }
        }
    }

    // Create's mixer lowers its head only while it believes it is working: hold it at the bottom of its cycle while the battery runs.
    private void driveMixer() {
        if (!(level.getBlockEntity(mixerPos()) instanceof MechanicalMixerBlockEntity mixer)) {
            return;
        }
        if (!mixer.running) {
            mixer.running = true;
            mixer.runningTicks = 0;
            mixer.sendData();
        } else if (mixer.runningTicks >= 20) {
            mixer.runningTicks = 20;
            mixer.processingTicks = 100;
        }
    }

    private void showLinks() {
        boolean ahead = nextStage(true) != null;
        boolean behind = nextStage(false) != null;
        for (int a = 0; a < across; a++) {
            for (int u = 0; u < tall; u++) {
                link(cell(worldPosition, a, along - 1, u), MixerSettlerBlock.LINK_AHEAD, ahead);
                link(cell(worldPosition, a, 0, u), MixerSettlerBlock.LINK_BEHIND, behind);
            }
        }
    }

    private void link(BlockPos at, BooleanProperty property, boolean value) {
        BlockState state = level.getBlockState(at);
        if (state.is(Separation.mixerSettler()) && state.getValue(property) != value) {
            level.setBlock(at, state.setValue(property, value), Block.UPDATE_CLIENTS);
        }
    }

    private void flowOrganicForward() {
        MixerSettlerBlockEntity next = nextStage(true);
        if (next == null || organic.isEmpty()) {
            return;
        }
        int excess = organic.getFluidAmount() - next.organic.getFluidAmount();
        if (excess <= 0) {
            return;
        }
        FluidStack moved = organic.drain(Math.max(1, Math.min(excess / 4, batch() / 4)), IFluidHandler.FluidAction.SIMULATE);
        int taken = next.organic.fill(moved, IFluidHandler.FluidAction.EXECUTE);
        organic.drain(taken, IFluidHandler.FluidAction.EXECUTE);
    }

    private void bubble(ServerLevel level) {
        BlockPos at = mixerPos().below();
        double x = at.getX() + 0.5, y = worldPosition.getY() + surface(), z = at.getZ() + 0.5;
        level.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 3, 0.2, 0.03, 0.2, 0.0);
        level.sendParticles(ParticleTypes.SPLASH, x, y + 0.05, z, 2, 0.15, 0.0, 0.15, 0.0);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!isStage()) {
            tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.casing")));
            return true;
        }
        Battery battery = battery();
        MixerSettlerBlockEntity head = battery.head();
        tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.stages", battery.size(), head.batch())));
        tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.stage", head.across, head.along, head.tall)));
        Optional<Stall> stall = battery.stall();
        if (stall.isEmpty()) {
            SeparationRecipe cut = SeparationRecipe.forLiquor(level, head.aqueous.getFluid().getFluid()).orElseThrow();
            tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.ready",
                    head.aqueous.getFluid().getHoverName(), cut.lightOf(head.batch()), Battery.name(cut.light()),
                    cut.heavyOf(head.batch()), Battery.name(cut.heavy()))));
            if (head.settled < battery.equilibration()) {
                tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.settling", (battery.equilibration() - head.settled) / 20)));
            }
        } else if (!stall.get().key().equals("full")) {
            tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler." + stall.get().key(), stall.get().args())));
        }
        long charged = battery.stages().stream().filter(s -> s.organic.getFluidAmount() >= s.batch()).count();
        long wet = battery.stages().stream().filter(s -> !s.aqueous.isEmpty()).count();
        tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.progress", charged, battery.size(), wet)));
        tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.ends", held(head.aqueous), held(battery.tail().aqueous))));
        tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.products", held(head.out), held(battery.tail().out))));
        tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.sump", held(head.waste))));
        tooltip.add(indent(Component.translatable("goggles.fundamentals.heat", String.format("%.0f", com.wildspell.fundamental.api.heat.Heat.at(level, worldPosition)))));
        return true;
    }

    private static Component held(Tank tank) {
        FluidStack stack = tank.getFluid();
        if (stack.isEmpty()) {
            return Component.translatable("goggles.fundamentals.mixer_settler.nothing");
        }
        return Component.literal(String.format("%,d mB ", stack.getAmount())).append(stack.getHoverName())
                .withStyle(Style.EMPTY.withColor(Separation.tint(stack.getFluid())));
    }

    private static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null) {
            tag.put("controller", NbtUtils.writeBlockPos(controller));
        }
        tag.putIntArray("size", new int[] {across, along, tall});
        tag.put("organic", organic.getFluid().saveOptional(registries));
        tag.put("aqueous", aqueous.getFluid().saveOptional(registries));
        tag.put("out", out.getFluid().saveOptional(registries));
        tag.put("waste", waste.getFluid().saveOptional(registries));
        tag.putInt("stirring", stirring);
        tag.putInt("settled", settled);
        tag.putInt("crud", crud);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formations++;
        controller = NbtUtils.readBlockPos(tag, "controller").orElse(null);
        int[] size = tag.getIntArray("size");
        if (size.length == 3) {
            across = size[0];
            along = size[1];
            tall = size[2];
        }
        resize();
        organic.setFluid(FluidStack.parseOptional(registries, tag.getCompound("organic")));
        aqueous.setFluid(FluidStack.parseOptional(registries, tag.getCompound("aqueous")));
        out.setFluid(FluidStack.parseOptional(registries, tag.getCompound("out")));
        waste.setFluid(FluidStack.parseOptional(registries, tag.getCompound("waste")));
        stirring = tag.getInt("stirring");
        settled = tag.getInt("settled");
        crud = tag.getInt("crud");
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
