package art.arcane.mystcraft.platform.services;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Supplier;

/**
 * Abstracts platform-specific registry operations.
 * Each platform implements this to handle DeferredRegister (Forge/NeoForge) or direct registration (Fabric).
 */
public interface IRegistrationHelper {

  /**
   * Initializes the registration system with the platform's mod event bus.
   * For Forge/NeoForge this is the IEventBus, for Fabric this may be null.
   * Must be called before any registrations.
   * @param modEventBus Platform-specific event bus (IEventBus for Forge/NeoForge, null for Fabric)
   */
  void initialize(Object modEventBus);

  /**
   * Registers all deferred registries with the platform's mod event bus. Called during mod construction.
   */
  void register();

  /**
   * Registers a block supplier. Returns a Supplier that resolves after registration completes.
   */
  <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block);

  /**
   * Registers an item supplier.
   */
  <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item);

  /**
   * Registers a block entity type supplier.
   */
  <T extends BlockEntityType<?>> Supplier<T> registerBlockEntity(String name, Supplier<T> type);

  /**
   * Registers an entity type supplier.
   */
  <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> type);

  /**
   * Registers a fluid supplier.
   */
  <T extends Fluid> Supplier<T> registerFluid(String name, Supplier<T> fluid);

  /**
   * Registers a sound event supplier.
   */
  Supplier<SoundEvent> registerSound(String name, Supplier<SoundEvent> sound);

  /**
   * Registers a creative mode tab supplier.
   */
  Supplier<CreativeModeTab> registerCreativeTab(String name, Supplier<CreativeModeTab> tab);

  /**
   * Registers a menu type supplier.
   */
  <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType);

  /**
   * Populates common registry stubs from platform-specific RegistryObjects.
   * Called after all registrations are complete.
   */
  void populateCommonRegistries();
}
