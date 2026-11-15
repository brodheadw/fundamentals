package ai.gsmc.fundamentals.automaton;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import java.util.List;
import java.util.function.BiConsumer;

/** Steam automata: the walking machine, its cam-drum brain, the item you carry one about as, and the punched cards it reads. */
public final class Automata {

    private static EntityType<Automaton> entity;
    private static Item item, camDrum, carryCard, buildCard;

    private Automata() {}

    public static EntityType<Automaton> entity() { return entity; }
    public static Item item() { return item; }
    public static Item camDrum() { return camDrum; }
    public static Item carryCard() { return carryCard; }
    public static Item buildCard() { return buildCard; }

    public static List<Item> items() {
        return List.of(camDrum, item, carryCard, buildCard);
    }

    public static void registerEntities(BiConsumer<ResourceLocation, EntityType<?>> registry) {
        entity = EntityType.Builder.<Automaton>of(Automaton::new, MobCategory.MISC).sized(0.8F, 1.3F).eyeHeight(1.0F)
                .clientTrackingRange(10).build(Fundamentals.id("automaton").toString());
        registry.accept(Fundamentals.id("automaton"), entity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        camDrum = new Item(new Item.Properties());
        registry.accept(Fundamentals.id("cam_drum"), camDrum);
        item = new AutomatonItem(new Item.Properties().stacksTo(1));
        registry.accept(Fundamentals.id("automaton"), item);
        carryCard = new ProgramCard(ProgramCard.Kind.CARRY, new Item.Properties().stacksTo(1));
        registry.accept(Fundamentals.id("carry_card"), carryCard);
        buildCard = new ProgramCard(ProgramCard.Kind.BUILD, new Item.Properties().stacksTo(1));
        registry.accept(Fundamentals.id("build_card"), buildCard);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EntityAttributeCreationEvent.class, event -> event.put(entity, Automaton.attributes().build()));
    }
}
