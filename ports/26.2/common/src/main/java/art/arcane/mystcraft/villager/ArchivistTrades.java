package art.arcane.mystcraft.villager;

import art.arcane.mystcraft.MystcraftConstants;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.trading.TradeSet;

/**
 * Data-driven trade-set keys used by the 26.2 Archivist profession.
 *
 * <p>The profession itself remains loader-registered because its workstation
 * POI is loader-owned. Both loaders must pass {@link #TRADE_SETS_BY_LEVEL} to
 * the vanilla {@code VillagerProfession} constructor.</p>
 */
public final class ArchivistTrades {
  public static final ResourceKey<TradeSet> LEVEL_1 = key("archivist/level_1");
  public static final ResourceKey<TradeSet> LEVEL_2 = key("archivist/level_2");
  public static final ResourceKey<TradeSet> LEVEL_3 = key("archivist/level_3");
  public static final ResourceKey<TradeSet> LEVEL_4 = key("archivist/level_4");
  public static final ResourceKey<TradeSet> LEVEL_5 = key("archivist/level_5");

  public static final List<ResourceKey<TradeSet>> ALL_LEVELS = List.of(
      LEVEL_1,
      LEVEL_2,
      LEVEL_3,
      LEVEL_4,
      LEVEL_5
  );

  public static final Int2ObjectMap<ResourceKey<TradeSet>> TRADE_SETS_BY_LEVEL =
      Int2ObjectMap.ofEntries(
          Int2ObjectMap.entry(1, LEVEL_1),
          Int2ObjectMap.entry(2, LEVEL_2),
          Int2ObjectMap.entry(3, LEVEL_3),
          Int2ObjectMap.entry(4, LEVEL_4),
          Int2ObjectMap.entry(5, LEVEL_5)
      );

  private static final int[] EXPECTED_TRADE_COUNTS = {0, 4, 4, 4, 3, 5};

  private ArchivistTrades() {
  }

  public static ResourceKey<TradeSet> forLevel(int level) {
    ResourceKey<TradeSet> key = TRADE_SETS_BY_LEVEL.get(level);
    if (key == null) {
      throw new IllegalArgumentException("Archivist level must be between 1 and 5: " + level);
    }
    return key;
  }

  public static int expectedTradeCount(int level) {
    if (level < 1 || level >= EXPECTED_TRADE_COUNTS.length) {
      throw new IllegalArgumentException("Archivist level must be between 1 and 5: " + level);
    }
    return EXPECTED_TRADE_COUNTS[level];
  }

  private static ResourceKey<TradeSet> key(String path) {
    return ResourceKey.create(Registries.TRADE_SET, MystcraftConstants.loc(path));
  }
}
