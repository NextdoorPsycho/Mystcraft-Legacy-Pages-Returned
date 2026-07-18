package art.arcane.mystcraft.symbol;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Base implementation of IAgeSymbol. Provides common functionality for all
 * symbols.
 */
public abstract class SymbolBase implements IAgeSymbol {

  protected final ResourceLocation registryName;
  protected final SymbolCategory category;
  protected String[] poemWords;
  protected Integer cardRank;
  protected float instabilityCost = 0.0f;
  protected boolean duplicatable = false;

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

  @Override
  public String[] getPoem() {
    return poemWords;
  }

  public SymbolBase setPoem(String... words) {
    this.poemWords = words;
    return this;
  }

  @Override
  public Integer getCardRank() {
    return cardRank;
  }

  public SymbolBase setCardRank(Integer rank) {
    this.cardRank = rank;
    return this;
  }

  @Override
  public float getInstabilityCost() {
    return instabilityCost;
  }

  public SymbolBase setInstabilityCost(float cost) {
    this.instabilityCost = cost;
    return this;
  }

  public SymbolBase setDuplicatable(boolean duplicatable) {
    this.duplicatable = duplicatable;
    return this;
  }

  @Override
  public boolean canDuplicate() {
    return duplicatable;
  }

  protected String getUnlocalizedName() {
    return "myst.symbol." + registryName.getNamespace() + "." + registryName.getPath();
  }

  @Override
  public String getLocalizedName() {
    return Component.translatable(getUnlocalizedName()).getString();
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
