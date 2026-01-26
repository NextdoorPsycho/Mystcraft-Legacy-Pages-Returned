package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a drawable D'ni word for Narayan Poems.
 * Each word is composed of multiple symbol components with optional colors.
 *
 * The components are indices into the symbolcomponents.png sprite sheet,
 * which is an 8x8 grid of 64x64 pixel symbols.
 */
@OnlyIn(Dist.CLIENT)
public class DrawableWord {

    /**
     * The default sprite sheet containing D'ni word components.
     * 512x512 texture with 8x8 grid of 64x64 symbols.
     */
    public static final ResourceLocation WORD_COMPONENTS =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/symbolcomponents.png");

    private final List<Integer> components = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private ResourceLocation imageSource = null;

    public DrawableWord() {
    }

    public DrawableWord(Integer... components) {
        this.components.addAll(Arrays.asList(components));
    }

    /**
     * Gets the list of component indices.
     */
    public List<Integer> components() {
        return components;
    }

    /**
     * Gets the list of colors for each component.
     */
    public List<Integer> colors() {
        return colors;
    }

    /**
     * Adds a draw component with a color.
     * @param slot The component index (0-63 for 8x8 grid)
     * @param color The RGB color (0x000000 format)
     * @return This word for chaining
     */
    public DrawableWord addComponent(int slot, int color) {
        components.add(slot);
        colors.add(color);
        return this;
    }

    /**
     * Adds a draw component by grid position.
     * @param x Column (0-7)
     * @param y Row (0-7)
     * @param color The RGB color
     * @return This word for chaining
     */
    public DrawableWord addComponent(int x, int y, int color) {
        return addComponent(x + y * 8, color);
    }

    /**
     * Adds multiple components with the same color.
     */
    public DrawableWord addComponents(int[] components, int color) {
        for (int component : components) {
            addComponent(component, color);
        }
        return this;
    }

    /**
     * Adds multiple components with individual colors.
     */
    public DrawableWord addComponents(int[] components, int[] colors) {
        int defaultColor = colors.length > 0 ? colors[0] : 0;
        for (int i = 0; i < components.length; i++) {
            addComponent(components[i], i < colors.length ? colors[i] : defaultColor);
        }
        return this;
    }

    /**
     * Gets the image source for this word's components.
     * Falls back to the default WORD_COMPONENTS if not set.
     */
    public ResourceLocation imageSource() {
        return imageSource != null ? imageSource : WORD_COMPONENTS;
    }

    /**
     * Sets a custom image source for this word.
     */
    public DrawableWord setImageSource(ResourceLocation source) {
        this.imageSource = source;
        return this;
    }
}
