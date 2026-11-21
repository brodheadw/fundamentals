package ai.gsmc.fundamentals.survey;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

/**
 * A survey camera pointed straight down, like the K-24s that photographed whole countries for their maps: the higher it is
 * flown, the more ground each frame takes in. A frame comes out as a map of the ground below, the camera at its centre tile, with
 * the biomes round it noted by bearing.
 */
public class AerialCameraItem extends Item {

    /** Heights over the ground at which the frame widens to the next map scale, from 128 blocks across to 1,024. */
    static final int[] SCALE_HEIGHTS = {48, 96, 192};
    static final int COOLDOWN = 40;

    public AerialCameraItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack camera = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(camera);
        BlockPos at = player.blockPosition();
        if (!server.canSeeSky(at.above())) {
            player.displayClientMessage(Component.translatable("message.fundamentals.aerial_camera.sky").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(camera);
        }
        int paper = player.getInventory().findSlotMatchingItem(new ItemStack(Items.PAPER));
        if (paper < 0 && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.fundamentals.aerial_camera.paper").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(camera);
        }
        if (paper >= 0 && !player.getAbilities().instabuild) player.getInventory().removeItem(paper, 1);
        ItemStack photo = photograph(server, at);
        if (!player.getInventory().add(photo)) player.drop(photo, false);
        level.playSound(null, at, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.4F);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.success(camera);
    }

    /** A map of the ground under {@code camera}: what is loaded is seen as it lies, what is not only as its biome's tint through the haze. */
    public static ItemStack photograph(ServerLevel level, BlockPos camera) {
        int height = camera.getY() - level.getHeight(Heightmap.Types.MOTION_BLOCKING, camera.getX(), camera.getZ());
        byte scale = 0;
        while (scale < SCALE_HEIGHTS.length && height >= SCALE_HEIGHTS[scale]) scale++;
        ItemStack map = MapItem.create(level, camera.getX(), camera.getZ(), scale, true, false);
        MapItemSavedData data = MapItem.getSavedData(map, level);
        int size = 1 << scale;
        for (int px = 0; px < 128; px++) {
            int north = Integer.MIN_VALUE;
            for (int pz = 0; pz < 128; pz++) {
                int x = data.centerX + (px - 64) * size, z = data.centerZ + (pz - 64) * size;
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) {
                    data.setColor(px, pz, haze(level.getBiome(new BlockPos(x, level.getSeaLevel(), z))).getPackedId(MapColor.Brightness.LOW));
                    north = Integer.MIN_VALUE;
                    continue;
                }
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                BlockPos ground = new BlockPos(x, top, z);
                MapColor colour = chunk.getBlockState(ground).getMapColor(level, ground);
                // shaded by the rise or fall from the pixel to the north, as a vanilla map is
                MapColor.Brightness light = north == Integer.MIN_VALUE || top == north ? MapColor.Brightness.NORMAL
                        : top > north ? MapColor.Brightness.HIGH : MapColor.Brightness.LOW;
                data.setColor(px, pz, colour.getPackedId(colour == MapColor.WATER ? MapColor.Brightness.NORMAL : light));
                north = top;
            }
        }
        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("item.fundamentals.aerial_survey.below", biome(level, camera, 0, 0)).withStyle(ChatFormatting.GRAY));
        int reach = 32 * size;
        for (int b = 0; b < 8; b++) {
            double angle = Math.PI * b / 4;
            int dx = (int) Math.round(Math.sin(angle) * reach), dz = (int) Math.round(-Math.cos(angle) * reach);
            lore.add(Component.translatable("item.fundamentals.aerial_survey.bearing", Survey.bearing(dx, dz), reach, biome(level, camera, dx, dz))
                    .withStyle(ChatFormatting.GRAY));
        }
        map.set(DataComponents.ITEM_NAME, Component.translatable("item.fundamentals.aerial_survey", height));
        map.set(DataComponents.LORE, new ItemLore(lore));
        return map;
    }

    private static Component biome(ServerLevel level, BlockPos camera, int dx, int dz) {
        BlockPos at = camera.offset(dx, 0, dz);
        return level.getBiome(at.atY(level.getSeaLevel())).unwrapKey()
                .<Component>map(key -> Component.translatable(Util.makeDescriptionId("biome", key.location())))
                .orElse(Component.literal("?"));
    }

    /** The tint a biome shows from high up, where its chunks are not loaded to be seen properly. */
    static MapColor haze(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return MapColor.WATER;
        if (biome.is(BiomeTags.IS_NETHER)) return MapColor.NETHER;
        if (biome.is(BiomeTags.IS_BADLANDS)) return MapColor.TERRACOTTA_ORANGE;
        if (biome.is(BiomeTags.IS_BEACH) || biome.is(Tags.Biomes.IS_DESERT)) return MapColor.SAND;
        if (biome.value().getBaseTemperature() < 0.15F) return MapColor.SNOW;
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_TAIGA)) return MapColor.PLANT;
        return MapColor.GRASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fundamentals.aerial_camera").withStyle(ChatFormatting.GRAY));
    }
}
