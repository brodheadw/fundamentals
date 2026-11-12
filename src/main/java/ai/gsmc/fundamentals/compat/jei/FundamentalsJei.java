package ai.gsmc.fundamentals.compat.jei;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.kiln.Kiln;
import ai.gsmc.fundamentals.kiln.KilnRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Shows what the rotary kiln roasts, and how hot and how long. Only JEI loads this class; EMI shows it through JEI's plugins. */
@JeiPlugin
public class FundamentalsJei implements IModPlugin {

    private static final RecipeType<KilnRecipe> KILN = RecipeType.create(Fundamentals.MOD_ID, "kiln", KilnRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return Fundamentals.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new KilnCategory(gui.createDrawableItemLike(Kiln.drum()), gui.getRecipeArrow()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        registration.addRecipes(KILN, level.getRecipeManager().getAllRecipesFor(KilnRecipe.TYPE).stream().map(RecipeHolder::value).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Kiln.drum()), KILN);
    }

    private record KilnCategory(IDrawable icon, IDrawableStatic arrow) implements IRecipeCategory<KilnRecipe> {

        @Override public RecipeType<KilnRecipe> getRecipeType() { return KILN; }
        @Override public Component getTitle() { return Component.translatable("fundamentals.jei.kiln"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 120; }
        @Override public int getHeight() { return 40; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, KilnRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(4, 4).setStandardSlotBackground().addIngredients(recipe.ingredient());
            for (int i = 0; i < recipe.results().size(); i++) {
                builder.addOutputSlot(62 + 20 * i, 4).setOutputSlotBackground().addItemStack(recipe.results().get(i));
            }
        }

        @Override
        public void draw(KilnRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            arrow.draw(graphics, 28, 5);
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("fundamentals.jei.kiln.needs",
                    recipe.celsius(), String.format("%.1f", recipe.ticks() / 20.0)), 4, 28, 0xFF808080, false);
        }
    }
}
