package art.arcane.mystcraft.blockentity;

/**
 * Interface for block entities that support rotation.
 * Used by bookstands, lecterns, and other rotatable displays.
 */
public interface IRotateableBlockEntity {

  /**
   * Gets the yaw rotation (horizontal rotation) in degrees.
   */
  short getYaw();

  /**
   * Sets the yaw rotation (horizontal rotation) in degrees.
   */
  void setYaw(int yaw);

  /**
   * Gets the pitch rotation (vertical tilt) in degrees.
   */
  short getPitch();

  /**
   * Sets the pitch rotation (vertical tilt) in degrees.
   */
  void setPitch(int pitch);

  /**
   * Snaps the yaw to the nearest increment.
   * For example, 45 degree increments for bookstands, 90 for lecterns.
   */
  default int snapYaw(int yaw, int increment) {
    return Math.round((float) yaw / increment) * increment;
  }

  /**
   * Snaps the pitch to the nearest increment.
   */
  default int snapPitch(int pitch, int increment) {
    return Math.round((float) pitch / increment) * increment;
  }
}
