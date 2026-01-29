package art.arcane.mystcraft.platform.services;

/**
 * Abstracts client-side registration operations.
 * Handles menu screen bindings, entity renderers, block entity renderers, model layers, and item colors.
 */
public interface IClientHelper {

  /**
   * Registers menu screen factories (binds MenuType to Screen).
   */
  void registerMenuScreens();

  /**
   * Registers block entity renderers.
   */
  void registerBlockEntityRenderers();

  /**
   * Registers entity renderers.
   */
  void registerEntityRenderers();

  /**
   * Registers model layer definitions.
   */
  void registerModelLayers();

  /**
   * Registers item and block color handlers.
   */
  void registerColorHandlers();

  /**
   * Registers custom render types for blocks.
   */
  void registerRenderTypes();
}
