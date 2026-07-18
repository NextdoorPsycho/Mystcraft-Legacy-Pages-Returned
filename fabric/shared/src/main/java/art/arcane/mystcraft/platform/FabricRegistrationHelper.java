package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.services.IRegistrationHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
 * Fabric service implementation for registering common Mystcraft objects.
 */
public class FabricRegistrationHelper implements IRegistrationHelper {

  @Override
  public void initialize(Object modEventBus) {
  }

  @Override
  public void register() {
  }

  @Override
  public void populateCommonRegistries() {
  }

  @Override
  public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
    T value = Registry.register(BuiltInRegistries.BLOCK, id(name), block.get());
    return () -> value;
  }

  @Override
  public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
    T value = Registry.register(BuiltInRegistries.ITEM, id(name), item.get());
    return () -> value;
  }

  @Override
  public <T extends BlockEntityType<?>> Supplier<T> registerBlockEntity(String name, Supplier<T> type) {
    T value = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(name), type.get());
    return () -> value;
  }

  @Override
  public <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> type) {
    T value = Registry.register(BuiltInRegistries.ENTITY_TYPE, id(name), type.get());
    return () -> value;
  }

  @Override
  public <T extends Fluid> Supplier<T> registerFluid(String name, Supplier<T> fluid) {
    T value = Registry.register(BuiltInRegistries.FLUID, id(name), fluid.get());
    return () -> value;
  }

  @Override
  public Supplier<SoundEvent> registerSound(String name, Supplier<SoundEvent> sound) {
    SoundEvent value = Registry.register(BuiltInRegistries.SOUND_EVENT, id(name), sound.get());
    return () -> value;
  }

  @Override
  public Supplier<CreativeModeTab> registerCreativeTab(String name, Supplier<CreativeModeTab> tab) {
    CreativeModeTab value = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id(name), tab.get());
    return () -> value;
  }

  @Override
  public <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType) {
    T value = Registry.register(BuiltInRegistries.MENU, id(name), menuType.get());
    return () -> value;
  }

  private static ResourceLocation id(String name) {
    return new ResourceLocation(Mystcraft.MOD_ID, name);
  }
}
