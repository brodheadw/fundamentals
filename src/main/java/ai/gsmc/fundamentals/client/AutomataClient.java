package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.automaton.Automata;
import ai.gsmc.fundamentals.automaton.Automaton;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class AutomataClient {
    private static final ResourceLocation TEXTURE = Fundamentals.id("textures/entity/automaton.png");
    private static final RenderType GLOW = RenderType.eyes(Fundamentals.id("textures/entity/automaton_glow.png"));

    private AutomataClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> event.registerLayerDefinition(AutomatonModel.LAYER, AutomatonModel::create));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerEntityRenderer(Automata.entity(), Renderer::new));
    }

    static final class Renderer extends MobRenderer<Automaton, AutomatonModel> {
        Renderer(EntityRendererProvider.Context context) {
            super(context, new AutomatonModel(context.bakeLayer(AutomatonModel.LAYER)), 0.5F);
            addLayer(new Lenses(this));
        }

        @Override
        public ResourceLocation getTextureLocation(Automaton automaton) {
            return TEXTURE;
        }
    }

    /** The lenses and the gauge glow in the dark. */
    static final class Lenses extends EyesLayer<Automaton, AutomatonModel> {
        Lenses(RenderLayerParent<Automaton, AutomatonModel> parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return GLOW;
        }
    }
}
