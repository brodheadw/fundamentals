package ai.gsmc.fundamentals.oxidation;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.mixin.CompoundContainerAccessor;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Metal ageing in air, worked out lazily. Nothing scans a chest on a timer. A container keeps a clock (an attachment on its block
 * entity, or on the player for their inventory) of when its contents were last aged; when a player opens or closes it, every stack
 * in it is aged by the time since, at the rate the item's data map entry gives for the air at that spot, and so it is when a hopper or
 * pipe takes from it a minute or more after it was last aged. A stack carries only its
 * stage, so a stack that has aged no longer stacks with fresh metal, as vanilla never merges stacks that differ. Each step is an
 * exponential wait with the entry's mean, so it makes no difference how often a chest is looked into. The player's own inventory is
 * aged every few seconds, a dropped item by its age when picked up, and anything in a charged drum or a sealed canister not at all.
 */
public final class Oxidation {

    public static final DataMapType<Item, Rate> RATES = DataMapType.builder(id("oxidation"), Registries.ITEM, Rate.CODEC)
            .synced(Rate.CODEC, false).build();
    public static final DataComponentType<Integer> STAGE = DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(1, 8)).networkSynchronized(ByteBufCodecs.VAR_INT).build();
    public static final AttachmentType<Long> CLOCK = AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build();
    private static final int PLAYER_INTERVAL = 100;
    private static final long DAY = 24000;
    private static final long UNWATCHED = 1200;

    private static Block drum;
    private static BlockEntityType<InertDrumBlockEntity> drumEntity;
    private static Item drumItem;
    private static Item canister;
    private static Item argonCanister;
    private static Item rustyIronIngot;
    private static Item rustySteelIngot;

    private Oxidation() {}

    public static Block drum() { return drum; }
    public static BlockEntityType<InertDrumBlockEntity> drumEntity() { return drumEntity; }
    public static Item canister() { return canister; }
    public static Item argonCanister() { return argonCanister; }

    public static List<Item> items() {
        return List.of(drumItem, canister, argonCanister, rustyIronIngot, rustySteelIngot);
    }

    public static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(RATES);
    }

    public static void registerComponents(BiConsumer<ResourceLocation, DataComponentType<?>> registry) {
        registry.accept(id("oxidation_stage"), STAGE);
    }

    public static void registerAttachments(BiConsumer<ResourceLocation, AttachmentType<?>> registry) {
        registry.accept(id("oxidation_clock"), CLOCK);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        registry.accept(id("inert_storage_drum"), drum = new InertDrumBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.NETHERITE_BLOCK)));
        Weathering.registerBlocks(registry);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        drumEntity = BlockEntityType.Builder.of(InertDrumBlockEntity::new, drum).build(null);
        registry.accept(id("inert_storage_drum"), drumEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(id("inert_storage_drum"), drumItem = new BlockItem(drum, new Item.Properties()));
        registry.accept(id("canister"), canister = new Item(new Item.Properties().stacksTo(16)));
        registry.accept(id("argon_canister"), argonCanister = new CanisterItem(new Item.Properties().stacksTo(1)));
        registry.accept(id("rusty_iron_ingot"), rustyIronIngot = new Item(new Item.Properties()));
        registry.accept(id("rusty_steel_ingot"), rustySteelIngot = new Item(new Item.Properties()));
        Weathering.registerItems(registry);
    }

    /** {@code stack} after {@code ticks} in {@code air}: the same stack if nothing happened, a later stage, or what it turns into. */
    public static ItemStack age(ItemStack stack, double ticks, Moisture air, RandomSource random) {
        if (stack.isEmpty() || ticks <= 0) {
            return stack;
        }
        Rate rate = stack.getItemHolder().getData(RATES);
        if (rate == null || rate.factor(air) <= 0) {
            return stack;
        }
        double left = ticks * rate.factor(air);
        double mean = rate.days() * DAY;
        int from = stack.getOrDefault(STAGE, 0);
        int stage = from;
        while (stage < rate.steps()) {
            double wait = -mean * Math.log(1 - random.nextDouble());
            if (wait > left) {
                left = 0;
                break;
            }
            left -= wait;
            stage++;
        }
        if (stage == from) {
            return stack;
        }
        if (rate.product().isPresent() && stage == rate.steps()) {
            int count = stack.getCount() / rate.per() + (random.nextInt(rate.per()) < stack.getCount() % rate.per() ? 1 : 0);
            return count == 0 ? ItemStack.EMPTY : age(new ItemStack(rate.product().get(), count), left / rate.factor(air), air, random);
        }
        ItemStack aged = stack.copy();
        aged.set(STAGE, stage);
        return aged;
    }

    /** Ages a container's contents up to now by its clock, in the air at {@code pos} (ordinary air if null), and resets the clock. */
    public static void ageHeld(ServerLevel level, IAttachmentHolder holder, Container container, @Nullable BlockPos pos) {
        long now = level.getGameTime();
        long then = holder.hasData(CLOCK) ? holder.getData(CLOCK) : now;
        holder.setData(CLOCK, now);
        if (then >= now) {
            container.setChanged();
            return;
        }
        Moisture air = pos == null ? Moisture.AIR : Moisture.at(level, pos, false);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            ItemStack aged = age(stack, now - then, air, level.random);
            if (aged != stack) {
                container.setItem(i, aged);
            }
        }
        container.setChanged();
    }

    /** Ages everything a menu shows: the container it opens and the player's own inventory. */
    public static void sweep(Player player, AbstractContainerMenu menu) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Set<Container> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Slot slot : menu.slots) {
            if (slot.container instanceof CompoundContainerAccessor both) {
                age(level, both.fundamentals$first(), seen);
                age(level, both.fundamentals$second(), seen);
            } else {
                age(level, slot.container, seen);
            }
        }
    }

    private static void age(ServerLevel level, Container container, Set<Container> seen) {
        if (!seen.add(container)) {
            return;
        }
        if (container instanceof Inventory inventory) {
            ageHeld(level, inventory.player, inventory, null);
        } else if (container instanceof InertDrumBlockEntity drum) {
            drum.settle();
        } else if (container instanceof BlockEntity entity) {
            ageHeld(level, entity, container, entity.getBlockPos());
        }
    }

    /** A container a hopper or pipe takes from: aged first if it has not been for a minute, so automation does not keep it fresh. */
    public static void taking(Container container) {
        if (container instanceof CompoundContainerAccessor both) {
            taking(both.fundamentals$first());
            taking(both.fundamentals$second());
        } else if (container instanceof BlockEntity entity && !(container instanceof InertDrumBlockEntity) && entity.getLevel() instanceof ServerLevel level) {
            long now = level.getGameTime();
            if (!entity.hasData(CLOCK)) {
                entity.setData(CLOCK, now);
                entity.setChanged();
            } else if (now - entity.getData(CLOCK) > UNWATCHED) {
                ageHeld(level, entity, container, entity.getBlockPos());
            }
        }
    }

    public static void onOpen(PlayerContainerEvent.Open event) {
        sweep(event.getEntity(), event.getContainer());
    }

    public static void onClose(PlayerContainerEvent.Close event) {
        sweep(event.getEntity(), event.getContainer());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % PLAYER_INTERVAL == 0) {
            ageHeld(player.serverLevel(), player, player.getInventory(), null);
        }
    }

    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity entity = event.getItemEntity();
        if (!(entity.level() instanceof ServerLevel level) || entity.getAge() <= 0) {
            return;
        }
        long then = entity.hasData(CLOCK) ? entity.getData(CLOCK) : 0;
        entity.setData(CLOCK, (long) entity.getAge());
        if (entity.getAge() <= then) {
            return;
        }
        Moisture air = entity.isInWater() ? Moisture.WET : Moisture.at(level, entity.blockPosition(), true);
        ItemStack stack = entity.getItem();
        ItemStack aged = age(stack, entity.getAge() - then, air, level.random);
        if (aged == stack) {
            return;
        }
        // playerTouch has already taken hold of the old stack; the aged one is picked up next tick
        if (aged.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(aged);
        }
        event.setCanPickup(TriState.FALSE);
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Integer stage = stack.get(STAGE);
        Rate rate = stack.getItemHolder().getData(RATES);
        if (stage != null && rate != null && !event.getToolTip().isEmpty()) {
            ChatFormatting colour = switch (rate.kind()) {
                case "patina" -> ChatFormatting.DARK_GREEN;
                case "rust" -> ChatFormatting.GOLD;
                case "tarnish" -> ChatFormatting.DARK_GRAY;
                default -> ChatFormatting.GRAY;
            };
            event.getToolTip().add(1, Component.translatable("oxidation.fundamentals." + rate.kind() + "." + stage).withStyle(colour));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
