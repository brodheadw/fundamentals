package ai.gsmc.fundamentals.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Turns one item in a loot roll into another, count for count: an iron golem's ingots into nuggets, a drowned's copper ingot into malachite. */
public class SwapDrop extends LootModifier {

    public static final MapCodec<SwapDrop> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).and(instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("from").forGetter(m -> m.from),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("to").forGetter(m -> m.to))).apply(instance, SwapDrop::new));

    private final Item from;
    private final Item to;

    public SwapDrop(LootItemCondition[] conditions, Item from, Item to) {
        super(conditions);
        this.from = from;
        this.to = to;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        loot.replaceAll(stack -> stack.is(from) ? new ItemStack(to, stack.getCount()) : stack);
        return loot;
    }

    @Override
    public MapCodec<? extends SwapDrop> codec() {
        return CODEC;
    }
}
