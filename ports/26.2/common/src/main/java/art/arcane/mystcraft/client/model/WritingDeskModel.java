package art.arcane.mystcraft.client.model;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/**
 * Writing Desk block entity model. Uses a 256x128 texture (displayed as
 * 2048x1024 in the actual file - 8x scale).
 */
public class WritingDeskModel extends Model<Unit> {

  public static final ModelLayerLocation LAYER_LOCATION =
      new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "writing_desk"), "main");

  public WritingDeskModel(ModelPart root) {
    super(root, RenderTypes::entitySolid);
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition partdefinition = meshdefinition.getRoot();

    partdefinition.addOrReplaceChild("bottom_shelf",
        CubeListBuilder.create()
            .texOffs(0, 34)
            .addBox(0F, 0F, 0F, 14, 1, 15),
        PartPose.offset(-7F, 23F, -8F));

    partdefinition.addOrReplaceChild("middle_shelf",
        CubeListBuilder.create()
            .texOffs(0, 17)
            .addBox(0F, 0F, 0F, 30, 2, 15),
        PartPose.offset(-7F, 15F, -8F));

    partdefinition.addOrReplaceChild("desk_top",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(0F, 0F, 0F, 32, 1, 16),
        PartPose.offset(-8F, 8F, -8F));

    partdefinition.addOrReplaceChild("desk_middle",
        CubeListBuilder.create()
            .texOffs(94, 36)
            .addBox(0F, 0F, 0F, 2, 6, 15),
        PartPose.offset(7F, 9F, -8F));

    partdefinition.addOrReplaceChild("desk_left",
        CubeListBuilder.create()
            .texOffs(90, 1)
            .addBox(0F, 0F, 0F, 1, 15, 16),
        PartPose.offset(-8F, 9F, -8F));

    partdefinition.addOrReplaceChild("desk_right",
        CubeListBuilder.create()
            .texOffs(90, 1)
            .mirror()
            .addBox(0F, 0F, 0F, 1, 15, 16),
        PartPose.offset(23F, 9F, -8F));

    partdefinition.addOrReplaceChild("desk_back",
        CubeListBuilder.create()
            .texOffs(128, 0)
            .addBox(0F, 0F, 0F, 30, 15, 1),
        PartPose.offset(-7F, 9F, 7F));

    partdefinition.addOrReplaceChild("desk_middle_bottom",
        CubeListBuilder.create()
            .texOffs(77, 42)
            .addBox(0F, 0F, 0F, 1, 7, 15),
        PartPose.offset(7F, 17F, -8F));

    partdefinition.addOrReplaceChild("desk_top_back",
        CubeListBuilder.create()
            .texOffs(128, 16)
            .addBox(0F, 0F, 0F, 32, 12, 1),
        PartPose.offset(-8F, -4F, 7F));

    partdefinition.addOrReplaceChild("desk_top_left",
        CubeListBuilder.create()
            .texOffs(146, 40)
            .addBox(0F, 0F, 0F, 1, 12, 6),
        PartPose.offset(-8F, -4F, 1F));

    partdefinition.addOrReplaceChild("desk_top_right",
        CubeListBuilder.create()
            .texOffs(146, 40)
            .addBox(0F, 0F, 0F, 1, 12, 6),
        PartPose.offset(23F, -4F, 1F));

    partdefinition.addOrReplaceChild("desk_top_top",
        CubeListBuilder.create()
            .texOffs(128, 29)
            .addBox(0F, 0F, 0F, 30, 1, 6),
        PartPose.offset(-7F, -4F, 1F));

    partdefinition.addOrReplaceChild("floor",
        CubeListBuilder.create()
            .texOffs(0, 34)
            .addBox(0F, 0.01F, 0F, 32, 1, 16),
        PartPose.offset(-8F, 23F, -8F));

    partdefinition.addOrReplaceChild("floor2",
        CubeListBuilder.create(),
        PartPose.ZERO);

    return LayerDefinition.create(meshdefinition, 256, 128);
  }

  @Override
  public void setupAnim(Unit state) {
    // The desk is static; keeping the state hook explicit makes submission
    // compatible with Minecraft's extracted render-state pipeline.
  }
}
