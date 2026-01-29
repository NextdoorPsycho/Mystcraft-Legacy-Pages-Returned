package art.arcane.mystcraft.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;

/**
 * The Glasses item.
 * Allows the player to see hidden Age symbols and instability.
 * When worn, provides visual overlays showing symbol information.
 */
public class GlassesItem extends Item implements Equipable {

  public GlassesItem(Properties properties) {
    super(properties);
  }

  @Override
  public EquipmentSlot getEquipmentSlot() {
    return EquipmentSlot.HEAD;
  }

  // Client-side rendering implemented in:
  // - GlassesOverlay.java - Shows age info, instability, and symbol visibility
  // - InstabilityEffects.java - Visual effects based on instability level
}
