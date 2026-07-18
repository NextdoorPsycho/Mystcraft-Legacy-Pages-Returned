package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.entity.PersonalPocketProxyEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/** Renders a personal-pocket proxy as a still player-shaped clone. */
public class PersonalPocketProxyRenderer
    extends EntityRenderer<PersonalPocketProxyEntity, PersonalPocketProxyRenderer.State> {

  private static final UUID FALLBACK_SKIN_ID = new UUID(0L, 0L);

  private final PlayerModel defaultModel;
  private final PlayerModel slimModel;
  private final Map<UUID, Supplier<PlayerSkin>> skinLookups = new HashMap<>();

  public PersonalPocketProxyRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.defaultModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false);
    this.slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    this.shadowRadius = 0.5F;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(PersonalPocketProxyEntity entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.bodyRot = entity.getYRot(partialTicks);
    state.yRot = 0.0F;
    state.xRot = 0.0F;
    state.attackTime = 0.0F;
    state.isCrouching = false;
    state.isBaby = false;
    state.skin = resolveSkin(entity.getOwnerProfile());
    state.slim = state.skin.model() == PlayerModelType.SLIM;
  }

  private PlayerSkin resolveSkin(GameProfile profile) {
    if (profile == null) {
      return DefaultPlayerSkin.get(FALLBACK_SKIN_ID);
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.getConnection() != null) {
      PlayerInfo info = minecraft.getConnection().getPlayerInfo(profile.id());
      if (info != null) {
        return info.getSkin();
      }
    }
    Supplier<PlayerSkin> lookup = skinLookups.computeIfAbsent(
        profile.id(),
        ignored -> minecraft.getSkinManager().createLookup(profile, false));
    return lookup.get();
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                     CameraRenderState camera) {
    poseStack.pushPose();
    poseStack.translate(0.0F, 1.5F, 0.0F);
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
    poseStack.scale(-1.0F, -1.0F, 1.0F);
    PlayerModel model = state.slim ? slimModel : defaultModel;
    submitNodeCollector.submitModel(
        model,
        state,
        poseStack,
        RenderTypes.entityTranslucent(state.skin.body().texturePath()),
        state.lightCoords,
        OverlayTexture.NO_OVERLAY,
        -1,
        null,
        state.outlineColor,
        null);
    poseStack.popPose();
    super.submit(state, poseStack, submitNodeCollector, camera);
  }

  public static final class State extends AvatarRenderState {
    private boolean slim;
  }
}
