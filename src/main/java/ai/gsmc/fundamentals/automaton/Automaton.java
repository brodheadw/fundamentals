package ai.gsmc.fundamentals.automaton;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;

/**
 * A clockwork automaton after Dederick's 1868 steam man: a small boiler in its chest drives the works, a cam drum in its head
 * is all the mind it has. It follows its maker, picks up what is dropped and brings it back, climbs ladders and scaffolding,
 * blows steam at what threatens it, and walks back to a lit boiler to refill before its own runs dry.
 */
public class Automaton extends PathfinderMob implements OwnableEntity {
    public static final int MAX_STEAM = 12000, LOW_STEAM = 3000, BLAST_COST = 200, REFILL_PER_TICK = 40, BLAST_COOLDOWN = 30;
    public static final int BOILER_SEARCH = 24, FETCH_RADIUS = 10;
    public static final double BLAST_REACH = 6;
    private static final EntityDataAccessor<Integer> STEAM = SynchedEntityData.defineId(Automaton.class, EntityDataSerializers.INT);

    private final SimpleContainer cargo = new SimpleContainer(9);
    private UUID owner;
    /** The punched card in its head, or empty: with none it follows its maker and fetches. */
    private ItemStack card = ItemStack.EMPTY;
    private BlockPos aim;
    private boolean descending;

