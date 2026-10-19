package mod.crabmod.showercore.geometry;

/** Shape contracts in model pixels (16 pixels per block), independent of materials. */
public final class BathroomGeometry {
    public static final Tub STANDARD_TUB = new Tub(12, .6f, 4, .1, java.util.List.of(new Point(8,11,2)), new Drop(7,3,9,4,11));
    public static final Tub CLAWFOOT_TUB = new Tub(16, .74f, 4, .32, java.util.List.of(new Point(14,15,7), new Point(14,15,10), new Point(14,15,13)), new Drop(12.5,9,13.5,11,15));
    public static final Shower RAIN_SHOWER = new Shower(-16, 15, 8, 12.5, 8, 9, 5,
            new Bounds(2.75, -16, 5.5, 12.5, 15, 16));
    public static final Shower COMPACT_SHOWER = new Shower(-5, 15, 8, 12.5, 8.5, 6, 6,
            new Bounds(3, -5.01, 5.5, 13, 15, 16.25));

    public static final Tub OFURO_TUB = new Tub(15, 0.78125f, 3, 0.2, java.util.List.of(new Point(13.25, 15, 2.5)), new Drop(12.75, 6.25, 13.75, 7, 20.75));
    public static final Tub ROMAN_TUB = new Tub(16, 0.65625f, 3, 0.1, java.util.List.of(new Point(8, 11.25, 3)), new Drop(7.6, 4, 8.4, 4.6, 11.9));
    public static final Tub SEASHELL_TUB = new Tub(12, 0.59375f, 3, 0.1, java.util.List.of(new Point(12.5, 12, 2.5)), new Drop(12.1, 3.85, 12.9, 4.4, 13.75));
    public static final Tub WASHTUB_TUB = new Tub(11, 0.546875f, 2, 0.05, java.util.List.of(new Point(2, 13, 4.5)), new Drop(4.6, 4.1, 5.4, 4.9, 13.5));
    public static final Tub SPA_TUB = new Tub(13, 0.65625f, 3.5, 0.12, java.util.List.of(new Point(8, 10, 3), new Point(15, 12, 5.5)), new Drop(5.5, 3.5, 10.5, 3.75, 11));
    public static final Shower BAMBOO_SHOWER = new Shower(-16, 14, 8, 10, 7, 6, 6, new Bounds(4, -16, 3, 12, 14, 16));
    public static final Shower COLUMN_SHOWER = new Shower(-16, 15, 8, 12.5, 8, 8, 8, new Bounds(3.5, -16, 3.5, 12.5, 15, 16));
    public static final Shower CORAL_SHOWER = new Shower(-16, 15, 8, 12.5, 10, 10, 6, new Bounds(1.5, -16, 7, 14.5, 15, 16.001));
    public static final Shower BUCKET_SHOWER = new Shower(-16, 15, 8, 6.75, 6, 4, 4, new Bounds(4.85, -16, 3.05, 11.531, 15, 16));
    public static final Shower PANEL_SHOWER = new Shower(-16, 14, 8, 12.5, 8, 8, 8, new Bounds(3, -16, 3, 13, 14, 16));

    private BathroomGeometry() {}

    public record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public Bounds {
            if (!(minX < maxX && minY < maxY && minZ < maxZ)) throw new IllegalArgumentException("Invalid outline bounds");
        }
    }

    /** The back rim is the mounting plane; decorative faucets do not define height. */
    public record Point(double x, double y, double z) {}
    public record Drop(double x1, double z1, double x2, double z2, double topY) {}

    public record Tub(int rimY, float waterLevel, double floorY, double seatOffset,
                      java.util.List<Point> faucets, Drop drop) {
        public Tub(int rimY) { this(rimY, Math.min(.6f, rimY / 32f), Math.min(4, rimY / 4.0), .1, java.util.List.of(new Point(8,11,2)), new Drop(7,3,9,4,rimY)); }
        public Tub {
            faucets = java.util.List.copyOf(faucets);
            if (!(floorY >= 0 && floorY / 16 < waterLevel && waterLevel < rimY / 16.0)
                    || faucets.isEmpty() || drop.topY / 16 <= waterLevel || drop.x1 >= drop.x2 || drop.z1 >= drop.z2)
                throw new IllegalArgumentException("Invalid tub geometry");
            if (rimY <= 0 || rimY > 16) throw new IllegalArgumentException("Tub rim must be within one block");
        }
    }

    /** NORTH model: the mounting end lies on the back edge, z=16. */
    public record Shower(int mountY, int topY, double outletX, double outletY,
                         double outletZ, double width, double depth, Bounds bounds) {
        public Shower {
            if (mountY > 0 || mountY < -16 || topY <= 0 || topY > 16
                    || outletY <= 0 || outletY > topY || width <= 0 || depth <= 0
                    || outletX - width / 2 < 0 || outletX + width / 2 > 16
                    || outletZ - depth / 2 < 0 || outletZ + depth / 2 > 16
                    || bounds.maxY != topY || bounds.minY > mountY) {
                throw new IllegalArgumentException("Invalid shower installation geometry");
            }
        }

        public Mount on(Tub tub) {
            int origin = tub.rimY - mountY;
            // The outlet must stay inside its owning cell for native ray targeting.
            int above = (int)Math.floor((origin + outletY) / 16.0);
            return new Mount(above, origin - above * 16);
        }
    }

    /** Whole-block position plus the sub-block translation shared by every consumer. */
    public record Mount(int above, int offsetPixels) {
        public double offset() { return offsetPixels / 16.0; }
        public int topBlock(int topY) { return above + Math.floorDiv(offsetPixels + topY - 1, 16); }
    }
}
