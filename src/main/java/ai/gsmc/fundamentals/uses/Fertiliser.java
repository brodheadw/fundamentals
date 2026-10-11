package ai.gsmc.fundamentals.uses;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.Nullable;

/** Bone meal's growth, given as many times over as the fertiliser is stronger, on one crop or on a patch. */
public class Fertiliser extends Item {

    // A straight fertiliser carries one nutrient and feeds the crop it is put on; a compound carries all three and is broadcast over a patch.
    public static final List<Fertiliser> ALL = List.of(
            new Fertiliser("ammonium_nitrate", 2, 0, true), new Fertiliser("urea", 2, 0, false),
            new Fertiliser("superphosphate", 2, 0, false), new Fertiliser("ammonium_phosphate", 2, 0, false),
            new Fertiliser("npk_fertiliser", 2, 1, false));
    // A stack this big, burning or caught in a blast, detonates; less only decomposes.
    public static final int DETONATES = 16;
    private static final Queue<Runnable> BLASTS = new ConcurrentLinkedQueue<>();

    private final String id;
    private final int doses;
    private final int reach;
    private final boolean explosive;

    private Fertiliser(String id, int doses, int reach, boolean explosive) {
        super(new Item.Properties());
        this.id = id;
        this.doses = doses;
        this.reach = reach;
        this.explosive = explosive;
    }

    public String id() {
        return id;
    }

    public boolean feed(Level level, BlockPos centre, @Nullable Player player) {
        boolean fed = false;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reach, 0, -reach), centre.offset(reach, 0, reach))) {
            boolean grew = false;
            for (int i = 0; i < doses && BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, pos, player); i++) {
                grew = true;
            }
            if (grew && !level.isClientSide) {
                level.levelEvent(1505, pos, 15);
            }
            fed |= grew;
        }
        return fed;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!feed(level, context.getClickedPos(), context.getPlayer())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (explosive && !entity.level().isClientSide && (entity.isOnFire() || entity.isInLava())) {
            detonate(entity);
            return true;
        }
        return false;
    }

    private static void detonate(ItemEntity entity) {
        int count = entity.getItem().getCount();
        entity.discard();
        if (count >= DETONATES) {
            Level level = entity.level();
            double x = entity.getX(), y = entity.getY(), z = entity.getZ();
            BLASTS.add(() -> level.explode(null, x, y, z, power(count), Level.ExplosionInteraction.TNT));
        }
    }

    public static float power(int count) {
        return Math.min(6.0F, 1.0F + count / 16.0F);
    }

    public static void registerDispenserBehaviour() {
        for (Fertiliser fertiliser : ALL) {
            DispenserBlock.registerBehavior(fertiliser, new OptionalDispenseItemBehavior() {
                @Override
                protected ItemStack execute(BlockSource source, ItemStack stack) {
                    ServerLevel level = source.level();
                    BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                    setSuccess(fertiliser.feed(level, target, null));
                    if (isSuccess()) {
                        stack.shrink(1);
                    }
                    return stack;
                }
            });
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        for (Entity entity : event.getAffectedEntities()) {
            if (entity instanceof ItemEntity item && item.isAlive() && item.getItem().getItem() instanceof Fertiliser fertiliser && fertiliser.explosive) {
                detonate(item);
            }
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        for (Runnable blast; (blast = BLASTS.poll()) != null; ) {
            blast.run();
        }
    }
}
