package ai.gsmc.fundamentals.glass;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Safety and armour glass, polycarbonate, and the blast door they end in. */
public final class Glass {

    public static final List<String> ITEMS = List.of("hot_glass", "hot_glass_pane", "bisphenol_a", "polycarbonate_resin", "polycarbonate_sheet");
    /** Riot shield uses: three times an oak shield's 336. */
    public static final int RIOT_SHIELD_USES = 1008;

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final List<Item> ALL = new ArrayList<>();
    private static BlastDoorBlock blastDoor;
    private static BlockEntityType<BlastDoorBlockEntity> blastDoorEntity;
    private static Item riotShield;

    private Glass() {}

    public static Block block(String id) { return BLOCKS.get(id); }
    public static BlastDoorBlock blastDoor() { return blastDoor; }
    public static BlockEntityType<BlastDoorBlockEntity> blastDoorEntity() { return blastDoorEntity; }
    public static Item riotShield() { return riotShield; }
    public static List<Item> items() { return ALL; }

    private static BlockBehaviour.Properties glass(float hardness, float blast) {
        return BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(hardness, blast).sound(SoundType.GLASS).noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false);
    }

    private static void block(BiConsumer<ResourceLocation, Block> registry, String id, Block block) {
        BLOCKS.put(id, block);
        registry.accept(Fundamentals.id(id), block);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        // Hardness and blast resistance follow strength: glass is 0.3; borosilicate is no stronger, only shock-proof;
        // tempering puts the faces in compression and makes glass four or five times as strong in bending;
        // a laminate of tempered glass and polycarbonate stops rifle fire; sapphire is Mohs 9, scratched by diamond alone.
        block(registry, "borosilicate_glass", new TransparentBlock(glass(0.4F, 0.6F)));
        block(registry, "borosilicate_glass_pane", new IronBarsBlock(glass(0.4F, 0.6F)));
        block(registry, "tempered_glass", new TransparentBlock(glass(1.2F, 9.0F)));
        block(registry, "tempered_glass_pane", new IronBarsBlock(glass(1.2F, 9.0F)));
        block(registry, "laminated_glass", new TransparentBlock(glass(8.0F, 1200.0F).requiresCorrectToolForDrops()));
        block(registry, "laminated_glass_pane", new IronBarsBlock(glass(8.0F, 1200.0F).requiresCorrectToolForDrops()));
        block(registry, "sapphire_window", new TransparentBlock(glass(25.0F, 600.0F).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));
        // Five times obsidian to cut through and three times its blast resistance: a bunker door is the strongest thing in the bunker.
        blastDoor = new BlastDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(250.0F, 3600.0F)
                .sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false));
        block(registry, "blast_door", blastDoor);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        blastDoorEntity = BlockEntityType.Builder.of(BlastDoorBlockEntity::new, blastDoor).build(null);
        registry.accept(Fundamentals.id("blast_door"), blastDoorEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (String id : ITEMS) {
            item(registry, id, new Item(new Item.Properties()));
        }
        BLOCKS.forEach((id, block) -> item(registry, id, new BlockItem(block, new Item.Properties().stacksTo(id.equals("blast_door") ? 1 : 64))));
        riotShield = item(registry, "riot_shield", new RiotShieldItem(new Item.Properties().durability(RIOT_SHIELD_USES)));
    }

    private static Item item(BiConsumer<ResourceLocation, Item> registry, String id, Item item) {
        ALL.add(item);
        registry.accept(Fundamentals.id(id), item);
        return item;
    }

    /** A polycarbonate riot shield: a shield you can see through, three times as long-lived as oak, mended with a polycarbonate sheet. */
    public static class RiotShieldItem extends net.minecraft.world.item.ShieldItem {
        public RiotShieldItem(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
            return repair.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Fundamentals.id("polycarbonate_sheet")));
        }
    }
}
