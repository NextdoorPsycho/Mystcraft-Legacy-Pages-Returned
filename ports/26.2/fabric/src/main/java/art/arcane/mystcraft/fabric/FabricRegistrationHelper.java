package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.services.IRegistrationHelper;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

/** Direct built-in-registry implementation used by Fabric. */
public final class FabricRegistrationHelper implements IRegistrationHelper {

  @Override
  public void initialize(Object ignoredEventBus) {
  }

  @Override
  public void register() {
    FabricRegistries.register();
  }

  @Override
  public void populateCommonRegistries() {
    FabricRegistries.populateCommonRegistries();
  }

  @Override
  public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
    return register(BuiltInRegistries.BLOCK, name, block);
  }

  @Override
  public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
    return register(BuiltInRegistries.ITEM, name, item);
  }

  @Override
  public <T extends BlockEntityType<?>> Supplier<T> registerBlockEntity(
      String name,
      Supplier<T> type
  ) {
    return register(BuiltInRegistries.BLOCK_ENTITY_TYPE, name, type);
  }

  @Override
  public <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> type) {
    return register(BuiltInRegistries.ENTITY_TYPE, name, type);
  }

  @Override
  public <T extends Fluid> Supplier<T> registerFluid(String name, Supplier<T> fluid) {
    return register(BuiltInRegistries.FLUID, name, fluid);
  }

  @Override
  public Supplier<SoundEvent> registerSound(String name, Supplier<SoundEvent> sound) {
    return register(BuiltInRegistries.SOUND_EVENT, name, sound);
  }

  @Override
  public Supplier<CreativeModeTab> registerCreativeTab(
      String name,
      Supplier<CreativeModeTab> tab
  ) {
    return register(BuiltInRegistries.CREATIVE_MODE_TAB, name, tab);
  }

  @Override
  public <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType) {
    return register(BuiltInRegistries.MENU, name, menuType);
  }

  private static <V, T extends V> Supplier<T> register(
      Registry<V> registry,
      String path,
      Supplier<T> factory
  ) {
    T value = Registry.register(registry, id(path), factory.get());
    return () -> value;
  }

  static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
  }
}
