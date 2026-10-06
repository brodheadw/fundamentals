package ai.gsmc.fundamentals.registry;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * The mortar and pestle. Besides working in a crafting grid, it grinds in the hands: hold it in
 * one hand and the material in the other, and hold use. Whatever the two would make on a
 * crafting grid is what comes out, so every grinding recipe works both ways.
 */
public class MortarItem extends HandToolItem {

    /** Ticks of grinding for one item. */
    public static final int GRIND_TICKS = 40;

    public MortarItem(Properties properties) {
        super(properties);
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** What grinding {@code material} with this mortar makes, if anything. */
    public static ItemStack grind(Level level, ItemStack mortar, ItemStack material) {
        if (material.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CraftingInput grid = CraftingInput.of(2, 1, List.of(material.copyWithCount(1), mortar.copyWithCount(1)));
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        return recipe.map(r -> r.value().assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack mortar = player.getItemInHand(hand);
        if (grind(level, mortar, player.getItemInHand(other(hand))).isEmpty()) {
            return InteractionResultHolder.pass(mortar);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(mortar);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return GRIND_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;  // the hands are posed by client.GrindingAnimation
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        ItemStack material = entity.getItemInHand(other(entity.getUsedItemHand()));
        if (material.isEmpty()) {
            entity.stopUsingItem();
            return;
        }
        int elapsed = GRIND_TICKS - remaining;
        if (elapsed % 4 != 0) {
            return;
        }
        if (level.isClientSide) {
            // Dust of what is being made puffs up from the bowl: green from malachite, white from wheat.
            ItemStack product = grind(level, stack, material);
            Vec3 look = entity.getLookAngle();
            Vec3 at = entity.getEyePosition().add(look.scale(0.5)).add(0, -0.3, 0);
            for (int i = 0; i < 3 && !product.isEmpty(); i++) {
                level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, product),
                        at.x, at.y, at.z,
                        (level.random.nextDouble() - 0.5) * 0.12, level.random.nextDouble() * 0.12,
                        (level.random.nextDouble() - 0.5) * 0.12);
            }
        } else if (elapsed % 8 == 0) {
            level.playSound(null, entity.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS,
                    0.35F, 1.4F + level.random.nextFloat() * 0.3F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack mortar, Level level, LivingEntity entity) {
        InteractionHand hand = entity.getUsedItemHand();
        ItemStack material = entity.getItemInHand(other(hand));
        ItemStack product = grind(level, mortar, material);
        if (level.isClientSide || product.isEmpty() || !(entity instanceof Player player)) {
            return mortar;
        }
        material.consume(1, player);
        if (!player.getInventory().add(product)) {
            player.drop(product, false);
        }
        mortar.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return mortar;
    }
}
