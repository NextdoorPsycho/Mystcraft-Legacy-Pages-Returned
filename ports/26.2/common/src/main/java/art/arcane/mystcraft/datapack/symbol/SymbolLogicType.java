package art.arcane.mystcraft.datapack.symbol;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

/**
 * Parses a JSON object into a SymbolLogic instance.
 */
public interface SymbolLogicType {
  Identifier id();

  SymbolLogic parse(JsonObject json);
}
