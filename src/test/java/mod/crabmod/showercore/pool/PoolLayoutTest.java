package mod.crabmod.showercore.pool;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PoolLayoutTest {
    private static List<PoolSpec> samples() {
        return List.of(new PoolSpec(PoolSpec.Shape.RECTANGLE,3,3,0),new PoolSpec(PoolSpec.Shape.RECTANGLE,16,16,0),
                new PoolSpec(PoolSpec.Shape.ROUNDED,8,6,5),new PoolSpec(PoolSpec.Shape.ROUNDED,3,3,6),
                new PoolSpec(PoolSpec.Shape.CIRCLE,3,3,0),new PoolSpec(PoolSpec.Shape.CIRCLE,16,16,0));
    }
    @Test void rejectsUnboundedAndMalformedDimensions() {
        assertThrows(IllegalArgumentException.class,()->new PoolSpec(PoolSpec.Shape.RECTANGLE,100000,3,0));
        assertThrows(IllegalArgumentException.class,()->new PoolSpec(PoolSpec.Shape.CIRCLE,5,6,0));
        assertThrows(IllegalArgumentException.class,()->new PoolSpec(PoolSpec.Shape.ROUNDED,3,3,7));
        assertThrows(IllegalArgumentException.class,()->new PoolSpec(null,3,3,0));
    }
    @Test void cellMeshesPartitionTheWholeFloorAndWaterWithoutMissingArea() {
        for(var spec:samples()) {
            var layout=PoolLayout.of(spec);
            assertEquals(PoolLayout.area(layout.outline()),layout.cells().stream().mapToDouble(c->PoolLayout.area(c.floor())).sum(),1e-7);
            assertEquals(PoolLayout.area(layout.waterOutline()),layout.cells().stream().mapToDouble(c->PoolLayout.area(c.water())).sum(),1e-7);
            assertNotNull(layout.cell(layout.controllerX(),0));
        }
    }
    @Test void rimFillsTheExactGapBetweenTheFloorAndTheWater() {
        for(var spec:samples()) {
            var layout=PoolLayout.of(spec);
            double rim=layout.cells().stream().flatMap(c->c.rim().stream()).mapToDouble(PoolLayout::area).sum();
            assertEquals(PoolLayout.area(layout.outline())-PoolLayout.area(layout.waterOutline()),rim,1e-7);
        }
    }
    @Test void eachCellStaysWithinItsOwnRenderableBounds() {
        for(var spec:samples())for(var cell:PoolLayout.of(spec).cells())for(var p:cell.floor()) {
            assertTrue(p.x()>=cell.x()-1e-7 && p.x()<=cell.x()+1+1e-7);
            assertTrue(p.z()>=cell.z()-1e-7 && p.z()<=cell.z()+1+1e-7);
        }
    }
    @Test void costChargesEachFloorAndRimCellOnce() {
        for(var spec:samples()) {
            var layout=PoolLayout.of(spec);
            assertEquals(layout.cells().size()+layout.cells().stream().filter(PoolLayout.Cell::hasRim).count(),layout.materialCost());
        }
        assertEquals(9+8,PoolLayout.of(new PoolSpec(PoolSpec.Shape.RECTANGLE,3,3,0)).materialCost());
    }
    @Test void outOfBoundsCoordinatesNeverAliasAnExistingCell() {
        var layout=PoolLayout.of(PoolSpec.DEFAULT);
        assertNull(layout.cell(-1,1));assertNull(layout.cell(5,0));assertNull(layout.cell(0,5));
    }
    @Test void biggerPoolsRequireMoreMaterialAndWater() {
        var small=PoolLayout.of(new PoolSpec(PoolSpec.Shape.CIRCLE,3,3,0));
        var big=PoolLayout.of(new PoolSpec(PoolSpec.Shape.CIRCLE,16,16,0));
        assertTrue(big.materialCost()>small.materialCost());assertTrue(big.capacity()>small.capacity());
        assertEquals(0,big.capacity()%1000);
    }
    @Test void partialWaterLevelsHaveOneSharedPlaneBelowTheRim() {
        for(var spec:samples()) {
            var layout=PoolLayout.of(spec);
            assertEquals(PoolSpec.FLOOR_Y,layout.waterY(0));
            assertEquals(PoolSpec.FULL_WATER_Y,layout.waterY(layout.capacity()));
            assertTrue(layout.waterY(1000)>PoolSpec.FLOOR_Y && layout.waterY(1000)<PoolSpec.RIM_Y);
        }
    }
    @Test void seatsFitThePoolAndNeverOverlap() {
        for(var spec:samples()) {
            var seats=PoolLayout.of(spec).seats();assertFalse(seats.isEmpty());
            for(int i=0;i<seats.size();i++) {
                var a=seats.get(i);assertEquals(i,a.index());assertTrue(spec.contains(a.x(),a.z(),0));
                if(a.kind()==PoolLayout.SeatKind.INSIDE)assertTrue(spec.contains(a.x(),a.z(),.6));
                for(int j=i+1;j<seats.size();j++)assertTrue(Math.hypot(a.x()-seats.get(j).x(),a.z()-seats.get(j).z())>=.85-1e-8);
            }
        }
    }
    @Test void bothSeatKindsAreAvailableForBathhouses() {
        var layout=PoolLayout.of(new PoolSpec(PoolSpec.Shape.ROUNDED,8,8,4));
        assertTrue(layout.seats().stream().anyMatch(s->s.kind()==PoolLayout.SeatKind.RIM));
        assertTrue(layout.seats().stream().anyMatch(s->s.kind()==PoolLayout.SeatKind.INSIDE));
    }
    @Test void clampingKeepsDucksInsideTheCircularWater() {
        var layout=PoolLayout.of(new PoolSpec(PoolSpec.Shape.CIRCLE,7,7,0));
        for(double x:new double[]{-2,0,3,7,10})for(double z:new double[]{-2,0,3,7,10}) {
            var p=layout.clampToInterior(x,z,.55);
            assertTrue(layout.spec().contains(p.x(),p.z(),.55));
        }
    }
}
