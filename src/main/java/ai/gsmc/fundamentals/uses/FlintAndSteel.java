package ai.gsmc.fundamentals.uses;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Flint struck on steel throws sparks; only some of them catch. A strike lights what it is aimed at about one time in three,
 * and the rest only spark, so lighting a fire takes a few tries, as it does.
 */
public final class FlintAndSteel {
    /** The chance one strike catches. */
    public static final float CATCHES = 1.0F / 3.0F;

    private FlintAndSteel() {}

    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(Items.FLINT_AND_STEEL) || event.getEntity().isCreative()) return;
        if (event.getLevel().random.nextFloat() < CATCHES) return;
        if (event.getLevel() instanceof ServerLevel level) {
            Vec3 at = event.getHitVec().getLocation();
            level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.02);
            level.playSound(null, event.getPos(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.8F, 1.2F + level.random.nextFloat() * 0.3F);
        }
        event.getEntity().swing(event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
