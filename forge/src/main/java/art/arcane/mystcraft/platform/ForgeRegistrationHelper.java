package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IRegistrationHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

// Forge registration is handled by the existing registry classes (MystcraftRegistries, ForgeModBlocks, etc.)
// This stub delegates to those classes.
public class ForgeRegistrationHelper implements IRegistrationHelper {

    @Override
    public void register() {
        // Forge DeferredRegisters are registered in MystcraftForge via MystcraftRegistries.register(bus)
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
        throw new UnsupportedOperationException("Use ForgeModBlocks for Forge block registration");
    }

    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
        throw new UnsupportedOperationException("Use ForgeModItems for Forge item registration");
    }

    @Override
    public <T extends BlockEntityType<?>> Supplier<T> registerBlockEntity(String name, Supplier<T> type) {
        throw new UnsupportedOperationException("Use ForgeModBlockEntities for Forge block entity registration");
    }

    @Override
    public <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> type) {
        throw new UnsupportedOperationException("Use ForgeModEntities for Forge entity registration");
    }

    @Override
    public <T extends Fluid> Supplier<T> registerFluid(String name, Supplier<T> fluid) {
        throw new UnsupportedOperationException("Use ForgeModFluids for Forge fluid registration");
    }

    @Override
    public Supplier<SoundEvent> registerSound(String name, Supplier<SoundEvent> sound) {
        throw new UnsupportedOperationException("Use ForgeModSounds for Forge sound registration");
    }

    @Override
    public Supplier<CreativeModeTab> registerCreativeTab(String name, Supplier<CreativeModeTab> tab) {
        throw new UnsupportedOperationException("Use ModCreativeTabs for Forge creative tab registration");
    }

    @Override
    public <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType) {
        throw new UnsupportedOperationException("Use ForgeModMenuTypes for Forge menu type registration");
    }
}
