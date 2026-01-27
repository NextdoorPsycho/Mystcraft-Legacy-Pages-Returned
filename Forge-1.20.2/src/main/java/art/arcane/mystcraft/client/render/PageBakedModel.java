package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.PageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom BakedModel wrapper for page items that displays different textures
 * based on the page's symbol content.
 */
@OnlyIn(Dist.CLIENT)
public class PageBakedModel implements BakedModel {

    private final BakedModel baseModel;
    private final ItemOverrides overrides;
    private final Map<ResourceLocation, BakedModel> symbolModelCache = new HashMap<>();

    public PageBakedModel(BakedModel baseModel) {
        this.baseModel = baseModel;
        this.overrides = new PageItemOverrides();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @NotNull RandomSource random) {
        return baseModel.getQuads(state, direction, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        // Return true to use the BEWLR for rendering pages with dynamic symbols
        return true;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return baseModel.getTransforms();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return overrides;
    }

    /**
     * Custom ItemOverrides that returns different models based on page NBT.
     */
    private class PageItemOverrides extends ItemOverrides {

        @Override
        @Nullable
        public BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack stack,
                                  @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            if (!(stack.getItem() instanceof PageItem)) {
                return model;
            }

            // Determine which texture variant to use
            ResourceLocation symbolId = Page.getSymbol(stack);
            boolean isLinkPanel = Page.isLinkPanel(stack);
            boolean isBlank = Page.isBlank(stack);

            if (isBlank && !isLinkPanel && symbolId == null) {
                // Blank page - use base model
                return baseModel;
            }

            // Get cache key
            ResourceLocation cacheKey;
            if (isLinkPanel) {
                cacheKey = new ResourceLocation("mystcraft", "page_linkpanel");
            } else if (symbolId != null) {
                cacheKey = symbolId;
            } else {
                return baseModel;
            }

            // Check cache
            if (symbolModelCache.containsKey(cacheKey)) {
                return symbolModelCache.get(cacheKey);
            }

            // For now, return base model - the dynamic texture system needs
            // texture atlas integration which is more complex
            // The textures are being generated but we need sprite registration
            return baseModel;
        }
    }
}
