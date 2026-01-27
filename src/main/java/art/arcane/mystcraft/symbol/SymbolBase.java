package art.arcane.mystcraft.symbol;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Base implementation of IAgeSymbol.
 * Provides common functionality for all symbols.
 */
public abstract class SymbolBase implements IAgeSymbol {

    protected final ResourceLocation registryName;
    protected final SymbolCategory category;
    protected String[] poemWords;
    protected Integer cardRank;
    protected float instabilityCost = 0.0f;

    @OnlyIn(Dist.CLIENT)
    private String cachedLocalizedName;

    public SymbolBase(ResourceLocation registryName, SymbolCategory category) {
        this.registryName = registryName;
        this.category = category;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public SymbolCategory getCategory() {
        return category;
    }

    public SymbolBase setPoem(String... words) {
        this.poemWords = words;
        return this;
    }

    @Override
    public String[] getPoem() {
        return poemWords;
    }

    public SymbolBase setCardRank(Integer rank) {
        this.cardRank = rank;
        return this;
    }

    @Override
    public Integer getCardRank() {
        return cardRank;
    }

    public SymbolBase setInstabilityCost(float cost) {
        this.instabilityCost = cost;
        return this;
    }

    @Override
    public float getInstabilityCost() {
        return instabilityCost;
    }

    protected String getUnlocalizedName() {
        return "myst.symbol." + registryName.getNamespace() + "." + registryName.getPath();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getLocalizedName() {
        if (cachedLocalizedName == null) {
            cachedLocalizedName = I18n.get(getUnlocalizedName());
        }
        return cachedLocalizedName;
    }

    @Override
    public String toString() {
        return "Symbol[" + registryName + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IAgeSymbol other)) return false;
        return registryName.equals(other.getRegistryName());
    }

    @Override
    public int hashCode() {
        return registryName.hashCode();
    }
}
