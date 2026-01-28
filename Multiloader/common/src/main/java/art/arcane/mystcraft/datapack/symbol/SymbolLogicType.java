package art.arcane.mystcraft.datapack.symbol;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * Parses a JSON object into a SymbolLogic instance.
 */
public interface SymbolLogicType {
    ResourceLocation getId();
    SymbolLogic parse(JsonObject json);
}
