package art.arcane.mystcraft.network;

import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.item.PortfolioItem;
import art.arcane.mystcraft.menu.*;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet for custom container actions that can't be handled by vanilla slot
 * clicks. Used for things like clicking on the Ink Mixer basin to add items.
 */
public class ContainerActionPacket implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<ContainerActionPacket> TYPE =
      MystcraftNetwork.type("container_action");
  public static final StreamCodec<RegistryFriendlyByteBuf, ContainerActionPacket> STREAM_CODEC =
      CustomPacketPayload.codec(ContainerActionPacket::encode, ContainerActionPacket::decode);

  private static final int MAX_STRING_DATA_LENGTH = 256;
  private static final int MAX_BOOK_TITLE_LENGTH = 21;

  private final Action action;
  private final int containerId;
  private final boolean rightClick;
  private final String stringData;
  private final int intData;

  public ContainerActionPacket(Action action, int containerId, boolean rightClick, String stringData, int intData) {
    this.action = action;
    this.containerId = containerId;
    this.rightClick = rightClick;
    this.stringData = stringData != null ? stringData : "";
    this.intData = intData;
  }

  public ContainerActionPacket(Action action, int containerId, boolean rightClick, String stringData) {
    this(action, containerId, rightClick, stringData, 0);
  }

  public ContainerActionPacket(Action action, int containerId, boolean rightClick) {
    this(action, containerId, rightClick, "", 0);
  }

  public ContainerActionPacket(Action action, int containerId, int intData) {
    this(action, containerId, false, "", intData);
  }

  public static void encode(ContainerActionPacket packet, FriendlyByteBuf buf) {
    buf.writeEnum(packet.action);
    buf.writeVarInt(packet.containerId);
    buf.writeBoolean(packet.rightClick);
    buf.writeUtf(packet.stringData, MAX_STRING_DATA_LENGTH);
    buf.writeVarInt(packet.intData);
  }

  public static ContainerActionPacket decode(FriendlyByteBuf buf) {
    Action action = buf.readEnum(Action.class);
    int containerId = buf.readVarInt();
    boolean rightClick = buf.readBoolean();
    String stringData = buf.readUtf(MAX_STRING_DATA_LENGTH);
    int intData = buf.readVarInt();
    return new ContainerActionPacket(action, containerId, rightClick, stringData, intData);
  }

  @Override
  public CustomPacketPayload.Type<ContainerActionPacket> type() {
    return TYPE;
  }

  public static void handle(ContainerActionPacket packet, PacketContext ctx) {
    ctx.enqueueWork(() -> {
      ServerPlayer player = ctx.getServerPlayer();
      if (player == null) return;

      if (player.containerMenu.containerId != packet.containerId) {
        return;
      }

      switch (packet.action) {
        case INK_MIXER_ADD_ITEM ->
            handleInkMixerAddItem(player, packet.rightClick);
        case BOOK_BINDER_SET_TITLE ->
            handleBookBinderSetTitle(player, packet.stringData);
        case BOOK_BINDER_INSERT_PAGE ->
            handleBookBinderInsertPage(player, packet.intData, packet.rightClick);
        case BOOK_BINDER_REMOVE_PAGE ->
            handleBookBinderRemovePage(player, packet.intData);
        case WRITING_DESK_SET_ACTIVE_TAB ->
            handleWritingDeskSetActiveTab(player, packet.intData);
        case WRITING_DESK_ADD_TO_SURFACE ->
            handleWritingDeskAddToSurface(player, packet.intData, packet.rightClick);
        case WRITING_DESK_REMOVE_FROM_SURFACE ->
            handleWritingDeskRemoveFromSurface(player, packet.intData, packet.rightClick);
        case WRITING_DESK_WRITE_SYMBOL ->
            handleWritingDeskWriteSymbol(player, packet.stringData);
        case WRITING_DESK_SET_TITLE ->
            handleWritingDeskSetTitle(player, packet.stringData);
        case WRITING_DESK_ADD_TO_BOOK ->
            handleWritingDeskAddToBook(player, packet.intData, packet.rightClick);
        case WRITING_DESK_REMOVE_FROM_BOOK ->
            handleWritingDeskRemoveFromBook(player, packet.intData);
        case LINK_MODIFIER_SET_FLAG ->
            handleLinkModifierSetFlag(player, packet.stringData, packet.rightClick);
        case LINK_MODIFIER_SET_TITLE ->
            handleLinkModifierSetTitle(player, packet.stringData);
        case LINK_MODIFIER_SET_SEED ->
            handleLinkModifierSetSeed(player, packet.stringData);
        case LINK_MODIFIER_APPLY -> handleLinkModifierApply(player);
        case LINK_MODIFIER_RECYCLE -> handleLinkModifierRecycle(player);
        case FOLDER_ADD_PAGE ->
            handleFolderAddPage(player, packet.intData, packet.rightClick);
        case FOLDER_REMOVE_PAGE ->
            handleFolderRemovePage(player, packet.intData);
        case PORTFOLIO_SORT -> handlePortfolioSort(player);
      }
    });
  }

  private static void handleInkMixerAddItem(ServerPlayer player, boolean singleItem) {
    if (!(player.containerMenu instanceof InkMixerMenu menu)) {
      return;
    }

    ItemStack carried = player.containerMenu.getCarried();
    if (carried.isEmpty()) {
      return;
    }

    InkMixerBlockEntity blockEntity = menu.getBlockEntity();
    if (!blockEntity.canAddItem(carried)) {
      return;
    }

    int amount = singleItem ? 1 : carried.getCount();
    ItemStack remaining = blockEntity.addItems(carried, amount);

    player.containerMenu.setCarried(remaining);
    player.containerMenu.broadcastChanges();
  }

  private static void handleBookBinderSetTitle(ServerPlayer player, String title) {
    if (!(player.containerMenu instanceof BookBinderMenu menu)) {
      return;
    }

    BookBinderBlockEntity blockEntity = menu.getBlockEntity();
    blockEntity.setBookTitle(truncateBookTitle(title));
    player.containerMenu.broadcastChanges();
  }

  private static void handleBookBinderInsertPage(ServerPlayer player, int index, boolean singleItem) {
    if (!(player.containerMenu instanceof BookBinderMenu menu)) {
      return;
    }

    ItemStack carried = player.containerMenu.getCarried();
    if (carried.isEmpty() || !(carried.getItem() instanceof PageItem)) {
      return;
    }

    BookBinderBlockEntity blockEntity = menu.getBlockEntity();

    if (singleItem) {

      ItemStack single = carried.split(1);
      ItemStack remainder = blockEntity.insertPage(single, index);
      if (!remainder.isEmpty()) {
        carried.grow(remainder.getCount());
      }
    } else {

      ItemStack remainder = blockEntity.insertPage(carried.copy(), index);
      carried.setCount(remainder.getCount());
    }

    player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
    player.containerMenu.broadcastChanges();
  }

  private static void handleBookBinderRemovePage(ServerPlayer player, int index) {
    if (!(player.containerMenu instanceof BookBinderMenu menu)) {
      return;
    }

    if (!player.containerMenu.getCarried().isEmpty()) {
      return;
    }

    BookBinderBlockEntity blockEntity = menu.getBlockEntity();
    ItemStack removed = blockEntity.removePage(index);

    if (!removed.isEmpty()) {
      player.containerMenu.setCarried(removed);
      player.containerMenu.broadcastChanges();
    }
  }

  private static void handleLinkModifierSetFlag(ServerPlayer player, String flagId, boolean value) {
    if (!(player.containerMenu instanceof LinkModifierMenu menu)) {
      return;
    }

    LinkModifierBlockEntity blockEntity = menu.getBlockEntity();
    blockEntity.setLinkFlag(flagId, value);
    player.containerMenu.broadcastChanges();
  }

  private static void handleLinkModifierSetTitle(ServerPlayer player, String title) {
    if (!(player.containerMenu instanceof LinkModifierMenu menu)) {
      return;
    }

    LinkModifierBlockEntity blockEntity = menu.getBlockEntity();
    blockEntity.setBookTitle(player, truncateBookTitle(title));
    player.containerMenu.broadcastChanges();
  }

  private static void handleLinkModifierSetSeed(ServerPlayer player, String seed) {
    if (!(player.containerMenu instanceof LinkModifierMenu menu)) {
      return;
    }

    LinkModifierBlockEntity blockEntity = menu.getBlockEntity();
    blockEntity.setItemSeed(player, seed);
    player.containerMenu.broadcastChanges();
  }

  private static void handleLinkModifierApply(ServerPlayer player) {
    if (!(player.containerMenu instanceof LinkModifierMenu menu)) {
      return;
    }

    menu.getBlockEntity().performModification();
    player.containerMenu.broadcastChanges();
  }

  private static void handleLinkModifierRecycle(ServerPlayer player) {
    if (!(player.containerMenu instanceof LinkModifierMenu menu)) {
      return;
    }

    LinkModifierBlockEntity blockEntity = menu.getBlockEntity();
    blockEntity.recycleDimension();
    player.containerMenu.broadcastChanges();
  }

  private static void handleWritingDeskAddToBook(ServerPlayer player, int index, boolean singleItem) {
    if (!(player.containerMenu instanceof WritingDeskMenu menu)) {
      return;
    }

    ItemStack carried = player.containerMenu.getCarried();
    if (carried.isEmpty() || !(carried.getItem() instanceof PageItem)) {
      return;
    }

    WritingDeskBlockEntity blockEntity = menu.getBlockEntity();
    ItemStack writingItem = blockEntity.getMainInventory().getItem(WritingDeskBlockEntity.SLOT_WRITING);

    if (!(writingItem.getItem() instanceof AgebookItem agebook)) {
      return;
    }

    List<ItemStack> pages = new ArrayList<>(agebook.getPageList(writingItem));

    int maxSymbols = art.arcane.mystcraft.config.MystcraftConfig.maxSymbolsPerBook.get();

    if (singleItem) {

      if (maxSymbols >= 0 && pages.size() >= maxSymbols) {
        return;
      }
      ItemStack single = carried.split(1);
      pages.add(clampInsertionIndex(index, pages.size()), single);
    } else {

      int insertIndex = clampInsertionIndex(index, pages.size());
      while (!carried.isEmpty()) {
        if (maxSymbols >= 0 && pages.size() >= maxSymbols) {
          break;
        }
        ItemStack single = carried.split(1);
        pages.add(insertIndex++, single);
      }
    }

    agebook.setPageList(writingItem, pages);
    blockEntity.setChanged();

    player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
    player.containerMenu.broadcastChanges();
  }

  private static void handleWritingDeskRemoveFromBook(ServerPlayer player, int index) {
    if (!(player.containerMenu instanceof WritingDeskMenu menu)) {
      return;
    }

    if (!player.containerMenu.getCarried().isEmpty()) {
      return;
    }

    WritingDeskBlockEntity blockEntity = menu.getBlockEntity();
    ItemStack writingItem = blockEntity.getMainInventory().getItem(WritingDeskBlockEntity.SLOT_WRITING);

    if (!(writingItem.getItem() instanceof AgebookItem agebook)) {
      return;
    }

    List<ItemStack> pages = new ArrayList<>(agebook.getPageList(writingItem));

    if (index < 0 || index >= pages.size()) {
      return;
    }

    ItemStack removed = pages.remove(index);

    agebook.setPageList(writingItem, pages);
    blockEntity.setChanged();

    if (!removed.isEmpty()) {
      player.containerMenu.setCarried(removed);
      player.containerMenu.broadcastChanges();
    }
  }

  private static void handleWritingDeskSetActiveTab(ServerPlayer player, int tabIndex) {
    if (!(player.containerMenu instanceof WritingDeskMenu menu)) {
      return;
    }

    player.containerMenu.broadcastChanges();
  }

  private static void handleWritingDeskAddToSurface(ServerPlayer player, int encodedData, boolean singleItem) {
    if (!(player.containerMenu instanceof WritingDeskMenu menu)) {
      return;
    }

    ItemStack carried = player.containerMenu.getCarried();
    if (carried.isEmpty() || !(carried.getItem() instanceof PageItem)) {
      return;
    }

    int tabIndex = (encodedData >> 16) & 0xFFFF;
    WritingDeskBlockEntity blockEntity = menu.getBlockEntity();

    if (singleItem) {
      ItemStack single = carried.split(1);
      ItemStack remainder = blockEntity.addPageToTab(player, tabIndex, single);
      if (!remainder.isEmpty()) {
        carried.grow(remainder.getCount());
      }
    } else {
      while (!carried.isEmpty()) {
        ItemStack single = carried.split(1);
        ItemStack remainder = blockEntity.addPageToTab(player, tabIndex, single);
        if (!remainder.isEmpty()) {
          carried.grow(remainder.getCount());
          break;
        }
      }
    }

    player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
    player.containerMenu.broadcastChanges();
  }

  private static void handleWritingDeskRemoveFromSurface(ServerPlayer player, int encodedData, boolean unused) {
    if (!(player.containerMenu instanceof WritingDeskMenu menu)) {
      return;
    }

    if (!player.containerMenu.getCarried().isEmpty()) {
      return;
    }

    int tabIndex = (encodedData >> 16) & 0xFFFF;
    int pageIndex = encodedData & 0xFFFF;

    WritingDeskBlockEntity blockEntity = menu.getBlockEntity();
    ItemStack removed = blockEntity.removePageFromTab(player, tabIndex, pageIndex);

    if (!removed.isEmpty()) {
      player.containerMenu.setCarried(removed);
      player.containerMenu.broadcastChanges();
    }
  }

  private static void handleWritingDeskWriteSymbol(ServerPlayer player, String symbolId) {
    if (!(player.containerMenu instanceof WritingDeskMenu menu)) {
      return;
    }

    if (symbolId == null || symbolId.isEmpty()) {
      return;
    }

    Identifier symbol = Identifier.tryParse(symbolId);
    if (symbol == null || art.arcane.mystcraft.symbol.SymbolRegistry.get(symbol) == null) {
      return;
    }

    WritingDeskBlockEntity blockEntity = menu.getBlockEntity();
    blockEntity.writeSymbol(player, symbol);
    player.containerMenu.broadcastChanges();
  }

  private static void handleWritingDeskSetTitle(ServerPlayer player, String title) {
    if (!(player.containerMenu instanceof WritingDeskMenu menu)) {
      return;
    }

    WritingDeskBlockEntity blockEntity = menu.getBlockEntity();
    ItemStack writingItem = blockEntity.getMainInventory().getItem(WritingDeskBlockEntity.SLOT_WRITING);

    if (writingItem.isEmpty()) {
      return;
    }

    if (writingItem.getItem() instanceof AgebookItem) {
      CompoundTag tag = ItemStackNbt.getOrCreateTag(writingItem);
      LinkOptions.setDisplayName(tag, truncateBookTitle(title));
      ItemStackNbt.setTag(writingItem, tag);
      blockEntity.setChanged();
    } else if (writingItem.getItem() instanceof LinkbookItem linkbook) {
      linkbook.setDisplayName(writingItem, truncateBookTitle(title));
      blockEntity.setChanged();
    }

    player.containerMenu.broadcastChanges();
  }

  private static void handleFolderAddPage(ServerPlayer player, int index, boolean singleItem) {
    if (!(player.containerMenu instanceof FolderMenu menu)) {
      return;
    }

    ItemStack carried = player.containerMenu.getCarried();
    if (carried.isEmpty() || !(carried.getItem() instanceof PageItem)) {
      return;
    }

    ItemStack folder = menu.getFolderStack();
    if (folder.isEmpty()) {
      return;
    }

    List<ItemStack> pages = new ArrayList<>(FolderItem.getPages(folder));

    if (singleItem) {
      ItemStack single = carried.split(1);
      if (pages.size() < FolderItem.MAX_PAGES) {
        pages.add(clampInsertionIndex(index, pages.size()), single);
      } else {
        carried.grow(1);
      }
    } else {
      int insertIndex = clampInsertionIndex(index, pages.size());
      while (!carried.isEmpty() && pages.size() < FolderItem.MAX_PAGES) {
        ItemStack single = carried.split(1);
        pages.add(insertIndex++, single);
      }
    }

    FolderItem.setPages(folder, pages);
    player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
    player.containerMenu.broadcastChanges();
  }

  private static void handleFolderRemovePage(ServerPlayer player, int index) {
    if (!(player.containerMenu instanceof FolderMenu menu)) {
      return;
    }

    if (!player.containerMenu.getCarried().isEmpty()) {
      return;
    }

    ItemStack folder = menu.getFolderStack();
    if (folder.isEmpty()) {
      return;
    }

    List<ItemStack> pages = new ArrayList<>(FolderItem.getPages(folder));

    if (index < 0 || index >= pages.size()) {
      return;
    }

    ItemStack removed = pages.remove(index);
    FolderItem.setPages(folder, pages);

    if (!removed.isEmpty()) {
      player.containerMenu.setCarried(removed);
      player.containerMenu.broadcastChanges();
    }
  }

  private static void handlePortfolioSort(ServerPlayer player) {
    if (!(player.containerMenu instanceof PortfolioMenu menu)) {
      return;
    }

    PortfolioItem.sortPages(menu.getPortfolioStack());

    menu.reloadFromPortfolio();

    player.containerMenu.broadcastChanges();
  }

  private static String truncateBookTitle(String title) {
    return title.length() <= MAX_BOOK_TITLE_LENGTH
        ? title
        : title.substring(0, MAX_BOOK_TITLE_LENGTH);
  }

  private static int clampInsertionIndex(int index, int size) {
    return Math.max(0, Math.min(index, size));
  }

  public enum Action {
    INK_MIXER_ADD_ITEM,
    BOOK_BINDER_SET_TITLE,
    BOOK_BINDER_INSERT_PAGE,
    BOOK_BINDER_REMOVE_PAGE,
    WRITING_DESK_SET_ACTIVE_TAB,
    WRITING_DESK_ADD_TO_SURFACE,
    WRITING_DESK_REMOVE_FROM_SURFACE,
    WRITING_DESK_WRITE_SYMBOL,
    WRITING_DESK_SET_TITLE,
    WRITING_DESK_ADD_TO_BOOK,
    WRITING_DESK_REMOVE_FROM_BOOK,
    LINK_MODIFIER_SET_FLAG,
    LINK_MODIFIER_SET_TITLE,
    LINK_MODIFIER_SET_SEED,
    LINK_MODIFIER_RECYCLE,
    FOLDER_ADD_PAGE,
    FOLDER_REMOVE_PAGE,
    PORTFOLIO_SORT,
    LINK_MODIFIER_APPLY
  }
}
