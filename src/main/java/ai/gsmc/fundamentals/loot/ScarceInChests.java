package ai.gsmc.fundamentals.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

public class ScarceInChests extends LootModifier {

    public static final MapCodec<ScarceInChests> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).and(instance.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(m -> m.items),
            Codec.floatRange(0, 1).fieldOf("keep").forGetter(m -> m.keep))).apply(instance, ScarceInChests::new));

    private final HolderSet<Item> items;
    private final float keep;

    public ScarceInChests(LootItemCondition[] conditions, HolderSet<Item> items, float keep) {
        super(conditions);
        this.items = items;
        this.keep = keep;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        String path = context.getQueriedLootTableId().getPath();
        if (!path.startsWith("chests/") && !path.contains("/chests/")) {
            return loot;
        }
        for (ItemStack stack : loot) {
            if (stack.is(items)) {
                int kept = 0;
                for (int i = 0; i < stack.getCount(); i++) {
                    if (context.getRandom().nextFloat() < keep) {
                        kept++;
                    }
                }
                stack.setCount(kept);
            }
        }
        loot.removeIf(ItemStack::isEmpty);
        return loot;
    }

    @Override
    public MapCodec<? extends ScarceInChests> codec() {
        return CODEC;
    }
}
