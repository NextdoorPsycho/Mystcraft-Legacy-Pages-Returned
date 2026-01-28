package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;

/**
 * Factory for creating populators from datapack JSON.
 */
public interface PopulatorFactory {
    IPopulate create(long seed, JsonObject json);
}
