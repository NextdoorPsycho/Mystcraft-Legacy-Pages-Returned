package art.arcane.mystcraft.item;

/**
 * Forge-specific PageItem for 1.18.2.
 * Note: IClientItemExtensions doesn't exist in 1.18.2 Forge.
 * Custom BEWLR requires different registration.
 */
public class ForgePageItem extends PageItem {

  public ForgePageItem(Properties properties) {
    super(properties);
  }

  // In 1.18.2, custom item renderers are handled differently.
  // The BEWLR functionality is registered separately in FMLClientSetupEvent.
}
