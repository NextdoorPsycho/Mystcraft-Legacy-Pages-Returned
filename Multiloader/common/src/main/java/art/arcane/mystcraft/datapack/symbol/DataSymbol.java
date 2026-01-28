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
    private final GrammarBindingMode grammarMode;
    private final ResourceLocation grammarToken;
    private final Integer grammarRank;
    private final List<SymbolLogic> logic;

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
                      List<SymbolLogic> logic) {
        super(id, category);
        this.allowRandom = allowRandom;
        this.grammarMode = grammarMode;
        this.grammarToken = grammarToken;
        this.grammarRank = grammarRank;
        this.logic = logic;
        if (cardRank != null) {
            setCardRank(cardRank);
        }
        setInstabilityCost(instabilityCost);
        if (poem != null) {
            setPoem(poem);
        }
        setDuplicatable(canDuplicate);
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
}
