package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.api.symbol.GrammarBindingMode;
import art.arcane.mystcraft.api.symbol.IGrammarBinding;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Datapack-backed symbol implementation.
 */
public class DataSymbol extends SymbolBase implements IGrammarBinding {

  private final boolean allowRandom;
  private final String displayName;
  private final GrammarBindingMode grammarMode;
  private final ResourceLocation grammarToken;
  private final Integer grammarRank;
  private final List<SymbolLogic> logic;
  private final SymbolDisplay display;

  public DataSymbol(ResourceLocation id,
                    SymbolCategory category,
                    Integer cardRank,
                    float instabilityCost,
                    String[] poem,
                    boolean allowRandom,
                    boolean canDuplicate,
                    GrammarBindingMode grammarMode,
                    ResourceLocation grammarToken,
                    Integer grammarRank,
                    List<SymbolLogic> logic,
                    String displayName,
                    SymbolDisplay display) {
    super(id, category);
    this.allowRandom = allowRandom;
    this.displayName = displayName;
    this.grammarMode = grammarMode;
    this.grammarToken = grammarToken;
    this.grammarRank = grammarRank;
    this.logic = logic;
    this.display = display;
    if (cardRank != null) {
      setCardRank(cardRank);
    }
    setInstabilityCost(instabilityCost);
    if (poem != null) {
      setPoem(poem);
    }
    setDuplicatable(canDuplicate);
  }

  /**
   * Backwards-compatible constructor for callers that haven't yet
   * threaded a {@link SymbolDisplay}. Uses {@code null} (= category
   * default rendering) so the auto-generated symbol-page path takes
   * over.
   */
  public DataSymbol(ResourceLocation id,
                    SymbolCategory category,
                    Integer cardRank,
                    float instabilityCost,
                    String[] poem,
                    boolean allowRandom,
                    boolean canDuplicate,
                    GrammarBindingMode grammarMode,
                    ResourceLocation grammarToken,
                    Integer grammarRank,
                    List<SymbolLogic> logic,
                    String displayName) {
    this(id, category, cardRank, instabilityCost, poem, allowRandom, canDuplicate,
        grammarMode, grammarToken, grammarRank, logic, displayName, null);
  }

  @Override
  public void registerLogic(AgeDirector director, long seed) {
    if (logic == null || logic.isEmpty()) {
      return;
    }
    for (SymbolLogic entry : logic) {
      entry.apply(director, seed);
    }
  }

  @Override
  public boolean allowInRandomGeneration() {
    return allowRandom;
  }

  @Override
  public boolean canDuplicate() {
    return duplicatable;
  }

  @Override
  public String getLocalizedName() {
    if (displayName != null && !displayName.isBlank()) {
      return displayName;
    }
    return super.getLocalizedName();
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public GrammarBindingMode getGrammarBindingMode() {
    return grammarMode;
  }

  @Override
  public ResourceLocation getGrammarToken() {
    return grammarToken;
  }

  @Override
  public Integer getGrammarRank() {
    return grammarRank;
  }

  @Override
  public SymbolDisplay getDisplay() {
    return display;
  }
}
