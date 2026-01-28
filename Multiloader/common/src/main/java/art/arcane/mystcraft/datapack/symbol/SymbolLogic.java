package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.api.world.AgeDirector;

/**
 * Executes a single datapack-defined symbol behavior.
 */
public interface SymbolLogic {
    void apply(AgeDirector director, long seed);
}
