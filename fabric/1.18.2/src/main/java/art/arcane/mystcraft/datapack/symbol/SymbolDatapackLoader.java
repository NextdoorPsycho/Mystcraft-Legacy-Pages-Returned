package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.datapack.grammar.GrammarDatapackLoader;
import art.arcane.mystcraft.grammar.CFGGrammarGenerator;
import art.arcane.mystcraft.grammar.GrammarRules;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Map;

/**
 * Applies datapack symbol definitions to the live registry.
 * 1.18.2 version - uses Registry.FLUID instead of BuiltInRegistries.
 */
public final class SymbolDatapackLoader {

  private SymbolDatapackLoader() {
  }

  public static void apply(Map<ResourceLocation, JsonElement> elements,
                           Map<ResourceLocation, JsonElement> grammarRules) {
    applySymbolBlacklist();
    SymbolRegistry.resetToStatic();
    CFGGrammarGenerator.reset();
    GrammarRules.registerBaseRules();
    GrammarDatapackLoader.applyRules(grammarRules);

    registerFluidSeaSymbols();

    int applied = 0;
    for (Map.Entry<ResourceLocation, JsonElement> entry : elements.entrySet()) {
      if (!entry.getValue().isJsonObject()) continue;
      ResourceLocation id = entry.getKey();
      JsonObject json = entry.getValue().getAsJsonObject();
      SymbolDefinition definition = SymbolDefinition.fromJson(id, json);
      if (definition == null) {
        continue;
      }
      DataSymbol symbol = new DataSymbol(
          definition.id,
          definition.category,
          definition.cardRank,
          definition.instabilityCost,
          definition.poem,
          definition.allowRandom,
          definition.canDuplicate,
          definition.grammarMode,
          definition.grammarToken,
          definition.grammarRank,
          definition.logic,
          definition.displayName
      );
      boolean registered = SymbolRegistry.register(symbol, definition.replace);
      if (registered) {
        applied++;
      }
    }

    CFGGrammarGenerator.buildGrammar();
    SymbolRegistry.freeze();

    Mystcraft.LOGGER.info("[Datapack] Applied {} symbol definitions", applied);
  }

  private static void applySymbolBlacklist() {
    SymbolRegistry.clearBlacklist();
    List<String> disabled = MystcraftConfig.disabledSymbols.get();
    if (disabled == null || disabled.isEmpty()) {
      return;
    }
    for (String raw : disabled) {
      if (raw == null || raw.isBlank()) {
        continue;
      }
      if (!ResourceLocation.isValidResourceLocation(raw)) {
        Mystcraft.LOGGER.warn("[Datapack] Invalid symbol id in disabledSymbols: {}", raw);
        continue;
      }
      SymbolRegistry.blacklist(new ResourceLocation(raw));
    }
  }

  private static void registerFluidSeaSymbols() {
    int count = 0;
    for (Fluid fluid : Registry.FLUID) {
      if (fluid == Fluids.EMPTY) continue;
      FluidState state = fluid.defaultFluidState();
      if (!state.isSource()) continue;
      ResourceLocation fluidId = Registry.FLUID.getKey(fluid);
      if (fluidId == null) continue;

      boolean isWater = fluid == Fluids.WATER;
      boolean isLava = fluid == Fluids.LAVA;

      int cardRank = isWater ? 1 : (isLava ? 4 : 3);
      float instability = isWater ? 0.0f : (isLava ? 25.0f : 15.0f);
      String[] poem = isWater
          ? new String[]{"Terrain", "Water", "Flow", "Sea"}
          : (isLava ? new String[]{"Terrain", "Fire", "Flow", "Chaos"}
          : new String[]{"Terrain", "Liquid", "Flow", "Strange"});

      String symbolPath = "sea_" + fluidId.getNamespace() + "_" + fluidId.getPath();
      ResourceLocation symbolId = SymbolRegistry.mystcraftId(symbolPath);
      String displayName = formatDisplayName(fluidId.getPath()) + " Sea";

      SymbolLogic logic = (director, seed) -> {
        director.setSeaBlock(state.createLegacyBlock());
        if (instability != 0.0f) {
          director.addInstability(instability);
        }
      };

      DataSymbol symbol = new DataSymbol(
          symbolId,
          art.arcane.mystcraft.api.symbol.SymbolCategory.SEA,
          cardRank,
          instability,
          poem,
          true,
          false,
          art.arcane.mystcraft.api.symbol.GrammarBindingMode.DEFAULT,
          null,
          null,
          List.of(logic),
          displayName
      );
      if (SymbolRegistry.register(symbol)) {
        count++;
      }
    }
    Mystcraft.LOGGER.info("[Datapack] Registered {} fluid sea symbols", count);
  }

  private static String formatDisplayName(String path) {
    String[] parts = path.split("_");
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) builder.append(' ');
      String part = parts[i];
      if (!part.isEmpty()) {
        builder.append(Character.toUpperCase(part.charAt(0)));
        if (part.length() > 1) {
          builder.append(part.substring(1));
        }
      }
    }
    return builder.toString();
  }
}
