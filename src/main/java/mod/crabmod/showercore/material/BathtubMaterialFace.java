package mod.crabmod.showercore.material;

/** Missing face data is deliberately kept distinct from newly selected side textures. */
public enum BathtubMaterialFace {
    LEGACY, SIDE, TOP;

    public static final String TAG = "MaterialFace";

    public static BathtubMaterialFace read(String value) {
        return switch (value) {
            case "side" -> SIDE;
            case "top" -> TOP;
            default -> LEGACY;
        };
    }

    public String serializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
