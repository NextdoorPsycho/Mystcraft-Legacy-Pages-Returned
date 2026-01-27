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
 * Writing Desk block entity model.
 * Uses a 256x128 texture (displayed as 2048x1024 in the actual file - 8x scale).
 */
public class WritingDeskModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(Mystcraft.MOD_ID, "writing_desk"), "main");

    private final ModelPart bottomShelf;
    private final ModelPart middleShelf;
    private final ModelPart deskTop;
    private final ModelPart deskLeft;
    private final ModelPart deskRight;
    private final ModelPart deskBack;
    private final ModelPart deskMiddle;
    private final ModelPart deskMiddleBottom;
    private final ModelPart deskTopBack;
    private final ModelPart deskTopLeft;
    private final ModelPart deskTopRight;
    private final ModelPart deskTopTop;
    private final ModelPart floor;
    private final ModelPart floor2;

    public WritingDeskModel(ModelPart root) {
        super(RenderType::entitySolid);
        this.bottomShelf = root.getChild("bottom_shelf");
        this.middleShelf = root.getChild("middle_shelf");
        this.deskTop = root.getChild("desk_top");
        this.deskLeft = root.getChild("desk_left");
        this.floor = root.getChild("floor");
        this.deskRight = root.getChild("desk_right");
        this.deskBack = root.getChild("desk_back");
        this.deskMiddle = root.getChild("desk_middle");
        this.deskMiddleBottom = root.getChild("desk_middle_bottom");
        this.deskTopBack = root.getChild("desk_top_back");
        this.deskTopLeft = root.getChild("desk_top_left");
        this.deskTopRight = root.getChild("desk_top_right");
        this.deskTopTop = root.getChild("desk_top_top");
        this.floor2 = root.getChild("floor2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Bottom shelf
        partdefinition.addOrReplaceChild("bottom_shelf",
                CubeListBuilder.create()
                        .texOffs(0, 34)
                        .addBox(0F, 0F, 0F, 14, 1, 15),
                PartPose.offset(-7F, 23F, -8F));

        // Middle shelf
        partdefinition.addOrReplaceChild("middle_shelf",
                CubeListBuilder.create()
                        .texOffs(0, 17)
                        .addBox(0F, 0F, 0F, 30, 2, 15),
                PartPose.offset(-7F, 15F, -8F));

        // Desk top surface
        partdefinition.addOrReplaceChild("desk_top",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(0F, 0F, 0F, 32, 1, 16),
                PartPose.offset(-8F, 8F, -8F));

        // Desk middle vertical divider
        partdefinition.addOrReplaceChild("desk_middle",
                CubeListBuilder.create()
                        .texOffs(94, 36)
                        .addBox(0F, 0F, 0F, 2, 6, 15),
                PartPose.offset(7F, 9F, -8F));

        // Desk left side
        partdefinition.addOrReplaceChild("desk_left",
                CubeListBuilder.create()
                        .texOffs(90, 1)
                        .addBox(0F, 0F, 0F, 1, 15, 16),
                PartPose.offset(-8F, 9F, -8F));

        // Desk right side
        partdefinition.addOrReplaceChild("desk_right",
                CubeListBuilder.create()
                        .texOffs(90, 1)
                        .mirror()
                        .addBox(0F, 0F, 0F, 1, 15, 16),
                PartPose.offset(23F, 9F, -8F));

        // Desk back
        partdefinition.addOrReplaceChild("desk_back",
                CubeListBuilder.create()
                        .texOffs(128, 0)
                        .addBox(0F, 0F, 0F, 30, 15, 1),
                PartPose.offset(-7F, 9F, 7F));

        // Desk middle bottom
        partdefinition.addOrReplaceChild("desk_middle_bottom",
                CubeListBuilder.create()
                        .texOffs(77, 42)
                        .addBox(0F, 0F, 0F, 1, 7, 15),
                PartPose.offset(7F, 17F, -8F));

        // Top hutch back
        partdefinition.addOrReplaceChild("desk_top_back",
                CubeListBuilder.create()
                        .texOffs(128, 16)
                        .addBox(0F, 0F, 0F, 32, 12, 1),
                PartPose.offset(-8F, -4F, 7F));

        // Top hutch left
        partdefinition.addOrReplaceChild("desk_top_left",
                CubeListBuilder.create()
                        .texOffs(146, 40)
                        .addBox(0F, 0F, 0F, 1, 12, 6),
                PartPose.offset(-8F, -4F, 1F));

        // Top hutch right
        partdefinition.addOrReplaceChild("desk_top_right",
                CubeListBuilder.create()
                        .texOffs(146, 40)
                        .addBox(0F, 0F, 0F, 1, 12, 6),
                PartPose.offset(23F, -4F, 1F));

        // Top shelf
        partdefinition.addOrReplaceChild("desk_top_top",
                CubeListBuilder.create()
                        .texOffs(128, 29)
                        .addBox(0F, 0F, 0F, 30, 1, 6),
                PartPose.offset(-7F, -4F, 1F));

        // Floor to close off bottom - single piece covering the full desk footprint
        // Positioned at Y=22.99 to avoid z-fighting with ground
        partdefinition.addOrReplaceChild("floor",
                CubeListBuilder.create()
                        .texOffs(0, 34)
                        .addBox(0F, 0.01F, 0F, 32, 1, 16),
                PartPose.offset(-8F, 23F, -8F));

        // Empty placeholder to satisfy model part requirements (no geometry)
        partdefinition.addOrReplaceChild("floor2",
                CubeListBuilder.create(),
                PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 256, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        bottomShelf.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        middleShelf.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskTop.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskLeft.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskRight.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskBack.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskMiddle.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskMiddleBottom.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskTopBack.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskTopLeft.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskTopRight.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        deskTopTop.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        floor.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        floor2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
