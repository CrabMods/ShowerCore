package mod.crabmod.showercore.gametest;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.entity.*;
import mod.crabmod.showercore.registers.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.*;

/** Native player-use coverage for #9/#12. Excluded from the production JAR. */
@GameTestHolder("showercore") @PrefixGameTestTemplate(false)
public final class DuckPlacementGameTests {
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_green_north(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_GREEN.get(),Direction.NORTH,false);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_green_east(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_GREEN.get(),Direction.EAST,false);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_green_south(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_GREEN.get(),Direction.SOUTH,false);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_green_west(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_GREEN.get(),Direction.WEST,false);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_white_north(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),Direction.NORTH,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_white_east(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),Direction.EAST,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_white_south(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),Direction.SOUTH,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_white_west(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),Direction.WEST,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_oak_north(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_OAK.get(),Direction.NORTH,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_oak_east(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_OAK.get(),Direction.EAST,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_oak_south(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_OAK.get(),Direction.SOUTH,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_oak_west(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_OAK.get(),Direction.WEST,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_iron_north(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_IRON.get(),Direction.NORTH,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_iron_east(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_IRON.get(),Direction.EAST,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_iron_south(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_IRON.get(),Direction.SOUTH,true);}
 @GameTest(template="empty",timeoutTicks=100) public static void duck_rim_iron_west(GameTestHelper h){matrix(h,BlocksRegister.BATHTUB_CLAWFOOT_IRON.get(),Direction.WEST,true);}
 private static void matrix(GameTestHelper h,Block block,Direction facing,boolean clawfoot){
  var level=h.getLevel();var relative=new BlockPos(1,1,1);var foot=h.absolutePos(relative);var head=foot.relative(facing);
  var state=block.defaultBlockState().setValue(BathtubBlock.FACING,facing).setValue(BathtubBlock.PART,BedPart.FOOT)
   .setValue(BathtubBlock.LIQUID,BathtubBlock.LiquidType.EMPTY).setValue(BathtubBlock.RUNNING,false);
  level.setBlock(foot,state,3);((BathtubBlock)block).setPlacedBy(level,foot,state,null,ItemStack.EMPTY);
  var p=h.makeMockPlayer();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
  h.useBlock(relative,p,new BlockHitResult(Vec3.atCenterOf(foot),Direction.UP,foot,false));
  for(var pos:List.of(foot,head))if(((BathtubBlockEntity)level.getBlockEntity(pos)).getFluidTank().getFluidAmount()!=1000)
   throw new GameTestAssertException("Native bucket fill lost a bathtub half");
  var owned=new ArrayList<RubberDuckEntity>();var origins=new HashMap<UUID,BlockPos>();
  for(var pos:List.of(foot,head))for(double x:new double[]{0.05,0.5,0.95})for(double z:new double[]{0.05,0.5,0.95}){
   var before=level.getEntitiesOfClass(RubberDuckEntity.class,new AABB(pos).inflate(1)).stream().map(Entity::getUUID).toList();
   p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ItemRegister.RUBBER_DUCK.get()));
   h.useBlock(pos.subtract(h.absolutePos(BlockPos.ZERO)),p,new BlockHitResult(new Vec3(pos.getX()+x,pos.getY()+1,pos.getZ()+z),Direction.UP,pos,false));
   var added=level.getEntitiesOfClass(RubberDuckEntity.class,new AABB(pos).inflate(1)).stream().filter(e->!before.contains(e.getUUID())).toList();
   if(added.size()!=1||p.getItemInHand(InteractionHand.MAIN_HAND).getCount()!=0||p.isPassenger())
    throw new GameTestAssertException("Native placement must create one duck, consume one item and not seat the player");
   var d=added.get(0);owned.add(d);origins.put(d.getUUID(),pos);
   near(pos.getX()+Math.max(.375,Math.min(.625,x)),d.getX(),.001,"initial x");
   near(pos.getZ()+Math.max(.375,Math.min(.625,z)),d.getZ(),.001,"initial z");
   near(pos.getY()+(clawfoot?.79:.65),d.getY(),.002,"initial float height");
  }
  for(int delay:new int[]{10,20,40})h.runAfterDelay(delay,()->{
   for(var d:owned){var pos=origins.get(d.getUUID());var b=d.getBoundingBox();
    if(d.isRemoved()||b.minX<pos.getX()+.124||b.maxX>pos.getX()+.876||b.minZ<pos.getZ()+.124||b.maxZ>pos.getZ()+.876)
     throw new GameTestAssertException("Duck crossed the basin rim after "+delay+" natural ticks: "+d.position());
    if(d.getY()<pos.getY()+(clawfoot?.68:.54)||d.getY()>pos.getY()+(clawfoot?.82:.68))
     throw new GameTestAssertException("Duck lost its bathtub float surface after "+delay+" natural ticks: "+d.position());
   }
   if(delay==40){for(var d:owned)d.discard();for(var e:level.getEntitiesOfClass(FaucetInteractionEntity.class,new AABB(head).inflate(.5)))e.discard();
    level.setBlock(foot,Blocks.AIR.defaultBlockState(),3);level.setBlock(head,Blocks.AIR.defaultBlockState(),3);
    for(var d:owned)if(!d.isRemoved())throw new GameTestAssertException("Owned duck cleanup failed");h.succeed();}
  });
 }
 private static void near(double expected,double actual,double tolerance,String label){if(Math.abs(expected-actual)>tolerance)
  throw new GameTestAssertException(label+": expected="+expected+" actual="+actual);}
}
