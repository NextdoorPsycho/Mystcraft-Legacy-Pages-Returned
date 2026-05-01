package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.entity.PersonalPocketProxyEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Renders a personal-pocket proxy as a still player-shaped clone.
 */
public class PersonalPocketProxyRenderer extends EntityRenderer<PersonalPocketProxyEntity> {

  private static final UUID FALLBACK_SKIN_ID = new UUID(0L, 0L);

  private final PlayerModel<PersonalPocketProxyEntity> defaultModel;
  private final PlayerModel<PersonalPocketProxyEntity> slimModel;

  public PersonalPocketProxyRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.defaultModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
    this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    this.shadowRadius = 0.5F;
  }

  private static PlayerInfo getPlayerInfo(UUID ownerId) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.getConnection() == null) {
      return null;
    }
    return minecraft.getConnection().getPlayerInfo(ownerId);
  }

  @Override
  public void render(@NotNull PersonalPocketProxyEntity entity, float entityYaw, float partialTick,
                     @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
    poseStack.pushPose();
    poseStack.translate(0.0D, 1.5D, 0.0D);
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
    poseStack.scale(-1.0F, -1.0F, 1.0F);

    PlayerModel<PersonalPocketProxyEntity> model = getModel(entity);
    model.attackTime = 0.0F;
    model.crouching = false;
    model.young = false;
    model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);

    VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
    model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    poseStack.popPose();

    super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
  }

  @Override
  public @NotNull ResourceLocation getTextureLocation(@NotNull PersonalPocketProxyEntity entity) {
    GameProfile ownerProfile = entity.getOwnerProfile();
    if (ownerProfile != null) {
      PlayerInfo info = getPlayerInfo(ownerProfile.getId());
      if (info != null) {
        return info.getSkinLocation();
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (!ownerProfile.getProperties().isEmpty()) {
        return minecraft.getSkinManager().getInsecureSkinLocation(ownerProfile);
      }
      return DefaultPlayerSkin.getDefaultSkin(ownerProfile.getId());
    }
    return DefaultPlayerSkin.getDefaultSkin(FALLBACK_SKIN_ID);
  }

  private PlayerModel<PersonalPocketProxyEntity> getModel(PersonalPocketProxyEntity entity) {
    GameProfile ownerProfile = entity.getOwnerProfile();
    if (ownerProfile == null) {
      return defaultModel;
    }

    PlayerInfo info = getPlayerInfo(ownerProfile.getId());
    if (info != null && "slim".equals(info.getModelName())) {
      return slimModel;
    }
    return "slim".equals(DefaultPlayerSkin.getSkinModelName(ownerProfile.getId())) ? slimModel : defaultModel;
  }
}
