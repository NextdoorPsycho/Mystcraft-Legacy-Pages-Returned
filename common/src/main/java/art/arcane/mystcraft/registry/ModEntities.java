package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.entity.*;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/**
 * Common accessor for registered entity types. Platform modules populate these
 * suppliers during initialization.
 */
public final class ModEntities {

  public static Supplier<EntityType<LinkbookEntity>> LINKBOOK;
  public static Supplier<EntityType<PersonalPocketProxyEntity>> PERSONAL_POCKET_PROXY;
  public static Supplier<EntityType<MystcraftFallingBlockEntity>> FALLING_BLOCK;
  public static Supplier<EntityType<MeteorEntity>> METEOR;
  public static Supplier<EntityType<ColoredLightningEntity>> COLORED_LIGHTNING;

  private ModEntities() {
  }
}
