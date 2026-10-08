package ai.gsmc.fundamentals.client.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Hold W over a casing: a plant stage with its mixer, ends, sides and lever, and what each is for. */
public final class MixerSettlerScenes {

    private MixerSettlerScenes() {}

    public static void battery(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mixer_settler", MixerSettlerPonderText.HEADER);
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos hatch = util.grid().at(3, 2, 3);
        BlockPos mixer = hatch.above();
        String[] t = MixerSettlerPonderText.TEXTS;

        // the vat alone
        scene.world().showSection(util.select().fromTo(3, 1, 2, 5, 2, 4), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90).text(t[0]).pointAt(util.vector().topOf(4, 2, 3)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        // the mixer over the hatch
        scene.world().showSection(util.select().fromTo(3, 3, 3, 3, 3, 4), Direction.DOWN);
        scene.idle(5);
        scene.world().setKineticSpeed(util.select().fromTo(3, 3, 3, 3, 3, 4), 64);
        scene.overlay().showText(100).text(t[1]).pointAt(util.vector().blockSurface(mixer, Direction.WEST)).placeNearTarget().attachKeyFrame();
        scene.idle(110);

        // P507 from above
        scene.world().showSection(util.select().fromTo(4, 3, 2, 4, 4, 2), Direction.DOWN);
        scene.idle(5);
        scene.world().setKineticSpeed(util.select().position(4, 3, 2), 64);
        scene.overlay().showText(90).text(t[2]).pointAt(util.vector().blockSurface(util.grid().at(4, 4, 2), Direction.WEST)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        // the ends and the sides
        scene.world().showSection(util.select().fromTo(1, 1, 3, 2, 1, 3), Direction.EAST);
        scene.world().showSection(util.select().fromTo(6, 1, 3, 7, 1, 3), Direction.WEST);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 1, 0, 3, 1, 1), Direction.SOUTH);
        scene.world().showSection(util.select().fromTo(5, 1, 5, 5, 1, 6), Direction.NORTH);
        scene.idle(5);
        scene.world().setKineticSpeed(util.select().position(2, 1, 3), 64);
        scene.world().setKineticSpeed(util.select().position(6, 1, 3), 64);
        scene.world().setKineticSpeed(util.select().position(3, 1, 1), 64);
        scene.world().setKineticSpeed(util.select().position(5, 1, 5), 64);
        scene.overlay().showText(110).text(t[3]).pointAt(util.vector().blockSurface(util.grid().at(1, 1, 3), Direction.UP)).placeNearTarget().attachKeyFrame();
        scene.idle(120);

        // the lever
        scene.world().showSection(util.select().position(2, 2, 2), Direction.EAST);
        scene.idle(5);
        scene.world().toggleRedstonePower(util.select().position(2, 2, 2));
        scene.overlay().showText(110).text(t[4]).pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.WEST)).placeNearTarget().attachKeyFrame();
        scene.idle(120);

        // the sump
        scene.overlay().showText(100).text(t[5]).pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.DOWN)).placeNearTarget().attachKeyFrame();
        scene.idle(110);

        // the goggles
        scene.overlay().showText(90).text(t[6]).pointAt(util.vector().topOf(5, 2, 4)).placeNearTarget().attachKeyFrame();
        scene.idle(100);
        scene.markAsFinished();
    }
}
