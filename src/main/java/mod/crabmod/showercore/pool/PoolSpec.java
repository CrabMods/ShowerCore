package mod.crabmod.showercore.pool;

/** Validated dimensions in blocks; radius is in quarter-block units. */
public record PoolSpec(Shape shape, int width, int length, int radiusQuarters) {
    public enum Shape { RECTANGLE, ROUNDED, CIRCLE }
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 16;
    public static final PoolSpec DEFAULT = new PoolSpec(Shape.ROUNDED, 5, 5, 4);
    public static final double FLOOR_Y = .125;
    public static final double RIM_Y = 1;
    public static final double RIM_WIDTH = .25;
    public static final double FULL_WATER_Y = .875;

    public PoolSpec {
        if (shape == null || width < MIN_SIZE || width > MAX_SIZE || length < MIN_SIZE || length > MAX_SIZE
                || (shape == Shape.CIRCLE && width != length)
                || (shape == Shape.ROUNDED && (radiusQuarters < 1 || radiusQuarters > Math.min(width, length) * 2))
                || (shape != Shape.ROUNDED && radiusQuarters != 0)) {
            throw new IllegalArgumentException("Invalid pool dimensions");
        }
    }

    public double radius() { return shape == Shape.CIRCLE ? width / 2.0 : radiusQuarters / 4.0; }

    public PoolSpec withShape(Shape value) {
        return new PoolSpec(value, width, value == Shape.CIRCLE ? width : length,
                value == Shape.ROUNDED ? Math.max(1, Math.min(radiusQuarters == 0 ? 4 : radiusQuarters, Math.min(width, length) * 2)) : 0);
    }

    public PoolSpec resized(int w, int l) {
        return new PoolSpec(shape, w, shape == Shape.CIRCLE ? w : l,
                shape == Shape.ROUNDED ? Math.min(radiusQuarters, Math.min(w, l) * 2) : 0);
    }

    public boolean contains(double x, double z, double inset) {
        double halfX = width / 2.0 - inset, halfZ = length / 2.0 - inset;
        double radius = Math.max(0, radius() - inset);
        double px = Math.abs(x - width / 2.0), pz = Math.abs(z - length / 2.0);
        return px <= halfX && pz <= halfZ
                && Math.hypot(Math.max(0, px - halfX + radius), Math.max(0, pz - halfZ + radius)) <= radius + 1e-9;
    }
}
