package mod.crabmod.showercore.block;
import mod.crabmod.showercore.geometry.BathroomGeometry;
import mod.crabmod.showercore.client.renderer.BathtubDropGeometry;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class BathroomStyleGeometryTest {
    @Test void everyShapeUsesOneValidInstallationAndLiquidContract() {
        for(var tub:List.of(BathroomGeometry.STANDARD_TUB,BathroomGeometry.CLAWFOOT_TUB,BathroomGeometry.OFURO_TUB,
                BathroomGeometry.ROMAN_TUB,BathroomGeometry.SEASHELL_TUB,BathroomGeometry.WASHTUB_TUB,BathroomGeometry.SPA_TUB)) {
            assertTrue(tub.floorY()/16<tub.waterLevel() && tub.waterLevel()<tub.rimY()/16.0);
            for(var shower:List.of(BathroomGeometry.RAIN_SHOWER,BathroomGeometry.COMPACT_SHOWER,BathroomGeometry.BAMBOO_SHOWER,
                    BathroomGeometry.COLUMN_SHOWER,BathroomGeometry.CORAL_SHOWER,BathroomGeometry.BUCKET_SHOWER,BathroomGeometry.PANEL_SHOWER)) {
                var mount=shower.on(tub);
                assertEquals(tub.rimY(),mount.above()*16+mount.offsetPixels()+shower.mountY());
                assertTrue(mount.offsetPixels()>=-15 && mount.offsetPixels()<=15);
                assertTrue(mount.offsetPixels()+shower.outletY()>=0 && mount.offsetPixels()+shower.outletY()<16);
            }
            for(var facing:BathtubDropGeometry.FaucetSide.values()) {
                var drop=BathtubDropGeometry.computeDropBounds(facing,tub);
                assertEquals(tub.waterLevel(),drop[1]); assertEquals(tub.drop().topY()/16,drop[4],1e-6);
                assertTrue(drop[0]<drop[3] && drop[2]<drop[5]);
            }
        }
    }
    @Test void legacyDropGeometryIsUnchanged() {
        for(var facing:BathtubDropGeometry.FaucetSide.values()) {
            assertArrayEquals(BathtubDropGeometry.computeDropBounds(facing),BathtubDropGeometry.computeDropBounds(facing,BathroomGeometry.STANDARD_TUB),1e-6f);
        }
        assertArrayEquals(BathtubDropGeometry.computeDropBounds(BathtubDropGeometry.FaucetSide.EAST,true),
                BathtubDropGeometry.computeDropBounds(BathtubDropGeometry.FaucetSide.NORTH,BathroomGeometry.CLAWFOOT_TUB),1e-6f);
    }
}
