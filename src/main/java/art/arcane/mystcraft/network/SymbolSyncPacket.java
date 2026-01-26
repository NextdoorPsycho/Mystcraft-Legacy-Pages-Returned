package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

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
                    symbol.getInstabilityCost()
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
            symbols.add(new SymbolData(id, category, cardRank, instabilityCost));
        }
        return new SymbolSyncPacket(symbols);
    }

    /**
     * Handles this packet on the client.
     */
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Log received symbols for debugging
            Mystcraft.LOGGER.info("Received symbol sync packet with {} symbols", symbols.size());

            // In a full implementation, this would update a client-side symbol cache
            // For now, symbols are registered statically at mod load time
            // This packet is primarily for future extensibility (datapacks, custom symbols)

            for (SymbolData data : symbols) {
                // Verify the symbol exists on the client
                if (!SymbolRegistry.contains(data.id)) {
                    Mystcraft.LOGGER.warn("Server has symbol {} that client doesn't know about", data.id);
                }
            }
        });
        ctx.setPacketHandled(true);
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

        public SymbolData(ResourceLocation id, SymbolCategory category, int cardRank, float instabilityCost) {
            this.id = id;
            this.category = category;
            this.cardRank = cardRank;
            this.instabilityCost = instabilityCost;
        }
    }
}
