package art.arcane.mystcraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet for custom container actions that can't be handled by vanilla slot clicks.
 * <p>
 * 1.18.2 stub version - simplified packet handling without dependencies on excluded classes.
 */
public class ContainerActionPacket {

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

  public Action action() {
    return action;
  }

  public int containerId() {
    return containerId;
  }

  public boolean rightClick() {
    return rightClick;
  }

  public String stringData() {
    return stringData;
  }

  public int intData() {
    return intData;
  }

  public static void encode(ContainerActionPacket packet, FriendlyByteBuf buf) {
    buf.writeEnum(packet.action);
    buf.writeVarInt(packet.containerId);
    buf.writeBoolean(packet.rightClick);
    buf.writeUtf(packet.stringData);
    buf.writeVarInt(packet.intData);
  }

  public static ContainerActionPacket decode(FriendlyByteBuf buf) {
    Action action = buf.readEnum(Action.class);
    int containerId = buf.readVarInt();
    boolean rightClick = buf.readBoolean();
    String stringData = buf.readUtf();
    int intData = buf.readVarInt();
    return new ContainerActionPacket(action, containerId, rightClick, stringData, intData);
  }

  public static void handle(ContainerActionPacket packet, PacketContext ctx) {
    ctx.enqueueWork(() -> {
      ServerPlayer player = ctx.getServerPlayer();
      if (player == null) return;

      // Verify container ID matches
      if (player.containerMenu.containerId != packet.containerId) {
        return;
      }

      // TODO: Handle actions when menu classes are ported to 1.18.2
      // For now, this is a stub that acknowledges the packet
    });
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
    PORTFOLIO_SORT
  }
}
