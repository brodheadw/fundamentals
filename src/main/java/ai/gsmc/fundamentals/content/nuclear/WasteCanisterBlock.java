package ai.gsmc.fundamentals.content.nuclear;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A sealed canister of high-level waste. It irradiates whoever stands near it until it is entombed in a geological
 * repository: below y 0, every face against deepslate, tuff or clay.
 */
public class WasteCanisterBlock extends Block {

    public static final TagKey<Block> REPOSITORY_ROCK = TagKey.create(Registries.BLOCK, Fundamentals.id("repository_rock"));
    public static final int REACH = 8, PULSE_TICKS = 40;
    private static final ResourceLocation RADIATION = ResourceLocation.fromNamespaceAndPath("createnuclear", "radiation");

    public WasteCanisterBlock(Properties properties) {
        super(properties);
    }

    public static boolean entombed(Level level, BlockPos pos) {
        if (pos.getY() >= 0) return false;
        for (Direction direction : Direction.values()) {
            if (!level.getBlockState(pos.relative(direction)).is(REPOSITORY_ROCK)) return false;
        }
        return true;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        level.scheduleTick(pos, this, PULSE_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, PULSE_TICKS);
        if (entombed(level, pos)) return;
        // Create: Nuclear's radiation when it is installed, withering when it is not
        Holder<MobEffect> radiation = BuiltInRegistries.MOB_EFFECT.getHolder(RADIATION).<Holder<MobEffect>>map(holder -> holder).orElse(MobEffects.WITHER);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(REACH))) {
            entity.addEffect(new MobEffectInstance(radiation, PULSE_TICKS * 3, 0));
        }
    }
}
