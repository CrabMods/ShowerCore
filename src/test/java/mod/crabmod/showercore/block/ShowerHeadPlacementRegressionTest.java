package mod.crabmod.showercore.block;

import com.google.gson.JsonParser;
import mod.crabmod.showercore.geometry.BathroomGeometry;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ShowerHeadPlacementRegressionTest {
    @Test
    void currentFourPairsHaveStableMounts() {
        assertEquals(new BathroomGeometry.Mount(2, -4), BathroomGeometry.RAIN_SHOWER.on(BathroomGeometry.STANDARD_TUB));
        assertEquals(new BathroomGeometry.Mount(2, 0), BathroomGeometry.RAIN_SHOWER.on(BathroomGeometry.CLAWFOOT_TUB));
        assertEquals(new BathroomGeometry.Mount(1, 1), BathroomGeometry.COMPACT_SHOWER.on(BathroomGeometry.STANDARD_TUB));
        assertEquals(new BathroomGeometry.Mount(2, -11), BathroomGeometry.COMPACT_SHOWER.on(BathroomGeometry.CLAWFOOT_TUB));
    }

    @Test
    void everySupportedRimAlignsWithoutLosingTheNozzleOwningCell() {
        for (int rim = 1; rim <= 16; rim++) {
            for (var shower : List.of(BathroomGeometry.RAIN_SHOWER, BathroomGeometry.COMPACT_SHOWER)) {
                var mount = shower.on(new BathroomGeometry.Tub(rim));
                assertEquals(rim, mount.above() * 16 + mount.offsetPixels() + shower.mountY());
                assertTrue(mount.offsetPixels() >= -15 && mount.offsetPixels() <= 15);
                assertTrue(mount.topBlock(shower.topY()) >= mount.above());
                assertTrue(mount.offsetPixels() + shower.outletY() >= 0);
                assertTrue(mount.offsetPixels() + shower.outletY() < 16);
            }
        }
    }

    @Test
    void showerContractsMatchTheActualModelMountAndSprayPlate() throws Exception {
        for (var name : List.of("rain_shower_head", "compact_shower_head", "bamboo_shower_head", "column_shower_head", "coral_shower_head", "bucket_shower_head", "panel_shower_head")) {
            var geometry = switch(name.split("_")[0]) { case "rain" -> BathroomGeometry.RAIN_SHOWER; case "compact" -> BathroomGeometry.COMPACT_SHOWER; case "bamboo" -> BathroomGeometry.BAMBOO_SHOWER; case "column" -> BathroomGeometry.COLUMN_SHOWER; case "coral" -> BathroomGeometry.CORAL_SHOWER; case "bucket" -> BathroomGeometry.BUCKET_SHOWER; default -> BathroomGeometry.PANEL_SHOWER; };
            var model = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/showercore/models/block/template", name + ".json"))).getAsJsonObject();
            double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            double minX=minY, minZ=minY, maxX=maxY, maxZ=maxY;
            boolean foundPlate = false;
            double sprayMinX=Double.POSITIVE_INFINITY,sprayMaxX=Double.NEGATIVE_INFINITY;
            double sprayMinZ=Double.POSITIVE_INFINITY,sprayMaxZ=Double.NEGATIVE_INFINITY;
            for (var element : model.getAsJsonArray("elements")) {
                var e = element.getAsJsonObject();var from = e.getAsJsonArray("from");var to = e.getAsJsonArray("to");
                for(int bits=0;bits<8;bits++) {
                    double[] point=new double[3];for(int axis=0;axis<3;axis++)point[axis]=((bits>>axis)&1)==0?from.get(axis).getAsDouble():to.get(axis).getAsDouble();
                    if(e.has("rotation")) {
                        var rotation=e.getAsJsonObject("rotation");var origin=rotation.getAsJsonArray("origin");
                        int axis="xyz".indexOf(rotation.get("axis").getAsString());int u=(axis+1)%3,v=(axis+2)%3;
                        double angle=Math.toRadians(rotation.get("angle").getAsDouble()),cos=Math.cos(angle),sin=Math.sin(angle);
                        double pu=point[u]-origin.get(u).getAsDouble(),pv=point[v]-origin.get(v).getAsDouble();
                        double scale=rotation.has("rescale")&&rotation.get("rescale").getAsBoolean()?1/cos:1;
                        point[u]=origin.get(u).getAsDouble()+(pu*cos-pv*sin)*scale;
                        point[v]=origin.get(v).getAsDouble()+(pu*sin+pv*cos)*scale;
                    }
                    minX=Math.min(minX,point[0]);maxX=Math.max(maxX,point[0]);
                    minY=Math.min(minY,point[1]);maxY=Math.max(maxY,point[1]);
                    minZ=Math.min(minZ,point[2]);maxZ=Math.max(maxZ,point[2]);
                }
                var down=e.getAsJsonObject("faces").getAsJsonObject("down");
                if(down!=null && down.get("texture").getAsString().equals("#spray") && Math.abs(from.get(1).getAsDouble()-geometry.outletY())<=.3) {
                    sprayMinX=Math.min(sprayMinX,from.get(0).getAsDouble());sprayMaxX=Math.max(sprayMaxX,to.get(0).getAsDouble());
                    sprayMinZ=Math.min(sprayMinZ,from.get(2).getAsDouble());sprayMaxZ=Math.max(sprayMaxZ,to.get(2).getAsDouble());
                }
                if (Math.abs(from.get(1).getAsDouble() - geometry.outletY()) < .0001) {
                    foundPlate |= from.get(0).getAsDouble() == geometry.outletX() - geometry.width()/2
                            && to.get(0).getAsDouble() == geometry.outletX() + geometry.width()/2
                            && from.get(2).getAsDouble() == geometry.outletZ() - geometry.depth()/2
                            && to.get(2).getAsDouble() == geometry.outletZ() + geometry.depth()/2;
                }
            }
            assertEquals(geometry.mountY(), minY, .02, name + " mounting end");
            assertEquals(geometry.topY(), maxY, .001, name + " top");
            var actual=new double[]{minX,minY,minZ,maxX,maxY,maxZ};var bounds=geometry.bounds();
            var declared=new double[]{bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()};
            for(int axis=0;axis<6;axis++) assertEquals(Math.round(actual[axis]*1000)/1000.0,declared[axis],.000001,name+" rotated model bounds "+axis);
            if(name.startsWith("rain") || name.startsWith("compact")) assertTrue(foundPlate, name + " outlet must match the actual nozzle plate");
            else assertTrue(sprayMinX<=geometry.outletX()-geometry.width()/2 && sprayMaxX>=geometry.outletX()+geometry.width()/2
                    && sprayMinZ<=geometry.outletZ()-geometry.depth()/2 && sprayMaxZ>=geometry.outletZ()+geometry.depth()/2,
                    name+" declared spray footprint must fit the actual nozzle faces");
        }
    }

    @Test
    void invalidGeometryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BathroomGeometry.Tub(0));
        assertThrows(IllegalArgumentException.class, () -> new BathroomGeometry.Tub(17));
        assertThrows(IllegalArgumentException.class, () -> new BathroomGeometry.Shower(-17,15,8,12,8,6,6,BathroomGeometry.RAIN_SHOWER.bounds()));
        assertThrows(IllegalArgumentException.class, () -> new BathroomGeometry.Shower(-5,15,8,16,8,6,6,BathroomGeometry.RAIN_SHOWER.bounds()));
    }
}
