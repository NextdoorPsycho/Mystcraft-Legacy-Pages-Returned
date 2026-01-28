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
 * Packet for syncing symbol data from server to client.
 * Sends a list of all registered symbols with their basic metadata.
 * Used when a player joins to ensure client has correct symbol information.
 */
public class SymbolSyncPacket {

    private final List<SymbolData> symbols;

    /**
     * Creates a sync packet from the current symbol registry.
     */
    public SymbolSyncPacket() {
        this.symbols = new ArrayList<>();
        for (IAgeSymbol symbol : SymbolRegistry.getAll()) {
            symbols.add(new SymbolData(
                    symbol.getRegistryName(),
                    symbol.getCategory(),
                    symbol.getCardRank() != null ? symbol.getCardRank() : 0,
                    symbol.getInstabilityCost(),
                    symbol.getPoem(),
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
    public SymbolSyncPacket(List<SymbolData> symbols) {
        this.symbols = symbols;
    }

    /**
     * Encodes this packet to a buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(symbols.size());
        for (SymbolData data : symbols) {
            buf.writeResourceLocation(data.id);
            buf.writeEnum(data.category);
            buf.writeInt(data.cardRank);
            buf.writeFloat(data.instabilityCost);
            buf.writeBoolean(data.allowRandom);
            buf.writeBoolean(data.canDuplicate);
            buf.writeBoolean(data.isDatapack);
            buf.writeBoolean(data.isOverride);
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
            int poemLength = buf.readInt();
            String[] poem = null;
            if (poemLength >= 0) {
                poem = new String[poemLength];
                for (int j = 0; j < poemLength; j++) {
                    poem[j] = buf.readUtf();
                }
            }
            symbols.add(new SymbolData(id, category, cardRank, instabilityCost, poem,
                    allowRandom, canDuplicate, isDatapack, isOverride));
        }
        return new SymbolSyncPacket(symbols);
    }

    /**
     * Handles this packet on the client.
     */
    public void handle(PacketContext ctx) {
        ctx.enqueueWork(() -> {
            // Log received symbols for debugging
            Mystcraft.LOGGER.info("Received symbol sync packet with {} symbols", symbols.size());

            // In a full implementation, this would update a client-side symbol cache
            // For now, symbols are registered statically at mod load time
            // This packet is primarily for future extensibility (datapacks, custom symbols)

            for (SymbolData data : symbols) {
                boolean exists = SymbolRegistry.contains(data.id);
                boolean shouldRegister = data.isDatapack || data.isOverride || !exists;
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
                            List.of()
                    );
                    SymbolRegistry.register(symbol, data.isOverride || !exists);
                } else if (!exists) {
                    Mystcraft.LOGGER.warn("Server has symbol {} that client doesn't know about", data.id);
                }
            }
        });
    }

    /**
     * Gets the list of symbol data.
     */
    public List<SymbolData> getSymbols() {
        return symbols;
    }

    /**
     * Simple data class for symbol information.
     */
    public static class SymbolData {
        public final ResourceLocation id;
        public final SymbolCategory category;
        public final int cardRank;
        public final float instabilityCost;
        public final String[] poem;
        public final boolean allowRandom;
        public final boolean canDuplicate;
        public final boolean isDatapack;
        public final boolean isOverride;

        public SymbolData(ResourceLocation id, SymbolCategory category, int cardRank, float instabilityCost,
                          String[] poem, boolean allowRandom, boolean canDuplicate,
                          boolean isDatapack, boolean isOverride) {
            this.id = id;
            this.category = category;
            this.cardRank = cardRank;
            this.instabilityCost = instabilityCost;
            this.poem = poem;
            this.allowRandom = allowRandom;
            this.canDuplicate = canDuplicate;
            this.isDatapack = isDatapack;
            this.isOverride = isOverride;
        }
    }
}
