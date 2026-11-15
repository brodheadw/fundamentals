package ai.gsmc.fundamentals.bombs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A lit charge: vanilla's primed TNT, wearing the charge's block and going off as that charge does. */
public class Charge extends PrimedTnt {

    public Charge(EntityType<? extends Charge> type, Level level) {
        super(type, level);
    }

    void arm(BlockPos pos, BlockState state, int fuse, LivingEntity igniter) {
        setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        double angle = level().random.nextDouble() * Math.PI * 2;
        setDeltaMovement(-Math.sin(angle) * 0.02, 0.2, -Math.cos(angle) * 0.02);
        setFuse(fuse);
        setBlockState(state);
        xo = getX();
        yo = getY();
        zo = getZ();
    }

    @Override
    public void tick() {
        super.tick();
        // an armed bomb counts down aloud, a second a stroke
        if (!level().isClientSide && getBlockState().getBlock() instanceof ExplosiveBlock bomb && bomb.kind().nuclear() && getFuse() % 20 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 2F, 0.5F);
        }
    }

    @Override
    protected void explode() {
        if (level() instanceof ServerLevel level && getBlockState().getBlock() instanceof ExplosiveBlock bomb) {
            bomb.detonate(level, position().add(0, getBbHeight() / 16.0, 0), this);
        } else {
            super.explode();
        }
    }
}
