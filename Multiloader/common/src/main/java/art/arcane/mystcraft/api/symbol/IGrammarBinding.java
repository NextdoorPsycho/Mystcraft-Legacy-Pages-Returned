package art.arcane.mystcraft.api.symbol;

import net.minecraft.resources.ResourceLocation;

/**
 * Optional interface for symbols that want to control grammar rule registration.
 */
public interface IGrammarBinding {

    /**
     * Returns how this symbol should bind into the grammar system.
     */
    GrammarBindingMode getGrammarBindingMode();

    /**
     * Returns the grammar token to use when {@link #getGrammarBindingMode()} is CUSTOM.
     */
    ResourceLocation getGrammarToken();

    /**
     * Returns the grammar rank to use when {@link #getGrammarBindingMode()} is CUSTOM.
     * Null falls back to the symbol's card rank.
     */
    Integer getGrammarRank();
}
