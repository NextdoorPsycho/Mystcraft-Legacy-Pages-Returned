package art.arcane.mystcraft.client.model;

import art.arcane.mystcraft.Mystcraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Bookstand block entity model. Uses the 64x32 entity texture.
 */
public class BookstandModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(Mystcraft.MOD_ID, "bookstand"), "main");

    private final ModelPart leftArm;
    private final ModelPart post;
    private final ModelPart base;
    private final ModelPart rightArm;

    public BookstandModel(ModelPart root) {
        super(RenderType::entitySolid);
        this.leftArm = root.getChild("left_arm");
        this.post = root.getChild("post");
        this.base = root.getChild("base");
        this.rightArm = root.getChild("right_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Original values from ModelBookstand.java:
        // leftarm: addBox(-0.5F, -0.5F, -1.5F, 6, 1, 3) at (0.25F, 0F, -0.25F), rotation (0.5235988F, 0F, -0.2617994F), texOffset (4, 8)
        // post: addBox(-0.5F, 0F, -0.5F, 1, 6, 1) at (0F, 0F, 0F), texOffset (0, 8)
        // base: addBox(-2.5F, 0F, -2.5F, 5, 3, 5) at (0F, 5F, 0F), texOffset (0, 0)
        // rightarm: addBox(-0.5F, -0.5F, -1.5F, 6, 1, 3) at (-0.25F, 0F, -0.25F), rotation (-0.5235988F, 3.141593F, 0.2617994F), texOffset (4, 8)

        // Base - bottom foundation
        partdefinition.addOrReplaceChild("base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.5F, 0F, -2.5F, 5, 3, 5),
                PartPose.offset(0F, 5F, 0F));

        // Post - vertical support
        partdefinition.addOrReplaceChild("post",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-0.5F, 0F, -0.5F, 1, 6, 1),
                PartPose.ZERO);

        // Left arm - angled support (30 degrees up, slight tilt)
        partdefinition.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(4, 8)
                        .addBox(-0.5F, -0.5F, -1.5F, 6, 1, 3),
                PartPose.offsetAndRotation(0.25F, 0F, -0.25F,
                        0.5235988F, 0F, -0.2617994F));

        // Right arm - angled support (opposite direction)
        partdefinition.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(4, 8)
                        .mirror()
                        .addBox(-0.5F, -0.5F, -1.5F, 6, 1, 3),
                PartPose.offsetAndRotation(-0.25F, 0F, -0.25F,
                        -0.5235988F, 3.141593F, 0.2617994F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        base.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        post.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        leftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        rightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
