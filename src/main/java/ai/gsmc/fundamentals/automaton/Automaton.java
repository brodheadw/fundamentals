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
import net.minecraft.world.item.ItemStack;
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
        gather();
        deliver();
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
        if (!player.isSecondaryUseActive() || !player.getItemInHand(hand).isEmpty() || owner != null && !owner.equals(player.getUUID())) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            dropCargo();
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
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Steam", steam());
        if (owner != null) tag.putUUID("Owner", owner);
        tag.put("Cargo", cargo.createTag(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Steam")) setSteam(tag.getInt("Steam"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        cargo.fromTag(tag.getList("Cargo", 10), registryAccess());
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
            if (steam() <= 0 || --scan > 0) return false;
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
            return steam() > 0 && maker() != null && maker().level() == level() && distanceToSqr(maker()) > 36;
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
