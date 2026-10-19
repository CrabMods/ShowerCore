package mod.crabmod.showercore.geometry;

import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;

/** A rim supplies a world-space mounting plane; each shower supplies its own geometry. */
public interface BathMountSurface {
    @Nullable Support showerSupport(Level level,BlockPos clicked,BlockHitResult hit,BathroomGeometry.Shower shower);
    static Vec3 rotate(double x,double z,Direction facing) {
        return switch(facing) {
            case EAST -> new Vec3(1-z,0,x);
            case SOUTH -> new Vec3(1-x,0,1-z);
            case WEST -> new Vec3(z,0,1-x);
            default -> new Vec3(x,0,z);
        };
    }
    record Support(Vec3 point,Direction facing) {
        public BlockPos baseBlock() {
            return BlockPos.containing(Math.nextAfter(point.x,point.x+facing.getStepX()),Math.nextDown(point.y),
                    Math.nextAfter(point.z,point.z+facing.getStepZ()));
        }
        public Placement place(BathroomGeometry.Shower shower) {
            var reference=rotate(.5,1,facing);
            var outlet=rotate(shower.outletX()/16,shower.outletZ()/16,facing);
            double originY=point.y-shower.mountY()/16.0;
            var pos=BlockPos.containing(point.x+outlet.x-reference.x,originY+shower.outletY()/16,point.z+outlet.z-reference.z);
            int offset=(int)Math.round((originY-pos.getY())*16);
            if(offset< -15||offset>15)throw new IllegalArgumentException("Unrepresentable rim height");
            return new Placement(pos,facing,offset,(float)(point.x-pos.getX()-reference.x),(float)(point.z-pos.getZ()-reference.z),point.y);
        }
    }
    record Placement(BlockPos pos,Direction facing,int offsetPixels,float xShift,float zShift,double rimY) {
        public Vec3 outlet(BathroomGeometry.Shower shower) {
            return rotate(shower.outletX()/16,shower.outletZ()/16,facing)
                    .add(pos.getX(),pos.getY()+offsetPixels/16.0+shower.outletY()/16,pos.getZ()).add(xShift,0,zShift);
        }
        public AABB clearance(BathroomGeometry.Shower shower) {
            var b=shower.bounds();
            double minX=Double.POSITIVE_INFINITY,minZ=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,maxZ=Double.NEGATIVE_INFINITY;
            for(double x:new double[]{b.minX()/16,b.maxX()/16})for(double z:new double[]{b.minZ()/16,b.maxZ()/16}) {
                var p=rotate(x,z,facing);minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minZ=Math.min(minZ,p.z);maxZ=Math.max(maxZ,p.z);
            }
            double originY=pos.getY()+offsetPixels/16.0;
            double originX=pos.getX()+(double)xShift,originZ=pos.getZ()+(double)zShift;
            return new AABB(originX+minX,Math.max(rimY,originY+b.minY()/16),originZ+minZ,
                    originX+maxX,originY+b.maxY()/16,originZ+maxZ);
        }
    }
}
