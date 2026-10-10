package ai.gsmc.fundamentals.registry;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
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

public class MortarItem extends HandToolItem {

    public static final int GRIND_TICKS = 40;

    public MortarItem(Properties properties) {
        super(properties);
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static CraftingInput grid(ItemStack mortar, ItemStack material) {
        return CraftingInput.of(2, 1, List.of(material.copyWithCount(1), mortar.copyWithCount(1)));
    }

    private static Optional<RecipeHolder<CraftingRecipe>> recipe(Level level, ItemStack mortar, ItemStack material) {
        return material.isEmpty() ? Optional.empty()
                : level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid(mortar, material), level);
    }

    public static ItemStack grind(Level level, ItemStack mortar, ItemStack material) {
        return recipe(level, mortar, material).map(r -> r.value().assemble(grid(mortar, material), level.registryAccess())).orElse(ItemStack.EMPTY);
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
        return UseAnim.NONE;
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
            ItemStack product = grind(level, stack, material);
            Vec3 look = entity.getLookAngle();
            Vec3 at = entity.getEyePosition().add(look.scale(1.1)).add(0, -0.55, 0);
            for (int i = 0; i < 3 && !product.isEmpty(); i++) {
                level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, product),
                        at.x, at.y, at.z,
                        (level.random.nextDouble() - 0.5) * 0.06, level.random.nextDouble() * 0.08,
                        (level.random.nextDouble() - 0.5) * 0.06);
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
        if (player instanceof ServerPlayer grinder) {
            recipe(level, mortar, material).ifPresent(ground -> grinder.triggerRecipeCrafted(ground, List.of(product)));
        }
        material.consume(1, player);
        if (!player.getInventory().add(product)) {
            player.drop(product, false);
        }
        mortar.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return mortar;
    }
}
