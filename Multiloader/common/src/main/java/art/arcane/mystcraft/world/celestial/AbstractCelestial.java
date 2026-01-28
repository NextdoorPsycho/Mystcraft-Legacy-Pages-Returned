package art.arcane.mystcraft.world.celestial;

import art.arcane.mystcraft.api.world.logic.ICelestial;

/**
 * Abstract base class for celestial objects.
 * Provides common functionality for sun, moon, and stars.
 */
public abstract class AbstractCelestial implements ICelestial {

    protected final CelestialType type;
    protected int color = -1;
    protected float angle = 0.0f;
    protected float phase = 0.0f;
    protected float period = 1.0f;
    protected float size = 1.0f;
    protected boolean visible = true;
    protected boolean dark = false;

    protected AbstractCelestial(CelestialType type) {
        this.type = type;
    }

    @Override
    public CelestialType getCelestialType() {
        return type;
    }

    @Override
    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    @Override
    public float getPhase() {
        return phase;
    }

    public void setPhase(float phase) {
        this.phase = phase;
    }

    @Override
    public float getPeriod() {
        return period;
    }

    public void setPeriod(float period) {
        this.period = Math.max(0.1f, period);
    }

    @Override
    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = Math.max(0.1f, size);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }
}
