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

/** A copper golem built heavier: a boiler for a belly, a long nose, a chimney where the golem has its rod, and long arms for climbing. */
public class AutomatonModel extends HierarchicalModel<Automaton> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Fundamentals.id("automaton"), "main");

    private final ModelPart root, head, rightArm, leftArm, rightLeg, leftLeg;

    public AutomatonModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        PartDefinition head = parts.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -7, -4, 10, 7, 8)
                .texOffs(38, 0).addBox(-1, -4, -6, 2, 4, 2), PartPose.offset(0, 10, 0));
        head.addOrReplaceChild("chimney", CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, -5, -1.5F, 3, 5, 3), PartPose.offset(0, -7, 1.5F));
        parts.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-6, 0, -3.5F, 12, 8, 7), PartPose.offset(0, 10, 0));
        parts.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.5F, 0, -1.5F, 3, 11, 3), PartPose.offset(-7.5F, 11, 0));
        parts.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.5F, 0, -1.5F, 3, 11, 3), PartPose.offset(7.5F, 11, 0));
        parts.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-2, 0, -2, 4, 6, 4), PartPose.offset(-3, 18, 0));
        parts.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-2, 0, -2, 4, 6, 4), PartPose.offset(3, 18, 0));
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
        float stride = Mth.cos(swing * 0.6662F) * 1.2F * amount;
        rightLeg.xRot = stride;
        leftLeg.xRot = -stride;
        if (automaton.onClimbable() && !automaton.onGround()) {
            // hand over hand up the rungs
            float reach = Mth.cos(age * 0.4F) * 0.4F;
            rightArm.xRot = -Mth.PI * 0.85F + reach;
            leftArm.xRot = -Mth.PI * 0.85F - reach;
        } else {
            rightArm.xRot = -stride * 0.8F;
            leftArm.xRot = stride * 0.8F;
        }
        rightArm.zRot = 0.05F;
        leftArm.zRot = -0.05F;
    }
}
