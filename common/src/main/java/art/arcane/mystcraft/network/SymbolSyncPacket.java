package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.GrammarBindingMode;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.datapack.symbol.DataSymbol;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet for syncing symbol data from server to client. Sends a list of all
 * registered symbols with their basic metadata. Used when a player joins to
 * ensure client has correct symbol information.
 */
public record SymbolSyncPacket(List<SymbolData> symbols) {

  /**
   * Creates a sync packet from the current symbol registry.
   */
  public SymbolSyncPacket() {
    this(new ArrayList<>());
    for (IAgeSymbol symbol : SymbolRegistry.getAll()) {
      String displayName = null;
      if (symbol instanceof DataSymbol dataSymbol) {
        displayName = dataSymbol.getDisplayName();
      }
      symbols.add(new SymbolData(
          symbol.getRegistryName(),
          symbol.getCategory(),
          symbol.getCardRank() != null ? symbol.getCardRank() : 0,
          symbol.getInstabilityCost(),
          symbol.getPoem(),
          displayName,
          symbol.allowInRandomGeneration(),
          symbol.canDuplicate(),
          !SymbolRegistry.isStaticSymbol(symbol.getRegistryName()),
          SymbolRegistry.isOverridden(symbol.getRegistryName())
      ));
    }
  }

  /**
   * Creates a sync packet from a list of symbol data.
   */
  public SymbolSyncPacket {
  }

  /**
   * Decodes a packet from a buffer.
   */
  public static SymbolSyncPacket decode(FriendlyByteBuf buf) {
    int count = buf.readInt();
    List<SymbolData> symbols = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      ResourceLocation id = buf.readResourceLocation();
      SymbolCategory category = buf.readEnum(SymbolCategory.class);
      int cardRank = buf.readInt();
      float instabilityCost = buf.readFloat();
      boolean allowRandom = buf.readBoolean();
      boolean canDuplicate = buf.readBoolean();
      boolean isDatapack = buf.readBoolean();
      boolean isOverride = buf.readBoolean();
      String displayName = null;
      if (buf.readBoolean()) {
        displayName = buf.readUtf();
      }
      int poemLength = buf.readInt();
      String[] poem = null;
      if (poemLength >= 0) {
        poem = new String[poemLength];
        for (int j = 0; j < poemLength; j++) {
          poem[j] = buf.readUtf();
        }
      }
      symbols.add(new SymbolData(id, category, cardRank, instabilityCost, poem,
          displayName, allowRandom, canDuplicate, isDatapack, isOverride));
    }
    return new SymbolSyncPacket(symbols);
  }

  /**
   * Encodes a packet to a buffer.
   */
  public static void encode(SymbolSyncPacket packet, FriendlyByteBuf buf) {
    buf.writeInt(packet.symbols.size());
    for (SymbolData data : packet.symbols) {
      buf.writeResourceLocation(data.id);
      buf.writeEnum(data.category);
      buf.writeInt(data.cardRank);
      buf.writeFloat(data.instabilityCost);
      buf.writeBoolean(data.allowRandom);
      buf.writeBoolean(data.canDuplicate);
      buf.writeBoolean(data.isDatapack);
      buf.writeBoolean(data.isOverride);
      if (data.displayName == null) {
        buf.writeBoolean(false);
      } else {
        buf.writeBoolean(true);
        buf.writeUtf(data.displayName);
      }
      if (data.poem == null) {
        buf.writeInt(-1);
      } else {
        buf.writeInt(data.poem.length);
        for (String word : data.poem) {
          buf.writeUtf(word);
        }
      }
    }
  }

  /**
   * Handles a packet on the client.
   */
  public static void handle(SymbolSyncPacket packet, PacketContext ctx) {
    ctx.enqueueWork(() -> {

      Mystcraft.LOGGER.info("Received symbol sync packet with {} symbols", packet.symbols.size());

      for (SymbolData data : packet.symbols) {
        boolean exists = SymbolRegistry.contains(data.id);
        boolean shouldRegister = data.isOverride || !exists;
        if (shouldRegister) {
          DataSymbol symbol = new DataSymbol(
              data.id,
              data.category,
              data.cardRank,
              data.instabilityCost,
              data.poem,
              data.allowRandom,
              data.canDuplicate,
              GrammarBindingMode.DISABLED,
              null,
              null,
              List.of(),
              data.displayName
          );
          SymbolRegistry.registerSynced(symbol, data.isOverride || !exists);
        } else if (!exists) {
          Mystcraft.LOGGER.warn("Server has symbol {} that client doesn't know about", data.id);
        }
      }
    });
  }

  /**
   * Gets the list of symbol data.
   */
  @Override
  public List<SymbolData> symbols() {
    return symbols;
  }

  /**
   * Simple data class for symbol information.
   */
  public record SymbolData(ResourceLocation id, SymbolCategory category,
                           int cardRank, float instabilityCost, String[] poem,
                           String displayName, boolean allowRandom,
                           boolean canDuplicate, boolean isDatapack,
                           boolean isOverride) {
  }
}
