package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.separation.MixerSettlerBlock.Rows;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One casing of a stage. A casing alone is a stage of one; casings merge as they are placed into the largest
 * box they complete, up to three across, three along and two tall. A stage is stirred by Create's Mechanical
 * Mixer standing over the middle of its trough, and runs only while that turns. The casing at a stage's back-left-bottom
 * corner is its controller: it holds the tanks, which grow with the stage, runs the cut and draws the fluids;
 * the others point at it. The controller of the first stage in a battery (the one with no stage behind it)
 * runs the cut for the whole battery: it takes a batch from its own aqueous tank, the feed, and from the
 * last stage's, the strip acid, and puts the raffinate in its own out-tank and the loaded strip in the last
 * stage's. The stages between only carry the organic and show what is passing through them.
 */
public class MixerSettlerBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY_PER_CASING = 250;
    public static final int BATCH_PER_CASING = 10;
    public static final int PERIOD = 100;
    /** Ticks of running before a battery of one stage first delivers; each further stage adds as much. */
    public static final int EQUILIBRATION_PER_STAGE = 40;
    private static final int STIR_TICKS = 30;
    private static final int POUR_TICKS = 12;

    final Tank organic = new Tank();
    final Tank aqueous = new Tank();
    final Tank out = new Tank();
    @Nullable
    private BlockPos controller;
    private int across = 1;
    private int along = 1;
    private int tall = 1;
    private int stirring;
    private int cooldown;
    private int settled;
    private int pouring;
    private boolean dirty;

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
    public boolean pouring() { return pouring > 0; }
    public int across() { return across; }
    public int along() { return along; }
    public int tall() { return tall; }
    public int volume() { return across * along * tall; }
    public int capacity() { return CAPACITY_PER_CASING * volume(); }
    public int batch() { return BATCH_PER_CASING * volume(); }
    public boolean isController() { return controller == null || worldPosition.equals(controller); }
    public BlockPos controllerPos() { return controller == null ? worldPosition : controller; }

    public Direction facing() {
        return getBlockState().getValue(MixerSettlerBlock.FACING);
    }

    private Direction right() {
        return facing().getClockWise();
    }

    @Nullable
    private MixerSettlerBlockEntity casingAt(BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MixerSettlerBlockEntity casing && casing.facing() == facing() ? casing : null;
    }

    /** The controller of this casing's stage; a casing whose controller no longer claims it is a stage of one again. */
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

    private BlockPos cell(BlockPos origin, int a, int l, int u) {
        return origin.relative(right(), a).relative(facing(), l).above(u);
    }

    private void becomeSingle() {
        controller = null;
        across = along = tall = 1;
        resize();
        setChanged();
    }

    private void resize() {
        int capacity = capacity();
        organic.setCapacity(capacity);
        aqueous.setCapacity(capacity);
        out.setCapacity(capacity);
    }

    /** A tick after placement: join the largest box of casings this one completes. */
    void merge() {
        int best = volume();
        BlockPos bestOrigin = null;
        int[] bestDims = null;
        for (int w = 1; w <= MixerSettlerBlock.MAX_ACROSS; w++) {
            for (int l = 1; l <= MixerSettlerBlock.MAX_ALONG; l++) {
                for (int h = 1; h <= MixerSettlerBlock.MAX_TALL; h++) {
                    if (w * l * h <= best) {
                        continue;
                    }
                    for (int a = 0; a < w; a++) {
                        for (int l0 = 0; l0 < l; l0++) {
                            for (int u = 0; u < h; u++) {
                                BlockPos origin = worldPosition.relative(right(), -a).relative(facing(), -l0).below(u);
                                if (fits(origin, w, l, h)) {
                                    best = w * l * h;
                                    bestOrigin = origin;
                                    bestDims = new int[] {w, l, h};
                                }
                            }
                        }
                    }
                }
            }
        }
        if (bestOrigin != null) {
            form(bestOrigin, bestDims[0], bestDims[1], bestDims[2]);
        }
    }

    /** Every cell is a casing facing our way, and every stage any of them belongs to lies wholly inside. */
    private boolean fits(BlockPos origin, int w, int l, int h) {
        for (int a = 0; a < w; a++) {
            for (int l0 = 0; l0 < l; l0++) {
                for (int u = 0; u < h; u++) {
                    MixerSettlerBlockEntity casing = casingAt(cell(origin, a, l0, u));
                    if (casing == null) {
                        return false;
                    }
                    MixerSettlerBlockEntity stage = casing.stage();
                    BlockPos far = stage.cell(stage.worldPosition, stage.across - 1, stage.along - 1, stage.tall - 1);
                    if (!inside(origin, w, l, h, stage.worldPosition) || !inside(origin, w, l, h, far)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean inside(BlockPos origin, int w, int l, int h, BlockPos pos) {
        BlockPos rel = pos.subtract(origin);
        Direction right = right();
        int a = rel.getX() * right.getStepX() + rel.getZ() * right.getStepZ();
        int l0 = rel.getX() * facing().getStepX() + rel.getZ() * facing().getStepZ();
        return a >= 0 && a < w && l0 >= 0 && l0 < l && rel.getY() >= 0 && rel.getY() < h;
    }

    private void form(BlockPos origin, int w, int l, int h) {
        // what the stages being absorbed held goes into the new controller, as far as it fits
        Set<MixerSettlerBlockEntity> absorbed = new LinkedHashSet<>();
        List<FluidStack> organics = new ArrayList<>();
        List<FluidStack> aqueouses = new ArrayList<>();
        List<FluidStack> outs = new ArrayList<>();
        for (int a = 0; a < w; a++) {
            for (int l0 = 0; l0 < l; l0++) {
                for (int u = 0; u < h; u++) {
                    MixerSettlerBlockEntity stage = casingAt(cell(origin, a, l0, u)).stage();
                    if (absorbed.add(stage)) {
                        organics.add(stage.organic.getFluid().copy());
                        aqueouses.add(stage.aqueous.getFluid().copy());
                        outs.add(stage.out.getFluid().copy());
                    }
                }
            }
        }
        MixerSettlerBlockEntity head = casingAt(origin);
        for (int a = 0; a < w; a++) {
            for (int l0 = 0; l0 < l; l0++) {
                for (int u = 0; u < h; u++) {
                    MixerSettlerBlockEntity casing = casingAt(cell(origin, a, l0, u));
                    casing.controller = origin;
                    casing.across = w;
                    casing.along = l;
                    casing.tall = h;
                    casing.organic.setFluid(FluidStack.EMPTY);
                    casing.aqueous.setFluid(FluidStack.EMPTY);
                    casing.out.setFluid(FluidStack.EMPTY);
                    casing.stirring = 0;
                    casing.resize();
                    casing.setChanged();
                    BlockState state = casing.getBlockState()
                            .setValue(MixerSettlerBlock.LEFT, a > 0).setValue(MixerSettlerBlock.RIGHT, a < w - 1)
                            .setValue(MixerSettlerBlock.BACK, l0 > 0).setValue(MixerSettlerBlock.FRONT, l0 < l - 1)
                            .setValue(MixerSettlerBlock.BELOW, u > 0).setValue(MixerSettlerBlock.ABOVE, u < h - 1)
                            .setValue(MixerSettlerBlock.ROWS, l == 1 ? Rows.SINGLE : l0 == 0 ? Rows.WELL : Rows.BAY)
                            .setValue(MixerSettlerBlock.OPEN, l0 == 0 && u == h - 1 && a == w / 2);
                    level.setBlock(casing.worldPosition, state, Block.UPDATE_ALL);
                }
            }
        }
        for (FluidStack stack : organics) head.organic.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        for (FluidStack stack : aqueouses) head.aqueous.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        for (FluidStack stack : outs) head.out.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        head.dirty = true;
    }

    /** Losing a casing breaks its stage into single casings, which merge again on their own; the tanks are lost. */
    void dissolve() {
        MixerSettlerBlockEntity stage = stage();
        BlockPos origin = stage.worldPosition;
        int w = stage.across, l = stage.along, h = stage.tall;
        for (int a = 0; a < w; a++) {
            for (int l0 = 0; l0 < l; l0++) {
                for (int u = 0; u < h; u++) {
                    BlockPos at = cell(origin, a, l0, u);
                    MixerSettlerBlockEntity casing = casingAt(at);
                    if (casing == null || casing == this) {
                        continue;
                    }
                    casing.becomeSingle();
                    casing.organic.setFluid(FluidStack.EMPTY);
                    casing.aqueous.setFluid(FluidStack.EMPTY);
                    casing.out.setFluid(FluidStack.EMPTY);
                    casing.stirring = 0;
                    level.setBlock(at, Separation.mixerSettler().defaultBlockState().setValue(MixerSettlerBlock.FACING, facing()), Block.UPDATE_ALL);
                    level.scheduleTick(at, Separation.mixerSettler(), 1);
                }
            }
        }
    }

    /** The controller of the stage of the same size standing end to end with this one, ahead or behind. */
    @Nullable
    private MixerSettlerBlockEntity nextStage(boolean ahead) {
        MixerSettlerBlockEntity me = stage();
        MixerSettlerBlockEntity other = casingAt(ahead ? me.worldPosition.relative(facing(), me.along) : me.worldPosition.relative(facing(), -1));
        if (other == null) {
            return null;
        }
        MixerSettlerBlockEntity next = other.stage();
        BlockPos expected = ahead ? me.worldPosition.relative(facing(), me.along) : me.worldPosition.relative(facing(), -next.along);
        boolean aligned = next.worldPosition.equals(expected) && next.across == me.across && next.along == me.along && next.tall == me.tall;
        return aligned ? next : null;
    }

    /** Where the stage's Mechanical Mixer stands: over the middle casing of the trough's top layer. */
    public BlockPos mixerPos() {
        return cell(worldPosition, across / 2, 0, tall - 1).above();
    }

    public boolean isStirred() {
        return level.getBlockEntity(mixerPos()) instanceof MechanicalMixerBlockEntity mixer && mixer.getSpeed() != 0 && mixer.isSpeedRequirementFulfilled();
    }

    /** The battery runs while any casing of its head stage has a redstone signal: the lever on the wall. */
    public boolean isSwitchedOn() {
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

    public boolean isHead() {
        return nextStage(false) == null;
    }

    public boolean isTail() {
        return nextStage(true) == null;
    }

    /** The stage controllers from the head forward. */
    public List<MixerSettlerBlockEntity> battery() {
        MixerSettlerBlockEntity head = stage();
        for (MixerSettlerBlockEntity back; (back = head.nextStage(false)) != null; ) {
            head = back;
        }
        List<MixerSettlerBlockEntity> stages = new ArrayList<>();
        for (MixerSettlerBlockEntity stage = head; stage != null; stage = stage.nextStage(true)) {
            stages.add(stage);
        }
        return stages;
    }

    /**
     * Which tank a pipe on {@code side} of this casing reaches, if any: the organic from above anywhere; the
     * feed and the raffinate at the back and sides of the first stage; the acid and the loaded strip at the
     * front and sides of the last.
     */
    @Nullable
    public IFluidHandler handler(@Nullable Direction side) {
        if (side == null) {
            return null;
        }
        MixerSettlerBlockEntity stage = stage();
        MixerSettlerBlockEntity beyond = casingAt(worldPosition.relative(side));
        if (beyond != null && beyond.stage() == stage) {
            return null;
        }
        if (side == Direction.UP) {
            return new Port(stage.organic, true, this);
        }
        boolean head = stage.isHead();
        boolean tail = stage.isTail();
        if (head && side == facing().getOpposite() || tail && side == facing()) {
            return new Port(stage.aqueous, false, this);
        }
        if ((head || tail) && side.getAxis().isHorizontal() && side.getAxis() != facing().getAxis()) {
            return stage.out;
        }
        return null;
    }

    /** A tank seen through a pipe: the top takes only organics, the ends only liquors and acids. */
    private record Port(Tank tank, boolean organic, MixerSettlerBlockEntity pourer) implements IFluidHandler {
        private boolean accepts(FluidStack stack) {
            Reagents.Kind kind = Separation.kind(stack.getFluid());
            return organic ? kind == Reagents.Kind.ORGANIC : kind == Reagents.Kind.LIQUOR || kind == Reagents.Kind.ACID;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return accepts(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!accepts(resource)) {
                return 0;
            }
            int filled = tank.fill(resource, action);
            if (filled > 0 && action.execute() && organic) {
                pourer.pouring = POUR_TICKS;
                pourer.dirty = true;
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, MixerSettlerBlockEntity casing) {
        if (casing.pouring > 0 && --casing.pouring == 0) {
            casing.dirty = true;
        }
        if (!casing.isController()) {
            if (casing.dirty && level.getGameTime() % 5 == 0) {
                casing.dirty = false;
                level.sendBlockUpdated(pos, state, state, 3);
            }
            return;
        }
        if (casing.stirring > 0) {
            casing.stirring--;
            if (casing.stirring % 5 == 0) {
                casing.bubble((ServerLevel) level);
            }
            if (casing.stirring == 0) {
                casing.dirty = true;
            }
        }
        if (level.getGameTime() % 5 == 0) {
            casing.flowOrganicForward();
        }
        if (level.getGameTime() % 20 == 0) {
            casing.showLinks();
        }
        if (casing.dirty && level.getGameTime() % 10 == 0) {
            casing.dirty = false;
            level.sendBlockUpdated(pos, state, state, 3);
        }
        if (casing.isHead()) {
            // the battery comes to equilibrium before its first batch and keeps time after
            boolean ready = casing.stall().isEmpty();
            casing.settled = ready ? casing.settled + 1 : 0;
            if (ready && casing.settled >= casing.equilibration() && ++casing.cooldown >= PERIOD) {
                casing.cooldown = 0;
                casing.runCut();
            }
        }
    }

    public int equilibration() {
        return EQUILIBRATION_PER_STAGE * battery().size();
    }

    /** Seconds until the first batch, while the battery is coming to equilibrium. */
    public int secondsToEquilibrium() {
        return Math.max(0, equilibration() - settled) / 20;
    }

    /** The ports in the end walls appear when a stage stands end to end with another. */
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

    private void link(BlockPos at, net.minecraft.world.level.block.state.properties.BooleanProperty property, boolean value) {
        BlockState state = level.getBlockState(at);
        if (state.is(Separation.mixerSettler()) && state.getValue(property) != value) {
            level.setBlock(at, state.setValue(property, value), Block.UPDATE_CLIENTS);
        }
    }

    /** The organic runs forward along the battery: a stage with more than the one ahead sends some on. */
    private void flowOrganicForward() {
        MixerSettlerBlockEntity next = nextStage(true);
        if (next == null || organic.isEmpty()) {
            return;
        }
        int excess = organic.getFluidAmount() - next.organic.getFluidAmount();
        if (excess <= 0) {
            return;
        }
        FluidStack moved = organic.drain(Math.min(excess / 2, batch()), IFluidHandler.FluidAction.SIMULATE);
        int taken = next.organic.fill(moved, IFluidHandler.FluidAction.EXECUTE);
        organic.drain(taken, IFluidHandler.FluidAction.EXECUTE);
    }

    /** Why the head is not cutting, as a lang key and its arguments; empty when it is. */
    public Optional<Object[]> stall() {
        List<MixerSettlerBlockEntity> stages = battery();
        MixerSettlerBlockEntity tail = stages.getLast();
        FluidStack feed = aqueous.getFluid();
        int batch = batch();
        if (feed.getAmount() < batch) {
            return Optional.of(new Object[] {"idle"});
        }
        Optional<SeparationRecipe> found = SeparationRecipe.forLiquor(level, feed.getFluid());
        if (found.isEmpty()) {
            return Optional.of(new Object[] {"no_cut", feed.getHoverName()});
        }
        SeparationRecipe cut = found.get();
        if (stages.size() < cut.stages()) {
            return Optional.of(new Object[] {"short", feed.getHoverName(), cut.stages(), stages.size()});
        }
        if (!stages.stream().allMatch(MixerSettlerBlockEntity::isStirred)) {
            return Optional.of(new Object[] {"mixer"});
        }
        if (!isSwitchedOn()) {
            return Optional.of(new Object[] {"lever"});
        }
        if (!stages.stream().allMatch(s -> s.organic.getFluid().is(cut.organic()) && s.organic.getFluidAmount() >= batch)) {
            return Optional.of(new Object[] {"organic", name(cut.organic())});
        }
        if (!tail.aqueous.getFluid().is(cut.strip()) || tail.aqueous.getFluidAmount() < batch) {
            return Optional.of(new Object[] {"strip", name(cut.strip())});
        }
        if (out.fill(new FluidStack(cut.light(), batch), IFluidHandler.FluidAction.SIMULATE) < batch
                || tail.out.fill(new FluidStack(cut.heavy(), batch), IFluidHandler.FluidAction.SIMULATE) < batch) {
            return Optional.of(new Object[] {"full"});
        }
        return Optional.empty();
    }

    private void runCut() {
        List<MixerSettlerBlockEntity> stages = battery();
        MixerSettlerBlockEntity tail = stages.getLast();
        SeparationRecipe cut = SeparationRecipe.forLiquor(level, aqueous.getFluid().getFluid()).orElseThrow();
        int batch = batch();
        aqueous.drain(batch, IFluidHandler.FluidAction.EXECUTE);
        tail.aqueous.drain(batch, IFluidHandler.FluidAction.EXECUTE);
        out.fill(new FluidStack(cut.light(), batch), IFluidHandler.FluidAction.EXECUTE);
        tail.out.fill(new FluidStack(cut.heavy(), batch), IFluidHandler.FluidAction.EXECUTE);
        // The aqueous phase is the depleting feed through the extraction stages and the acid loading up
        // through the strip stages; the stages between the ends show one or the other going past.
        int strip = stages.size() * 2 / 5;
        for (int i = 0; i < stages.size(); i++) {
            MixerSettlerBlockEntity stage = stages.get(i);
            if (i > 0 && i < stages.size() - 1) {
                stage.aqueous.setFluid(new FluidStack(i < stages.size() - strip ? cut.light() : cut.heavy(), stage.capacity() / 2));
            }
            stage.stirring = STIR_TICKS;
            stage.dirty = true;
        }
    }

    private void bubble(ServerLevel level) {
        // Along the mixing trough at the surface: the back row, or the back of the only row.
        Direction right = right();
        double back = along == 1 ? 0.3 : 0.0;
        for (int a = 0; a < across; a++) {
            BlockPos at = worldPosition.relative(right, a).above(tall - 1);
            double x = at.getX() + 0.5 - facing().getStepX() * back;
            double z = at.getZ() + 0.5 - facing().getStepZ() * back;
            level.sendParticles(ParticleTypes.BUBBLE_POP, x, at.getY() + (along == 1 ? 0.6 : 0.25), z, 2, 0.3, 0.05, along == 1 ? 0.1 : 0.3, 0.0);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        List<MixerSettlerBlockEntity> stages = battery();
        MixerSettlerBlockEntity head = stages.getFirst();
        tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.stages", stages.size(), head.batch())));
        tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.stage", head.across, head.along, head.tall)));
        Object[] stall = head.stall().orElse(null);
        if (stall == null) {
            SeparationRecipe cut = SeparationRecipe.forLiquor(level, head.aqueous.getFluid().getFluid()).orElseThrow();
            tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.ready",
                    head.aqueous.getFluid().getHoverName(), name(cut.light()), name(cut.heavy()))));
            if (head.settled < head.equilibration()) {
                tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler.settling", head.secondsToEquilibrium())));
            }
        } else if (!stall[0].equals("full")) {
            Object[] args = List.of(stall).subList(1, stall.length).toArray();
            tooltip.add(indent(Component.translatable("goggles.fundamentals.mixer_settler." + stall[0], args)));
        }
        return true;
    }

    private static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }

    private static Component name(Fluid fluid) {
        return new FluidStack(fluid, 1).getHoverName();
    }

    /** The stage's whole box, for the renderer, which draws it all from the controller. */
    public AABB stageBox() {
        return AABB.encapsulatingFullBlocks(worldPosition, cell(worldPosition, across - 1, along - 1, tall - 1));
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
        tag.putInt("stirring", stirring);
        tag.putInt("settled", settled);
        tag.putInt("pouring", pouring);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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
        stirring = tag.getInt("stirring");
        settled = tag.getInt("settled");
        pouring = tag.getInt("pouring");
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