    public Automaton(EntityType<? extends Automaton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STEAM, MAX_STEAM);
    }

    public int steam() {
        return entityData.get(STEAM);
    }

    public void setSteam(int steam) {
        entityData.set(STEAM, Math.clamp(steam, 0, MAX_STEAM));
    }

    public SimpleContainer cargo() {
        return cargo;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Override
    public UUID getOwnerUUID() {
        return owner;
    }

    /** Where a goal wants it to go, so it knows which way to climb. */
    public void aim(BlockPos aim) {
        this.aim = aim;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BlastGoal());
        goalSelector.addGoal(2, new RefuelGoal());
        goalSelector.addGoal(3, new CarryGoal());
        goalSelector.addGoal(3, new BuildGoal());
        goalSelector.addGoal(3, new FetchGoal());
        goalSelector.addGoal(4, new FollowGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (steam() > 0 && random.nextInt(steam() < LOW_STEAM ? 12 : 4) == 0) {
                // out of one of the two exhausts on its back
                float yaw = yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
                double side = random.nextBoolean() ? 0.16 : -0.16, back = 0.31;
                double x = getX() + side * net.minecraft.util.Mth.cos(yaw) + back * net.minecraft.util.Mth.sin(yaw);
                double z = getZ() + side * net.minecraft.util.Mth.sin(yaw) - back * net.minecraft.util.Mth.cos(yaw);
                level().addParticle(ParticleTypes.WHITE_SMOKE, x, getY() + 1.22, z, 0, 0.04, 0);
            }
            return;
        }
        Vec3 motion = getDeltaMovement();
        if (motion.horizontalDistanceSqr() > 1.0E-5 || onClimbable() && Math.abs(motion.y) > 0.01) setSteam(steam() - 1);
        climb();
        if (card.isEmpty()) {
            gather();
            deliver();
        }
        if (steam() <= 0) getNavigation().stop();
    }

    /** On a ladder or in scaffolding it climbs toward where it's headed: up by pushing, down by letting itself through. */
    void climb() {
        descending = false;
        if (!onClimbable() || aim == null || steam() <= 0) return;
        if (aim.getY() > getY() + 0.5) {
            getJumpControl().jump();
            // scaffolding has no sides to lean on: hold to the middle of the column while going up it
            Vec3 middle = Vec3.atBottomCenterOf(blockPosition());
            Vec3 motion = getDeltaMovement();
            setDeltaMovement((middle.x - getX()) * 0.3, motion.y, (middle.z - getZ()) * 0.3);
        } else if (aim.getY() < getY() - 0.5) {
            descending = true;
        }
    }

    @Override
    public boolean isDescending() {
        return descending;
    }

    private void gather() {
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(1.0), i -> i.isAlive() && !i.hasPickUpDelay())) {
            ItemStack rest = cargo.addItem(item.getItem().copy());
            int taken = item.getItem().getCount() - rest.getCount();
            if (taken <= 0) continue;
            take(item, taken);
            if (rest.isEmpty()) item.discard();
            else item.setItem(rest);
        }
    }

    private void deliver() {
        if (cargo.isEmpty() || !(getOwner() instanceof Player maker) || distanceToSqr(maker) > 6.25) return;
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack stack = cargo.getItem(i);
            if (!stack.isEmpty()) maker.getInventory().add(stack);
        }
        cargo.setChanged();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (owner != null && !owner.equals(player.getUUID())) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() && (ProgramCard.kind(held) != null || held.isEmpty() && !card.isEmpty())) {
            // a card goes into the head, and whatever card was there comes out into the hand
            if (!level().isClientSide) {
                ItemStack out = card;
                card = held.isEmpty() ? ItemStack.EMPTY : held.split(1);
                if (!out.isEmpty()) {
                    if (held.isEmpty()) player.setItemInHand(hand, out);
                    else if (!player.getInventory().add(out)) spawnAtLocation(out);
                }
                dropCargo();
                getNavigation().stop();
                playSound(SoundEvents.CHISELED_BOOKSHELF_INSERT, 1.0F, 1.4F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!player.isSecondaryUseActive() || !held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            dropCargo();
            if (!card.isEmpty()) spawnAtLocation(card);
            card = ItemStack.EMPTY;
            player.setItemInHand(hand, AutomatonItem.of(this));
            playSound(SoundEvents.COPPER_BREAK, 1.0F, 1.2F);
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void dropCargo() {
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack stack = cargo.removeItemNoUpdate(i);
            if (!stack.isEmpty()) spawnAtLocation(stack);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        dropCargo();
        if (!card.isEmpty()) spawnAtLocation(card);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Steam", steam());
        if (owner != null) tag.putUUID("Owner", owner);
        tag.put("Cargo", cargo.createTag(registryAccess()));
        if (!card.isEmpty()) tag.put("Card", card.save(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Steam")) setSteam(tag.getInt("Steam"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        cargo.fromTag(tag.getList("Cargo", 10), registryAccess());
        card = tag.contains("Card") ? ItemStack.parseOptional(registryAccess(), tag.getCompound("Card")) : ItemStack.EMPTY;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COPPER_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.COPPER_STEP, 0.6F, 0.8F);
    }

    public ItemStack card() {
        return card;
    }

    public void setCard(ItemStack card) {
        this.card = card;
    }

    /** Walks to within reach of a block and reports whether it is there. */
    private boolean reach(BlockPos target, double within) {
        aim(target);
        if (distanceToSqr(Vec3.atCenterOf(target)) <= within * within) {
            getNavigation().stop();
            getLookControl().setLookAt(Vec3.atCenterOf(target));
            return true;
        }
        if (getNavigation().isDone() || tickCount % 20 == 0) getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
        return false;
    }

    private IItemHandler store(BlockPos pos) {
        return pos == null ? null : level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    /** Takes what a store will give into its cargo, as much as fits; filter chooses what. Returns whether anything came. */
    private boolean fill(IItemHandler from, java.util.function.Predicate<ItemStack> filter) {
        boolean took = false;
        for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack offered = from.extractItem(slot, 64, true);
            if (offered.isEmpty() || !filter.test(offered) || !cargo.canAddItem(offered)) continue;
            ItemStack rest = cargo.addItem(offered.copy());
            int moved = offered.getCount() - rest.getCount();
            if (moved > 0) {
                from.extractItem(slot, moved, false);
                took = true;
            }
        }
        return took;
    }

    /** A carry card: fill up at the first store, empty out at the second, and again. */
    final class CarryGoal extends Goal {
        private int idle;

        CarryGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            return steam() > 0 && ProgramCard.kind(card) == ProgramCard.Kind.CARRY
                    && ProgramCard.at(card, "from") != null && ProgramCard.at(card, "to") != null;
        }

        @Override
        public void tick() {
            if (idle > 0) {
                idle--;
                return;
            }
            BlockPos from = ProgramCard.at(card, "from"), to = ProgramCard.at(card, "to");
            if (cargo.isEmpty()) {
                if (!reach(from, 2.5)) return;
                IItemHandler source = store(from);
                if (source == null || !fill(source, stack -> true)) idle = 40;
            } else {
                if (!reach(to, 2.5)) return;
                IItemHandler target = store(to);
                if (target == null) {
                    idle = 40;
                    return;
                }
                boolean gave = false;
                for (int i = 0; i < cargo.getContainerSize(); i++) {
                    ItemStack stack = cargo.getItem(i);
                    if (stack.isEmpty()) continue;
                    ItemStack rest = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
                    gave |= rest.getCount() < stack.getCount();
                    cargo.setItem(i, rest);
                }
                // the store is full: wait rather than walk back and forth
                if (!gave) idle = 40;
            }
        }
    }

    /** A build card: fill the marked space from the bottom up with blocks fetched from the store. */
    final class BuildGoal extends Goal {
        private BlockPos target;
        private int scan, idle;

        BuildGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            return steam() > 0 && ProgramCard.kind(card) == ProgramCard.Kind.BUILD
                    && ProgramCard.at(card, "a") != null && ProgramCard.at(card, "b") != null;
        }

        private boolean hasBlocks() {
            for (int i = 0; i < cargo.getContainerSize(); i++) if (cargo.getItem(i).getItem() instanceof BlockItem) return true;
            return false;
        }

        /** The next empty space to fill: lowest first, then nearest, and only where something already holds it up. */
        private BlockPos next() {
            BlockPos a = ProgramCard.at(card, "a"), b = ProgramCard.at(card, "b");
            if (ProgramCard.volume(a, b) > ProgramCard.MAX_VOLUME) return null;
            BlockPos best = null;
            double bestScore = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(a, b)) {
                if (!level().getBlockState(pos).canBeReplaced() || getBoundingBox().intersects(new net.minecraft.world.phys.AABB(pos))) continue;
                boolean held = false;
                for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
                    if (side != net.minecraft.core.Direction.UP && !level().getBlockState(pos.relative(side)).canBeReplaced()) {
                        held = true;
                        break;
                    }
                }
                if (!held) continue;
                double score = pos.getY() * 10000.0 + pos.distSqr(blockPosition());
                if (score < bestScore) {
                    bestScore = score;
                    best = pos.immutable();
                }
            }
            return best;
        }

        @Override
        public void tick() {
            if (idle > 0) {
                idle--;
                return;
            }
            if (!hasBlocks()) {
                BlockPos supply = ProgramCard.at(card, "supply");
                IItemHandler store = store(supply);
                if (store == null) {
                    idle = 40;
                    return;
                }
                if (reach(supply, 2.5) && !fill(store, stack -> stack.getItem() instanceof BlockItem)) idle = 60;
                return;
            }
            if (target == null || !level().getBlockState(target).canBeReplaced() || --scan <= 0) {
                target = next();
                scan = 20;
            }
            if (target == null) {
                idle = 40;
                return;
            }
            if (!reach(target, 3.5)) return;
            for (int i = 0; i < cargo.getContainerSize(); i++) {
                ItemStack stack = cargo.getItem(i);
                if (!(stack.getItem() instanceof BlockItem block)) continue;
                var state = block.getBlock().defaultBlockState();
                if (!state.canSurvive(level(), target)) continue;
                level().setBlock(target, state, 3);
                level().playSound(null, target, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                swing(InteractionHand.MAIN_HAND);
                stack.shrink(1);
                cargo.setChanged();
                target = null;
                return;
            }
            idle = 40;
        }
    }

    /** It answers threats with the only thing it has: a jet of steam from the chimney, scalding and shoving. */
    final class BlastGoal extends Goal {
        private LivingEntity target;
        private int cooldown;

        BlastGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            if (steam() < BLAST_COST) return false;
            target = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BLAST_REACH),
                            e -> e instanceof Enemy && e.isAlive() && hasLineOfSight(e))
                    .stream().min(Comparator.comparingDouble(Automaton.this::distanceToSqr)).orElse(null);
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target.isAlive() && steam() >= BLAST_COST && distanceToSqr(target) < (BLAST_REACH + 2) * (BLAST_REACH + 2);
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--cooldown > 0 || distanceToSqr(target) > BLAST_REACH * BLAST_REACH) return;
            cooldown = BLAST_COOLDOWN;
            blast(target);
        }
    }

    void blast(LivingEntity target) {
        setSteam(steam() - BLAST_COST);
        target.hurt(damageSources().mobAttack(this), 4.0F);
        target.knockback(0.8, getX() - target.getX(), getZ() - target.getZ());
        if (level() instanceof ServerLevel server) {
            Vec3 from = getEyePosition(), to = target.getEyePosition();
            for (int i = 1; i <= 8; i++) {
                Vec3 p = from.lerp(to, i / 8.0);
                server.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.02);
            }
        }
        level().playSound(null, blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 0.8F, 1.4F);
    }

    /** Running low, it goes back to a lit boiler and stands beside it until its own is full again. */
    final class RefuelGoal extends Goal {
        private BlockPos boiler;
        private int ticks, scan;

        RefuelGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            if (steam() <= 0 || steam() >= LOW_STEAM || --scan > 0) return false;
            scan = 20;
            boiler = Boilers.nearest(level(), blockPosition(), BOILER_SEARCH);
            return boiler != null;
        }

        @Override
        public boolean canContinueToUse() {
            return steam() > 0 && steam() < MAX_STEAM && ticks < 2400 && Boilers.steaming(level(), boiler);
        }

        @Override
        public void start() {
            ticks = 0;
            aim(boiler);
            getNavigation().moveTo(boiler.getX() + 0.5, boiler.getY(), boiler.getZ() + 0.5, 1.0);
        }

        @Override
        public void tick() {
            ticks++;
            if (distanceToSqr(Vec3.atCenterOf(boiler)) <= 3.5 * 3.5) {
                getNavigation().stop();
                getLookControl().setLookAt(Vec3.atCenterOf(boiler));
                refill();
            } else if (getNavigation().isDone()) {
                getNavigation().moveTo(boiler.getX() + 0.5, boiler.getY(), boiler.getZ() + 0.5, 1.0);
            }
        }
    }

    public void refill() {
        setSteam(steam() + REFILL_PER_TICK);
        if (level() instanceof ServerLevel server && tickCount % 5 == 0) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + getBbHeight(), getZ(), 3, 0.2, 0.1, 0.2, 0.01);
        }
        if (tickCount % 20 == 0) level().playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.4F, 0.6F);
    }

    /** Anything dropped nearby, it goes and picks up. */
    final class FetchGoal extends Goal {
        private ItemEntity item;
        private int scan;

        FetchGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            if (steam() <= 0 || !card.isEmpty() || --scan > 0) return false;
            scan = 5;
            item = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(FETCH_RADIUS), i -> i.isAlive() && cargo.canAddItem(i.getItem()))
                    .stream().min(Comparator.comparingDouble(Automaton.this::distanceToSqr)).orElse(null);
            return item != null;
        }

        @Override
        public boolean canContinueToUse() {
            return item.isAlive() && steam() > 0 && distanceToSqr(item) < (FETCH_RADIUS + 4) * (FETCH_RADIUS + 4);
        }

        @Override
        public void tick() {
            aim(item.blockPosition());
            if (getNavigation().isDone() || tickCount % 20 == 0) getNavigation().moveTo(item, 1.1);
        }
    }

    /** Otherwise it keeps near its maker. */
    final class FollowGoal extends Goal {
        FollowGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private LivingEntity maker() {
            return getOwner();
        }

        @Override
        public boolean canUse() {
            return steam() > 0 && card.isEmpty() && maker() != null && maker().level() == level() && distanceToSqr(maker()) > 36;
        }

        @Override
        public boolean canContinueToUse() {
            return steam() > 0 && maker() != null && maker().level() == level() && distanceToSqr(maker()) > 9;
        }

        @Override
        public void tick() {
            aim(maker().blockPosition());
            getLookControl().setLookAt(maker(), 10.0F, getMaxHeadXRot());
            if (getNavigation().isDone() || tickCount % 10 == 0) getNavigation().moveTo(maker(), 1.0);
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
