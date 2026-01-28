package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import com.google.gson.JsonObject;

/**
 * Factory for creating terrain alterations from datapack JSON.
 */
public interface TerrainAlterationFactory {
    ITerrainAlteration create(long seed, JsonObject json);
}
