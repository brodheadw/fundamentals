package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.automaton.Automaton;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Clockwork in brass plate: a domed helm with a faceplate and two lenses, an open chest where the great wheel turns,
 *  a boiler on the back with twin exhausts, iron rods for upper limbs and plated forearms and boots. */
public class AutomatonModel extends HierarchicalModel<Automaton> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Fundamentals.id("automaton"), "main");

    private final ModelPart root, head, body, gear, rightArm, leftArm, rightLeg, leftLeg;

    public AutomatonModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.gear = body.getChild("gear");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(32, 0).addBox(-0.5F, -10, -3, 1, 2, 7)
                .texOffs(48, 9).addBox(-1.5F, 0, -1.5F, 3, 1, 3), PartPose.offset(0, 8, 0));
        PartDefinition body = parts.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-5, 0, -3, 10, 8, 6)
                .texOffs(32, 16).addBox(-4, 8, -2.5F, 8, 2, 5)
                .texOffs(32, 24).addBox(-3, 1, 3, 6, 6, 3)
                .texOffs(52, 24).addBox(1.5F, -4, 4, 2, 9, 2)
                .texOffs(52, 24).addBox(-3.5F, -4, 4, 2, 9, 2), PartPose.offset(0, 9, 0));
        body.addOrReplaceChild("gear", CubeListBuilder.create().texOffs(20, 48).addBox(-4, -4, -0.5F, 8, 8, 1), PartPose.offset(0, 4, -3.2F));
        CubeListBuilder arm = CubeListBuilder.create()
                .texOffs(20, 36).addBox(-2.5F, -1.5F, -2.5F, 4, 3, 5)
                .texOffs(40, 36).addBox(-1.5F, 1.5F, -1, 2, 5, 2)
                .texOffs(48, 36).addBox(-2, 6.5F, -1.5F, 3, 6, 3);
        parts.addOrReplaceChild("right_arm", arm, PartPose.offset(-6, 10, 0));
        parts.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
                .texOffs(20, 36).addBox(-1.5F, -1.5F, -2.5F, 4, 3, 5)
                .texOffs(40, 36).addBox(-0.5F, 1.5F, -1, 2, 5, 2)
                .texOffs(48, 36).addBox(-1, 6.5F, -1.5F, 3, 6, 3), PartPose.offset(6, 10, 0));
        CubeListBuilder leg = CubeListBuilder.create()
                .texOffs(0, 36).addBox(-1, 0, -1, 2, 3, 2)
                .texOffs(0, 42).addBox(-2, 3, -2.5F, 4, 2, 5);
        parts.addOrReplaceChild("right_leg", leg, PartPose.offset(-2.5F, 19, 0));
        parts.addOrReplaceChild("left_leg", leg, PartPose.offset(2.5F, 19, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Automaton automaton, float swing, float amount, float age, float headYaw, float headPitch) {
        head.yRot = headYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        // the great wheel always turns while there is steam, faster as it walks
        gear.zRot = automaton.steam() > 0 ? age * 0.06F + swing * 0.5F : 0;
        float stride = Mth.cos(swing * 0.6662F) * 1.1F * amount;
        rightLeg.xRot = stride;
        leftLeg.xRot = -stride;
        body.y = 9 + Math.abs(Mth.sin(swing * 0.6662F)) * amount * 0.6F;
        if (automaton.onClimbable() && !automaton.onGround()) {
            float reach = Mth.cos(age * 0.4F) * 0.4F;
            rightArm.xRot = -Mth.PI * 0.85F + reach;
            leftArm.xRot = -Mth.PI * 0.85F - reach;
        } else {
            rightArm.xRot = -stride * 0.7F;
            leftArm.xRot = stride * 0.7F;
        }
        rightArm.zRot = 0.08F;
        leftArm.zRot = -0.08F;
    }
}
