package art.arcane.mystcraft.api.symbol;

/**
 * Controls how a symbol binds into the CFG grammar system.
 */
public enum GrammarBindingMode {
    /** Use the default category -> grammar token mapping. */
    DEFAULT,
    /** Use a custom grammar token provided by the symbol. */
    CUSTOM,
    /** Do not register this symbol with the grammar system. */
    DISABLED
}
