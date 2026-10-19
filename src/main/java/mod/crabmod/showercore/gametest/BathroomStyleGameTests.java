package mod.crabmod.showercore.gametest;
import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.block.*;
import mod.crabmod.showercore.entity.*;
import mod.crabmod.showercore.material.*;
import mod.crabmod.showercore.menu.BathtubMaterialMenu;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
@GameTestHolder(ShowerCore.MODID) @PrefixGameTestTemplate(false)
public final class BathroomStyleGameTests {
    private static Block block(String id){return BuiltInRegistries.BLOCK.get(new ResourceLocation("showercore:"+id));}
    private static final String[] TUBS={"bathtub_white","bathtub_clawfoot_white","bathtub_ofuro_birch","bathtub_roman_white","bathtub_seashell_pink","bathtub_washtub_iron","bathtub_spa_white"};
    private static final String[] HEADS={"rain_shower_head_white","compact_shower_head_white","bamboo_shower_head_bamboo","column_shower_head_white","coral_shower_head_pink","bucket_shower_head_spruce","panel_shower_head_white"};
    private static void shape(GameTestHelper h,int index) {
        var level=h.getLevel();var p=h.makeMockSurvivalPlayer();var foot=h.absolutePos(new BlockPos(5,1,5));
        var tub=(BathtubBlock)block(TUBS[index]);
        h.assertTrue(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(tub.defaultBlockState()),"Shape must be harvestable with an iron pickaxe");
        for(var facing:Direction.Plane.HORIZONTAL) {
            var head=foot.relative(facing);var state=tub.defaultBlockState().setValue(BathtubBlock.FACING,facing);
            level.setBlock(foot,state,3);tub.setPlacedBy(level,foot,state,null,ItemStack.EMPTY);
            h.assertTrue(BathtubBlock.waterLevelFor(state)==tub.mountGeometry().waterLevel(),"Water profile");
            h.assertTrue(BathtubBlock.getSeatYOffset(state)==tub.mountGeometry().seatOffset(),"Seat profile");
            for(var id:HEADS) {
                var shower=(ShowerHeadBlock)block(id);var mount=shower.geometry().on(tub.mountGeometry());var pos=head.above(mount.above());
                for(var clicked:java.util.List.of(head,foot)) for(var clickedFace:java.util.List.of(Direction.UP,Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)) {
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(shower,2));
                    var use=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked).relative(clickedFace,.5),clickedFace,clicked,false));
                    h.assertTrue(shower.asItem().useOn(use).consumesAction(),"Install "+id+" on "+TUBS[index]);
                    var installed=level.getBlockState(pos);
                    h.assertTrue(installed.is(shower)&&installed.getValue(ShowerHeadBlock.FACING)==facing.getOpposite()&&ShowerHeadBlock.mountOffsetPixels(installed)==mount.offsetPixels(),"Installed profile offset");
                    h.assertTrue(p.getMainHandItem().getCount()==1,"One installation cost");
                    var origin=shower.sprayOrigin(pos,installed);
                    var hit=level.clip(new ClipContext(origin.add(0,.25,0),origin.add(0,-.02,0),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
                    h.assertTrue(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos),"Native outlet targeting");
                    h.assertTrue(Math.abs(pos.getY()+ShowerHeadBlock.mountOffset(installed)+shower.geometry().mountY()/16.0-head.getY()-tub.mountGeometry().rimY()/16.0)<1e-9,"Rim seam");
                    level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                }
            }
            level.setBlock(foot,Blocks.AIR.defaultBlockState(),3);level.setBlock(head,Blocks.AIR.defaultBlockState(),3);
        }
        h.succeed();
    }
    @GameTest(template="light_empty") public static void standard_all_showers(GameTestHelper h){shape(h,0);}
    @GameTest(template="light_empty") public static void clawfoot_all_showers(GameTestHelper h){shape(h,1);}
    @GameTest(template="light_empty") public static void ofuro_all_showers(GameTestHelper h){shape(h,2);}
    @GameTest(template="light_empty") public static void roman_all_showers(GameTestHelper h){shape(h,3);}
    @GameTest(template="light_empty") public static void seashell_all_showers(GameTestHelper h){shape(h,4);}
    @GameTest(template="light_empty") public static void washtub_all_showers(GameTestHelper h){shape(h,5);}
    @GameTest(template="light_empty") public static void spa_all_showers(GameTestHelper h){shape(h,6);}
    @GameTest(template="light_empty") public static void menu_covers_every_shape_and_duck_skin(GameTestHelper h) {
        var p=h.makeMockSurvivalPlayer();
        var items=new java.util.ArrayList<Item>();
        for(String id:TUBS)items.add(block(id).asItem()); for(String id:HEADS)items.add(block(id).asItem());
        items.add(mod.crabmod.showercore.registers.ItemRegister.RUBBER_DUCK.get());items.add(mod.crabmod.showercore.registers.ItemRegister.RUBBER_DUCK_DELUXE.get());
        for(var item:items) {
            var menu=new BathtubMaterialMenu(1,p.getInventory());var input=new ItemStack(item,2);
            h.assertTrue(BathroomMaterials.blockId(input)==null,"Default shape or duck must not acquire an empty material id");
            menu.getSlot(0).set(input);int cost=BathroomMaterials.cost(input);
            menu.getSlot(1).set(new ItemStack(Blocks.OAK_LOG,cost));
            menu.clicked(2,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
            var output=menu.getCarried();h.assertTrue(output.is(item)&&output.getCount()==1,"Shared menu output");
            h.assertTrue(BathroomMaterials.blockId(output).toString().equals("minecraft:oak_log"),"Material id");
            h.assertTrue(menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(0).getItem().getCount()==1,"Material cost");
            menu.removed(p);
        }
        h.succeed();
    }
    @GameTest(template="light_empty") public static void duck_skin_material_pick_and_save_roundtrip(GameTestHelper h) {
        for(var item:java.util.List.of(mod.crabmod.showercore.registers.ItemRegister.RUBBER_DUCK.get(),mod.crabmod.showercore.registers.ItemRegister.RUBBER_DUCK_DELUXE.get())) {
            var stack=new ItemStack(item);BathroomMaterials.set(stack,new ResourceLocation("minecraft:oak_log"),BathtubMaterialFace.TOP);
            var duck=new RubberDuckEntity(h.getLevel(),0,0,0);duck.setAppearance(stack);
            var tag=new net.minecraft.nbt.CompoundTag();duck.save(tag);
            var restored=new RubberDuckEntity(h.getLevel(),0,0,0);restored.load(tag);
            h.assertTrue(ItemStack.isSameItemSameTags(stack,restored.getPickResult()),"Duck appearance lost in save/pick");
        }
        h.succeed();
    }
    @GameTest(template="light_empty") public static void shower_material_core_pick_and_drop_roundtrip(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,3,3));var shower=(ShowerHeadBlock)block(HEADS[2]);
        var clean=new ShowerHeadContainerEntity(pos,shower.defaultBlockState());
        clean.load(new net.minecraft.nbt.CompoundTag());
        h.assertTrue(clean.getMaterialBlockId()==null,"Default shower load must not store an empty material marker");
        var stack=new ItemStack(shower);BathroomMaterials.set(stack,new ResourceLocation("minecraft:oak_log"),BathtubMaterialFace.TOP);
        var support=pos.south();level.setBlock(support,Blocks.STONE.defaultBlockState(),3);
        var player=h.makeMockSurvivalPlayer();player.setItemInHand(InteractionHand.MAIN_HAND,stack.copy());
        var use=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(support).add(0,0,-.5),Direction.NORTH,support,false));
        h.assertTrue(shower.asItem().useOn(use).consumesAction()&&level.getBlockState(pos).is(shower),"Native shower item placement");
        var be=(ShowerHeadContainerEntity)level.getBlockEntity(pos);be.setItem(0,new ItemStack(block("hot_water_core")));be.toggleEffect();
        var picked=shower.getCloneItemStack(level,pos,level.getBlockState(pos));
        h.assertTrue(BathroomMaterials.blockId(picked).toString().equals("minecraft:oak_log")&&BathroomMaterials.face(picked)==BathtubMaterialFace.TOP,"Shower material pick");
        var drops=Block.getDrops(level.getBlockState(pos),level,pos,be);
        h.assertTrue(drops.size()==1&&BathroomMaterials.blockId(drops.get(0)).toString().equals("minecraft:oak_log")
                &&BathroomMaterials.face(drops.get(0))==BathtubMaterialFace.TOP,"Material lost in native loot path");
        var saved=be.saveWithoutMetadata();var restored=new ShowerHeadContainerEntity(pos,level.getBlockState(pos));restored.load(saved);
        h.assertTrue(!restored.isEmpty()&&restored.isEffectActive()&&restored.getMaterialBlockId().equals(be.getMaterialBlockId()),"Material edit lost core or switch");
        h.succeed();
    }
}
