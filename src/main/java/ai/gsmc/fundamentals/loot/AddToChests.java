package ai.gsmc.fundamentals.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

public class AddToChests extends LootModifier {

    public record Entry(Item item, int weight, int min, int max) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Entry::weight),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("min", 1).forGetter(Entry::min),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("max", 1).forGetter(Entry::max)).apply(instance, Entry::new));
    }

    public static final MapCodec<AddToChests> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).and(instance.group(
            Codec.STRING.listOf().fieldOf("tables").forGetter(m -> m.tables),
            Codec.floatRange(0, 1).fieldOf("chance").forGetter(m -> m.chance),
            Entry.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("pool").forGetter(m -> m.pool))).apply(instance, AddToChests::new));

    private final List<String> tables;
    private final float chance;
    private final List<Entry> pool;
    private final int totalWeight;

    public AddToChests(LootItemCondition[] conditions, List<String> tables, float chance, List<Entry> pool) {
        super(conditions);
        this.tables = tables;
        this.chance = chance;
        this.pool = pool;
        this.totalWeight = pool.stream().mapToInt(Entry::weight).sum();
    }

    private boolean matches(String table) {
        return tables.stream().anyMatch(t -> t.endsWith("/") ? table.startsWith(t) : table.equals(t));
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        RandomSource random = context.getRandom();
        if (!matches(context.getQueriedLootTableId().toString()) || random.nextFloat() >= chance) {
            return loot;
        }
        int pick = random.nextInt(totalWeight);
        for (Entry entry : pool) {
            pick -= entry.weight();
            if (pick < 0) {
                loot.add(new ItemStack(entry.item(), entry.min() + random.nextInt(Math.max(1, entry.max() - entry.min() + 1))));
                break;
            }
        }
        return loot;
    }

    @Override
    public MapCodec<? extends AddToChests> codec() {
        return CODEC;
    }
}
