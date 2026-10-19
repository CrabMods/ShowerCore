package mod.crabmod.showercore.gametest;

import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.ShowerHeadBlock;
import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import mod.crabmod.showercore.geometry.BathroomGeometry;
import mod.crabmod.showercore.registers.BlocksRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ShowerCore.MODID)
@PrefixGameTestTemplate(false)
public final class ShowerMountGameTests {
    private static final BlockPos FOOT = new BlockPos(5,1,5);
    private static Player player(GameTestHelper h) { return h.makeMockSurvivalPlayer(); }
    private static BlockPos bathtub(GameTestHelper h, boolean claw, Direction facing) {
        var foot=h.absolutePos(FOOT);
        var tub=(BathtubBlock)(claw?BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get():BlocksRegister.BATHTUB_WHITE.get());
        var state=tub.defaultBlockState().setValue(BathtubBlock.FACING,facing).setValue(BathtubBlock.PART,BedPart.FOOT);
        h.getLevel().setBlock(foot,state,3);tub.setPlacedBy(h.getLevel(),foot,state,null,ItemStack.EMPTY);
        return foot.relative(facing);
    }
    private static ShowerHeadBlock shower(boolean compact) {
        return (ShowerHeadBlock)(compact?BlocksRegister.COMPACT_SHOWER_HEAD_WHITE.get():BlocksRegister.RAIN_SHOWER_HEAD_WHITE.get());
    }
    private static void install(GameTestHelper h,Player p,BlockPos clicked,ShowerHeadBlock shower) {
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(shower,2));
        var use=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked).add(0,.5,0),Direction.UP,clicked,false));
        h.assertTrue(shower.asItem().useOn(use).consumesAction(),"Explicit bathtub-top installation must succeed");
        h.assertTrue(p.getMainHandItem().getCount()==1,"Installation must consume exactly one shower");
    }
    private static void pair(GameTestHelper h,boolean claw,boolean compact,Direction facing) {
        var level=h.getLevel();var head=bathtub(h,claw,facing);var tub=(BathtubBlock)level.getBlockState(head).getBlock();
        var shower=shower(compact);var mount=shower.geometry().on(tub.mountGeometry());var pos=head.above(mount.above());
        var p=player(h);
        for(var clicked:java.util.List.of(head,head.relative(facing.getOpposite()))) {
            install(h,p,clicked,shower);
            var state=level.getBlockState(pos);
            h.assertTrue(state.is(shower)&&state.getValue(ShowerHeadBlock.FACING)==facing.getOpposite(),"Installation owner/facing");
            h.assertTrue(ShowerHeadBlock.mountOffsetPixels(state)==mount.offsetPixels(),"Offset contract");
            double modelOrigin=pos.getY()+ShowerHeadBlock.mountOffset(state);
            h.assertTrue(Math.abs(modelOrigin+shower.geometry().mountY()/16.0-(head.getY()+tub.mountGeometry().rimY()/16.0))<1e-9,"Mounting planes must coincide");
            var outline=shower.getShape(state,level,pos,net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds();
            var ordinary=shower.getShape(ShowerHeadBlock.withMountOffset(state,0),level,pos,net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds();
            h.assertTrue(Math.abs(outline.minY-ordinary.minY-mount.offset())<1e-9,"Selection must move with model");
            var origin=shower.sprayOrigin(pos,state);var bounds=((ShowerHeadContainerEntity)level.getBlockEntity(pos)).sprayBounds();
            h.assertTrue(Math.abs(bounds.maxY-origin.y)<1e-9&&Math.abs((bounds.minX+bounds.maxX)/2-origin.x)<1e-9,"Effect bounds must follow outlet");
            var hit=level.clip(new net.minecraft.world.level.ClipContext(origin.add(0,.25,0),origin.add(0,-.02,0),
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            h.assertTrue(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos),"Native ray must target the translated shower");
            h.assertTrue(Math.abs(bounds.getXsize()-shower.sprayWidth(state))<1e-9&&Math.abs(bounds.getZsize()-shower.sprayDepth(state))<1e-9,"Effect area must share the emitter dimensions");
            var encoded=net.minecraft.nbt.NbtUtils.writeBlockState(state);
            var restored=net.minecraft.nbt.NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),encoded);
            h.assertTrue(restored==state,"State persistence must keep offset and facing");
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        }
        h.succeed();
    }

    @GameTest(batch="shower_mount", template="light_empty") public static void rain_standard_north(GameTestHelper h){pair(h,false,false,Direction.NORTH);}
    @GameTest(batch="shower_mount", template="light_empty") public static void rain_standard_east(GameTestHelper h){pair(h,false,false,Direction.EAST);}
    @GameTest(batch="shower_mount", template="light_empty") public static void rain_standard_south(GameTestHelper h){pair(h,false,false,Direction.SOUTH);}
    @GameTest(batch="shower_mount", template="light_empty") public static void rain_standard_west(GameTestHelper h){pair(h,false,false,Direction.WEST);}
    @GameTest(batch="shower_mount", template="light_empty") public static void rain_clawfoot_north(GameTestHelper h){pair(h,true,false,Direction.NORTH);}
    @GameTest(batch="shower_mount", template="light_empty") public static void rain_clawfoot_east(GameTestHelper h){pair(h,true,false,Direction.EAST);}
    @GameTest(batch="shower_mount", template="light_empty") public static void rain_clawfoot_south(GameTestHelper h){pair(h,true,false,Direction.SOUTH);}
    @GameTest(batch="shower_mount", template="light_empty") public static void rain_clawfoot_west(GameTestHelper h){pair(h,true,false,Direction.WEST);}
    @GameTest(batch="shower_mount", template="light_empty") public static void compact_standard_north(GameTestHelper h){pair(h,false,true,Direction.NORTH);}
    @GameTest(batch="shower_mount", template="light_empty") public static void compact_standard_east(GameTestHelper h){pair(h,false,true,Direction.EAST);}
    @GameTest(batch="shower_mount", template="light_empty") public static void compact_standard_south(GameTestHelper h){pair(h,false,true,Direction.SOUTH);}
    @GameTest(batch="shower_mount", template="light_empty") public static void compact_standard_west(GameTestHelper h){pair(h,false,true,Direction.WEST);}
    @GameTest(batch="shower_mount", template="light_empty") public static void compact_clawfoot_north(GameTestHelper h){pair(h,true,true,Direction.NORTH);}
    @GameTest(batch="shower_mount", template="light_empty") public static void compact_clawfoot_east(GameTestHelper h){pair(h,true,true,Direction.EAST);}
    @GameTest(batch="shower_mount", template="light_empty") public static void compact_clawfoot_south(GameTestHelper h){pair(h,true,true,Direction.SOUTH);}
    @GameTest(batch="shower_mount", template="light_empty") public static void compact_clawfoot_west(GameTestHelper h){pair(h,true,true,Direction.WEST);}

    @GameTest(batch="shower_mount", template="light_empty")
    public static void occupied_column_does_not_overwrite_or_consume(GameTestHelper h) {
        var head=bathtub(h,false,Direction.NORTH);var p=player(h);var shower=shower(false);
        for(int obstruction=1;obstruction<=2;obstruction++) for(var clickedFace:java.util.List.of(Direction.UP,Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)) {
            h.getLevel().setBlock(head.above(obstruction),Blocks.STONE.defaultBlockState(),3);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(shower,2));
            var result=shower.asItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(head).relative(clickedFace,.5),clickedFace,head,false)));
            h.assertTrue(!result.consumesAction()&&p.getMainHandItem().getCount()==2,"Obstruction must fail without item loss");
            h.assertTrue(h.getLevel().getBlockState(head.above(obstruction)).is(Blocks.STONE),"Existing block overwritten");
            h.getLevel().setBlock(head.above(obstruction),Blocks.AIR.defaultBlockState(),3);
        }
        h.succeed();
    }

    @GameTest(batch="shower_mount", template="light_empty")
    public static void ordinary_wall_placement_and_later_tub_do_not_move_shower(GameTestHelper h) {
        var level=h.getLevel();var p=player(h);var shower=shower(false);var wall=h.absolutePos(new BlockPos(5,3,4));
        level.setBlock(wall,Blocks.STONE.defaultBlockState(),3);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(shower));
        var result=shower.asItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(wall),Direction.SOUTH,wall,false)));
        var pos=wall.south();var original=level.getBlockState(pos);
        h.assertTrue(result.consumesAction()&&original.is(shower)&&ShowerHeadBlock.mountOffsetPixels(original)==0,"Native wall placement changed");
        bathtub(h,true,Direction.NORTH);
        h.assertTrue(level.getBlockState(pos)==original,"A later nearby bathtub must not move the shower");
        h.succeed();
    }

    @GameTest(batch="shower_mount", template="light_empty")
    public static void legacy_states_default_to_zero_offset(GameTestHelper h) {
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Name","showercore:rain_shower_head_white");
        var props=new net.minecraft.nbt.CompoundTag();props.putString("facing","west");props.putString("waterlogged","false");tag.put("Properties",props);
        var state=net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),tag);
        h.assertTrue(ShowerHeadBlock.mountOffsetPixels(state)==0&&state.getValue(ShowerHeadBlock.FACING)==Direction.WEST,"Legacy save moved");
        h.succeed();
    }

    @GameTest(batch="shower_mount", template="light_empty")
    public static void all_registered_material_variants_share_their_shape_contract(GameTestHelper h) {
        for(Block block:net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            String path=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
            if((path.startsWith("rain_") || path.startsWith("compact_")) && block instanceof ShowerHeadBlock shower)h.assertTrue(shower.geometry()==(path.startsWith("compact_")?BathroomGeometry.COMPACT_SHOWER:BathroomGeometry.RAIN_SHOWER),"Shower material changes geometry: "+path);
            if(block instanceof BathtubBlock tub && (path.startsWith("bathtub_clawfoot_") || !java.util.List.of("ofuro","roman","seashell","washtub","spa").contains(path.split("_")[1])))h.assertTrue(tub.mountGeometry()==(path.startsWith("bathtub_clawfoot_")?BathroomGeometry.CLAWFOOT_TUB:BathroomGeometry.STANDARD_TUB),"Tub material changes geometry: "+path);
        }
        h.succeed();
    }

    private static void effects(GameTestHelper h,boolean claw,boolean compact) {
        var head=bathtub(h,claw,Direction.NORTH);var shower=shower(compact);
        var mount=shower.geometry().on(((BathtubBlock)h.getLevel().getBlockState(head).getBlock()).mountGeometry());
        var pos=head.above(mount.above());install(h,player(h),head,shower);
        var be=(ShowerHeadContainerEntity)h.getLevel().getBlockEntity(pos);
        be.setItem(0,new ItemStack(BlocksRegister.HOT_WATER_CORE.get()));be.toggleEffect();
        var origin=shower.sprayOrigin(pos,h.getLevel().getBlockState(pos));
        var inside=new net.minecraft.world.entity.decoration.ArmorStand(h.getLevel(),origin.x,head.getY()+1.05,origin.z);
        var outside=new net.minecraft.world.entity.decoration.ArmorStand(h.getLevel(),origin.x+2.5,head.getY()+1.05,origin.z);
        outside.setPos(origin.x,head.getY()+1.05,origin.z+shower.sprayDepth(h.getLevel().getBlockState(pos))/2+outside.getBbWidth()/2+.05);
        inside.setNoGravity(true);outside.setNoGravity(true);
        h.getLevel().addFreshEntity(inside);h.getLevel().addFreshEntity(outside);
        h.runAtTickTime(220,()->{
            try {
                h.assertTrue(inside.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),"Mounted shower stopped applying core effects");
                h.assertTrue(!outside.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),"Effects escaped the spray column");
                be.toggleEffect();inside.removeAllEffects();
            } catch(Throwable error) {inside.discard();outside.discard();throw error;}
        });
        h.runAtTickTime(440,()->{
            try {
                h.assertTrue(!inside.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),"Disabled shower still applied effects");
                h.succeed();
            } finally {inside.discard();outside.discard();}
        });
    }
    @GameTest(batch="shower_mount", template="light_empty",timeoutTicks=500) public static void effects_rain_standard(GameTestHelper h){effects(h,false,false);}
    @GameTest(batch="shower_mount", template="light_empty",timeoutTicks=500) public static void effects_rain_clawfoot(GameTestHelper h){effects(h,true,false);}
    @GameTest(batch="shower_mount", template="light_empty",timeoutTicks=500) public static void effects_compact_standard(GameTestHelper h){effects(h,false,true);}
    @GameTest(batch="shower_mount", template="light_empty",timeoutTicks=500) public static void effects_compact_clawfoot(GameTestHelper h){effects(h,true,true);}
}
