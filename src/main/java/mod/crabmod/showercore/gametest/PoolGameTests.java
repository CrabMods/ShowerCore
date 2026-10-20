package mod.crabmod.showercore.gametest;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import mod.crabmod.showercore.block.*;
import mod.crabmod.showercore.entity.*;
import mod.crabmod.showercore.geometry.BathMountSurface;
import mod.crabmod.showercore.geometry.BathroomGeometry;
import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.menu.PoolConfigurationMenu;
import mod.crabmod.showercore.pool.*;
import mod.crabmod.showercore.registers.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("showercore") @PrefixGameTestTemplate(false)
public final class PoolGameTests {
    private static final PoolSpec RECT=new PoolSpec(PoolSpec.Shape.RECTANGLE,5,5,0);
    private static final PoolSpec ROUND=new PoolSpec(PoolSpec.Shape.ROUNDED,6,5,5);
    private static final PoolSpec CIRCLE=new PoolSpec(PoolSpec.Shape.CIRCLE,5,5,0);
    private static final ResourceLocation MATERIAL=new ResourceLocation("minecraft:quartz_block");
    private static PoolSettings settings(PoolSpec spec){return new PoolSettings(spec,MATERIAL,BathtubMaterialFace.SIDE,false);}
    private static BlockPos origin(GameTestHelper h){return h.absolutePos(new BlockPos(3,1,3));}
    private static Player mockPlayer(GameTestHelper h,boolean creative){
        return creative?h.makeMockPlayer():h.makeMockSurvivalPlayer();
    }
    private static Player player(GameTestHelper h,PoolSpec spec,boolean creative){
        var p=mockPlayer(h,creative);p.setUUID(UUID.randomUUID());
        p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ItemRegister.BATHING_POOL.get()));
        int amount=creative?0:PoolLayout.of(spec).materialCost()+3,slot=9;
        while(amount>0){int count=Math.min(64,amount);p.getInventory().setItem(slot++,new ItemStack(Blocks.QUARTZ_BLOCK,count));amount-=count;}
        return p;
    }
    private static PoolBlockEntity place(GameTestHelper h,PoolSpec spec,Player player){
        h.assertTrue(PoolStructure.place(h.getLevel(),origin(h),settings(spec),player),"Place valid pool");
        return (PoolBlockEntity)h.getLevel().getBlockEntity(origin(h).offset(PoolLayout.of(spec).controllerX(),0,0));
    }
    private static void click(GameTestHelper h,Player p,BlockPos pos){
        h.useBlock(pos.subtract(h.absolutePos(BlockPos.ZERO)),p,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    private static long blocks(GameTestHelper h,PoolSpec spec){return PoolLayout.of(spec).cells().stream().filter(c->h.getLevel().getBlockState(origin(h).offset(c.x(),0,c.z())).is(BlocksRegister.BATHING_POOL.get())).count();}
    private static int drops(GameTestHelper h){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(BlockPos.ZERO),h.absolutePos(new BlockPos(24,8,24))).inflate(1)).stream().mapToInt(e->e.getItem().is(Blocks.QUARTZ_BLOCK.asItem())?e.getItem().getCount():0).sum();}
    private static void built(GameTestHelper h,PoolSpec spec){
        var p=player(h,spec,false);var owner=place(h,spec,p);var layout=PoolLayout.of(spec);
        h.assertTrue(blocks(h,spec)==layout.cells().size(),"All declared cells placed");
        h.assertTrue(PoolStructure.available(p,Blocks.QUARTZ_BLOCK.asItem())==3,"Materials charged once");
        double centerX=origin(h).getX()+spec.width()/2.0,centerZ=origin(h).getZ()+spec.length()/2.0;
        var floor=h.getLevel().clip(new ClipContext(new Vec3(centerX,origin(h).getY()+.5,centerZ),new Vec3(centerX,origin(h).getY()-.2,centerZ),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        h.assertTrue(floor.getType()==HitResult.Type.BLOCK&&Math.abs(floor.getLocation().y-origin(h).getY()-PoolSpec.FLOOR_Y)<1e-6,"Native pool floor collision");
        for(var cell:layout.cells()){
            var part=(PoolBlockEntity)h.getLevel().getBlockEntity(origin(h).offset(cell.x(),0,cell.z()));
            h.assertTrue(part.matches(owner)&&part.controller()==owner,"Member resolves matching authority");
            h.assertTrue(part.fluidHandler()==owner.fluidHandler(),"One shared fluid handler");
        }
        h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_rectangle_construction(GameTestHelper h){built(h,RECT);}
    @GameTest(template="pool_empty") public static void pool_rounded_construction(GameTestHelper h){built(h,ROUND);}
    @GameTest(template="pool_empty") public static void pool_circle_construction(GameTestHelper h){built(h,CIRCLE);}
    @GameTest(template="pool_empty") public static void pool_maximum_size_construction(GameTestHelper h){built(h,new PoolSpec(PoolSpec.Shape.CIRCLE,16,16,0));}
    @GameTest(template="light_empty") public static void pool_mount_far_coordinates_keep_fractional_geometry(GameTestHelper h){
        for(int x:new int[]{-29_000_001,23_000_001})for(var facing:Direction.Plane.HORIZONTAL){
            var support=new BathMountSurface.Support(new Vec3(x+.125,74.75,-24_000_003+.875),facing);
            var placement=support.place(BathroomGeometry.CORAL_SHOWER);var box=placement.clearance(BathroomGeometry.CORAL_SHOWER);var outlet=placement.outlet(BathroomGeometry.CORAL_SHOWER);
            h.assertTrue(placement.pos().equals(BlockPos.containing(outlet))&&box.contains(outlet),"Far outlet/clearance mismatch");
            h.assertTrue(box.minX>x-1&&box.maxX<x+2,"Block coordinate was rounded to float");
        }
        h.succeed();
    }
    @GameTest(template="light_empty") public static void pool_mount_carrier_cell_is_below_the_rim(GameTestHelper h){
        var a=new BathMountSurface.Support(new Vec3(4.5,70.75,6),Direction.SOUTH);
        var b=new BathMountSurface.Support(new Vec3(4.5,70.75,7),Direction.NORTH);
        h.assertTrue(a.baseBlock().equals(new BlockPos(4,70,6))&&b.baseBlock().equals(new BlockPos(4,70,6)),"Wrong attachment cell");h.succeed();
    }

    @GameTest(template="pool_empty") public static void pool_obstruction_preserves_foreign_chest_and_inventory(GameTestHelper h){
        var p=player(h,RECT,false);var pos=origin(h).offset(2,0,2);
        h.getLevel().setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(pos);chest.setItem(0,new ItemStack(Items.DIAMOND,7));
        int before=PoolStructure.available(p,Blocks.QUARTZ_BLOCK.asItem());
        h.assertTrue(!PoolStructure.place(h.getLevel(),origin(h),settings(RECT),p),"Obstructed build accepted");
        h.assertTrue(blocks(h,RECT)==0&&chest.getItem(0).getCount()==7,"Foreign contents changed");
        h.assertTrue(PoolStructure.available(p,Blocks.QUARTZ_BLOCK.asItem())==before,"Failed build charged inventory");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_insufficient_materials_places_nothing(GameTestHelper h){
        var p=player(h,RECT,false);p.getInventory().clearContent();
        h.assertTrue(!PoolStructure.place(h.getLevel(),origin(h),settings(RECT),p)&&blocks(h,RECT)==0,"Partial unpaid structure");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_cancelled_multiplace_rolls_back(GameTestHelper h){
        var p=player(h,RECT,false);int before=PoolStructure.available(p,Blocks.QUARTZ_BLOCK.asItem());
        Consumer<BlockEvent.EntityMultiPlaceEvent> cancel=e->{if(e.getPlacedBlock().is(BlocksRegister.BATHING_POOL.get()))e.setCanceled(true);};
        MinecraftForge.EVENT_BUS.addListener(cancel);
        try{h.assertTrue(!PoolStructure.place(h.getLevel(),origin(h),settings(RECT),p),"Cancelled event accepted");}
        finally{MinecraftForge.EVENT_BUS.unregister(cancel);}
        h.assertTrue(blocks(h,RECT)==0&&PoolStructure.available(p,Blocks.QUARTZ_BLOCK.asItem())==before,"Cancellation leaked blocks or charges");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_event_replacement_is_not_overwritten_by_rollback(GameTestHelper h){
        var p=player(h,RECT,false);var target=origin(h).offset(2,0,2);
        Consumer<BlockEvent.EntityMultiPlaceEvent> mutate=e->{if(e.getPlacedBlock().is(BlocksRegister.BATHING_POOL.get()))h.getLevel().setBlock(target,Blocks.STONE.defaultBlockState(),3);};
        MinecraftForge.EVENT_BUS.addListener(mutate);
        try{h.assertTrue(!PoolStructure.place(h.getLevel(),origin(h),settings(RECT),p),"Mutated structure accepted");}
        finally{MinecraftForge.EVENT_BUS.unregister(mutate);}
        h.assertTrue(blocks(h,RECT)==0&&h.getLevel().getBlockState(target).is(Blocks.STONE),"Rollback overwrote a foreign replacement");
        h.assertTrue(PoolStructure.available(p,Blocks.QUARTZ_BLOCK.asItem())==PoolLayout.of(RECT).materialCost()+3,"Mutation charged resources");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_break_refunds_once_and_invalidates_cached_handler(GameTestHelper h){
        var p=player(h,RECT,false);var owner=place(h,RECT,p);var handler=owner.fluidHandler();
        h.assertTrue(handler.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==1000,"Native capability fill");
        var extra=origin(h).above(3);h.getLevel().setBlock(extra,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
        h.getLevel().destroyBlock(origin(h),true,p);
        h.assertTrue(blocks(h,RECT)==0&&drops(h)==PoolLayout.of(RECT).materialCost(),"Teardown/refund count");
        PoolStructure.dismantle(owner,true);
        h.assertTrue(drops(h)==PoolLayout.of(RECT).materialCost(),"Repeated refund");
        h.assertTrue(handler.drain(1000,IFluidHandler.FluidAction.EXECUTE).isEmpty()&&handler.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==0,"Stale capability remains active");
        h.assertTrue(h.getLevel().getBlockState(extra).is(Blocks.DIAMOND_BLOCK),"Teardown changed foreign upper blocks");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_creative_construction_has_no_paid_refund(GameTestHelper h){
        var p=player(h,RECT,true);place(h,RECT,p);h.getLevel().destroyBlock(origin(h),true,p);
        h.assertTrue(blocks(h,RECT)==0&&drops(h)==0,"Creative build produced paid materials");h.succeed();
    }

    private static ItemStack bucket(String fluid){
        if(fluid.equals("water"))return new ItemStack(Items.WATER_BUCKET);
        if(fluid.equals("hot_water"))return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("hotbath:hot_water_bucket")));
        return CustomFluidAPI.createBucket(new ResourceLocation("hotbath:"+fluid));
    }
    private static void fluids(GameTestHelper h,PoolSpec spec){
        var p=player(h,spec,false);var owner=place(h,spec,p);var part=origin(h).offset(2,0,2);
        for(String fluid:List.of("water","hot_water","golden_bath","silent_spring")){
            var input=bucket(fluid);p.setItemInHand(InteractionHand.MAIN_HAND,input.copy());click(h,p,part);
            h.assertTrue(owner.fluid().getAmount()==1000&&p.getMainHandItem().is(Items.BUCKET),"Native bucket fill "+fluid);
            if(fluid.equals("golden_bath")||fluid.equals("silent_spring"))h.assertTrue(new ResourceLocation("hotbath:"+fluid).equals(owner.getCustomFluidId()),"Custom identity lost");
            for(var cell:owner.layout().cells()){
                var member=(PoolBlockEntity)h.getLevel().getBlockEntity(origin(h).offset(cell.x(),0,cell.z()));
                h.assertTrue(member.fluid().getAmount()==1000&&member.waterY()==owner.waterY(),"Inconsistent water surface");
            }
            p.setItemInHand(InteractionHand.MAIN_HAND,input.copy());click(h,p,part);
            h.assertTrue(owner.fluid().getAmount()==2000,"Second bucket must add volume");
            for(int i=0;i<2;i++){
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));click(h,p,part);
                h.assertTrue(ItemStack.isSameItemSameTags(p.getMainHandItem(),input),"Drain identity "+fluid);
            }
            h.assertTrue(owner.fluid().isEmpty(),"Pool did not drain");
        }
        h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_rectangle_native_buckets(GameTestHelper h){fluids(h,RECT);}
    @GameTest(template="pool_empty") public static void pool_rounded_native_buckets(GameTestHelper h){fluids(h,ROUND);}
    @GameTest(template="pool_empty") public static void pool_circle_native_buckets(GameTestHelper h){fluids(h,CIRCLE);}
    @GameTest(template="pool_empty") public static void pool_custom_fluids_never_mix_or_lose_their_id(GameTestHelper h){
        var p=player(h,RECT,false);var owner=place(h,RECT,p);var handler=owner.fluidHandler();
        var a=CustomFluidAPI.createCustomFluidStack(new ResourceLocation("hotbath:golden_bath"),500);
        var b=CustomFluidAPI.createCustomFluidStack(new ResourceLocation("hotbath:blazing_bath"),500);
        h.assertTrue(handler.fill(a,IFluidHandler.FluidAction.EXECUTE)==500,"Custom capability fill");
        h.assertTrue(handler.fill(b,IFluidHandler.FluidAction.EXECUTE)==0,"Different custom identities mixed");
        var anonymous=new FluidStack(a.getFluid(),500);
        h.assertTrue(handler.fill(anonymous,IFluidHandler.FluidAction.EXECUTE)==0&&owner.fluid().getAmount()==500,"Anonymous custom fluid accepted");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_sync_and_save_keep_layout_material_and_fluid(GameTestHelper h){
        var p=player(h,ROUND,false);var owner=place(h,ROUND,p);
        owner.fluidHandler().fill(CustomFluidAPI.createCustomFluidStack(new ResourceLocation("hotbath:golden_bath"),2000),IFluidHandler.FluidAction.EXECUTE);
        owner.toggleFaucet();
        for(var cell:owner.layout().cells()){
            var pos=origin(h).offset(cell.x(),0,cell.z());var original=(PoolBlockEntity)h.getLevel().getBlockEntity(pos);
            var restored=new PoolBlockEntity(pos,h.getLevel().getBlockState(pos));restored.load(original.getUpdateTag());
            h.assertTrue(restored.matches(owner)&&restored.fluid().getAmount()==2000&&restored.circulating(),"Snapshot roundtrip");
            h.assertTrue(owner.getCustomFluidId().equals(restored.getCustomFluidId()),"Saved custom identity");
        }
        h.succeed();
    }

    private static void seating(GameTestHelper h,PoolLayout.SeatKind kind){
        var p=player(h,RECT,false);var owner=place(h,RECT,p);
        var seat=owner.layout().seats().stream().filter(s->s.kind()==kind).findFirst().orElseThrow();
        var hit=new Vec3(origin(h).getX()+seat.x(),origin(h).getY()+.2,origin(h).getZ()+seat.z());
        p.setPos(hit.add(0,1,0));
        h.assertTrue(PoolSeats.sit(owner,p,hit),"First seat claim");
        var second=h.makeMockSurvivalPlayer();second.setUUID(UUID.randomUUID());second.setPos(hit.add(0,1,0));
        h.assertTrue(!PoolSeats.sit(owner,second,hit),"Second player stole occupied seat");
        var occupied=(SeatEntity)p.getVehicle();var saved=new net.minecraft.nbt.CompoundTag();occupied.save(saved);
        var restored=new SeatEntity(h.getLevel(),0,0,0);restored.load(saved);
        h.assertTrue(owner.poolId().equals(restored.poolId())&&restored.poolSlot()==seat.index(),"Seat binding save");
        p.stopRiding();occupied.discard();h.assertTrue(PoolSeats.sit(owner,second,hit),"Seat was not released");
        PoolStructure.dismantle(owner,false);h.assertTrue(!second.isPassenger(),"Pool teardown left rider attached");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_inside_seat_claim_release_and_save(GameTestHelper h){seating(h,PoolLayout.SeatKind.INSIDE);}
    @GameTest(template="pool_empty") public static void pool_rim_seat_claim_release_and_save(GameTestHelper h){seating(h,PoolLayout.SeatKind.RIM);}
    @GameTest(template="pool_empty",timeoutTicks=400) public static void pool_effects_require_actual_water_contact(GameTestHelper h){
        var p=player(h,RECT,false);var owner=place(h,RECT,p);
        var hot=BuiltInRegistries.FLUID.get(new ResourceLocation("hotbath:hot_water_fluid"));
        owner.fluidHandler().fill(new FluidStack(hot,owner.layout().capacity()),IFluidHandler.FluidAction.EXECUTE);
        var wet=new net.minecraft.world.entity.animal.Pig(net.minecraft.world.entity.EntityType.PIG,h.getLevel());
        var dry=new net.minecraft.world.entity.animal.Pig(net.minecraft.world.entity.EntityType.PIG,h.getLevel());
        wet.setPos(origin(h).getX()+2.5,origin(h).getY()+PoolSpec.FLOOR_Y+.05,origin(h).getZ()+2.5);
        dry.setPos(origin(h).getX()+2.5,origin(h).getY()+1.2,origin(h).getZ()+2.5);
        wet.setNoAi(true);dry.setNoAi(true);wet.setNoGravity(true);dry.setNoGravity(true);
        h.getLevel().addFreshEntity(wet);h.getLevel().addFreshEntity(dry);
        h.runAtTickTime(335,()->{try{
            var observation=new java.util.LinkedHashMap<String,Object>();
            observation.put("wetPosition",wet.position().toString());observation.put("wetVelocity",wet.getDeltaMovement().toString());
            observation.put("wetBounds",wet.getBoundingBox().toString());observation.put("touchesActualWater",owner.touchesWater(wet.getBoundingBox()));
            observation.put("nativeInWater",wet.isInWater());observation.put("eligible",com.crabmod.hotbath.entity.FriendlyBathBenefits.isEligible(wet));
            observation.put("effects",wet.getActiveEffects().toString());observation.put("dryEffects",dry.getActiveEffects().toString());
            observation.put("worldTick",h.getLevel().getGameTime());observation.put("entityTick",wet.tickCount);
            observation.put("entries",com.crabmod.hotbath.bathing.BathImmersion.enterCount(wet,"hotbath:hot_water_fluid"));
            try {
                if(System.getProperty("case.evidence")!=null) {
                var file=java.nio.file.Path.of(System.getProperty("case.evidence"),"pool-stationary-contact.json");
                java.nio.file.Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(observation),java.nio.file.StandardOpenOption.CREATE_NEW);
                }
            }catch(java.io.IOException error){throw new RuntimeException(error);}
            h.assertTrue(wet.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),"Wet pool participant got no effect");
            h.assertTrue(!dry.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),"Dry participant got bath effect");h.succeed();
        }finally{wet.discard();dry.discard();}});
    }

    private static void mounts(GameTestHelper h,PoolSpec spec){
        var p=player(h,spec,false);var owner=place(h,spec,p);
        for(String id:List.of("rain_shower_head_white","compact_shower_head_white","bamboo_shower_head_bamboo","column_shower_head_white","coral_shower_head_pink","bucket_shower_head_spruce","panel_shower_head_white"))for(var facing:Direction.Plane.HORIZONTAL){
            var shower=(ShowerHeadBlock)BuiltInRegistries.BLOCK.get(new ResourceLocation("showercore:"+id));
            PoolLayout.RimPoint candidate=null;
            var outline=owner.layout().outline();
            for(int i=0;i<outline.size();i++)for(double fraction:new double[]{.25,.5,.75}){
                var point=outline.get(i).lerp(outline.get((i+1)%outline.size()),fraction);
                var rim=owner.layout().closestRim(point.x(),point.z());
                if(Direction.getNearest(-rim.normalX(),0,-rim.normalZ())==facing&&rim.point().distance(owner.layout().faucet())>.7){candidate=rim;break;}
            }
            h.assertTrue(candidate!=null,"Missing rim anchor "+facing);var point=candidate.point();
            int cx=Math.max(0,Math.min(spec.width()-1,(int)Math.floor(point.x()-.02*candidate.normalX())));
            int cz=Math.max(0,Math.min(spec.length()-1,(int)Math.floor(point.z()-.02*candidate.normalZ())));
            var clicked=origin(h).offset(cx,0,cz);var location=new Vec3(origin(h).getX()+point.x(),origin(h).getY()+.9,origin(h).getZ()+point.z());
            var hit=new BlockHitResult(location,facing.getOpposite(),clicked,false);
            var support=((BathMountSurface)BlocksRegister.BATHING_POOL.get()).showerSupport(h.getLevel(),clicked,hit,shower.geometry());
            h.assertTrue(support!=null,"Pool mount support");var placement=support.place(shower.geometry());
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(shower,2));
            var result=shower.asItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit));
            h.assertTrue(result.consumesAction()&&p.getMainHandItem().getCount()==1,"Native pool shower install "+id+" "+facing);
            var state=h.getLevel().getBlockState(placement.pos());
            h.assertTrue(state.getValue(ShowerHeadBlock.FACING)==facing,"Rim facing changed");
            var outlet=shower.sprayOrigin(h.getLevel(),placement.pos(),state);
            h.assertTrue(BlockPos.containing(outlet).equals(placement.pos()),"Outlet escaped its owning cell");
            h.assertTrue(spec.contains(outlet.x-origin(h).getX(),outlet.z-origin(h).getZ(),PoolSpec.RIM_WIDTH),"Spray center lies outside the pool water");
            var ray=h.getLevel().clip(new ClipContext(outlet.add(0,.2,0),outlet.add(0,-.02,0),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
            h.assertTrue(ray.getType()==HitResult.Type.BLOCK&&ray.getBlockPos().equals(placement.pos()),"Translated outlet not targetable");
            var be=(ShowerHeadContainerEntity)h.getLevel().getBlockEntity(placement.pos());
            var restored=new ShowerHeadContainerEntity(placement.pos(),state);restored.load(be.saveWithoutMetadata());
            h.assertTrue(restored.horizontalMount().equals(be.horizontalMount()),"Horizontal mount save");
            h.getLevel().setBlock(placement.pos(),Blocks.AIR.defaultBlockState(),3);
        }
        h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_rectangle_all_shower_mounts(GameTestHelper h){mounts(h,RECT);}
    @GameTest(template="pool_empty") public static void pool_rounded_all_shower_mounts(GameTestHelper h){mounts(h,ROUND);}
    @GameTest(template="pool_empty") public static void pool_circle_all_shower_mounts(GameTestHelper h){mounts(h,CIRCLE);}

    @GameTest(template="pool_empty") public static void pool_menu_keeps_native_inventory_and_never_charges_a_preview(GameTestHelper h){
        var p=player(h,RECT,false);var menu=new PoolConfigurationMenu(1,p.getInventory());
        menu.getSlot(0).set(new ItemStack(ItemRegister.BATHING_POOL.get()));menu.getSlot(1).set(new ItemStack(Blocks.OAK_LOG));
        h.assertTrue(menu.clickMenuButton(p,2)&&menu.spec().shape()==PoolSpec.Shape.CIRCLE,"Circle menu selection");
        h.assertTrue(menu.clickMenuButton(p,11)&&menu.spec().width()==menu.spec().length(),"Circle diameter");
        h.assertTrue(menu.clickMenuButton(p,21)&&menu.face()==BathtubMaterialFace.TOP,"Top material face");
        h.assertTrue(!menu.clickMenuButton(p,9999),"Invalid menu packet accepted");
        var result=menu.getSlot(0).getItem().copy();h.assertTrue(new ResourceLocation("minecraft:oak_log").equals(PoolSettings.read(result).material()),"Sample material not saved");
        h.assertTrue(menu.getSlot(1).getItem().is(Blocks.OAK_LOG.asItem())&&menu.getSlot(1).getItem().getCount()==1,"Preview consumed sample");
        // Vanilla clearContainer only returns inventory items for a real ServerPlayer.
        // The actual-client lane verifies closing and returning both native input slots.
        menu.removed(p);h.assertTrue(!menu.clickMenuButton(p,1),"Closed menu accepted action");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_room_selection_requires_confirmation_before_charging(GameTestHelper h){
        var p=player(h,RECT,false);var tool=p.getMainHandItem();PoolSettings.write(tool,new PoolSettings(RECT,MATERIAL,BathtubMaterialFace.SIDE,true));
        var first=origin(h).below();var second=first.offset(4,0,4);h.getLevel().setBlock(first,Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(second,Blocks.STONE.defaultBlockState(),3);
        var item=tool.getItem();
        var a=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(first),Direction.UP,first,false));
        var b=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(second),Direction.UP,second,false));
        item.useOn(a);item.useOn(b);
        h.assertTrue(blocks(h,RECT)==0&&PoolSettings.data(tool).contains("PoolReady"),"Selection built before confirmation");
        h.assertTrue(PoolStructure.available(p,Blocks.QUARTZ_BLOCK.asItem())==PoolLayout.of(RECT).materialCost()+3,"Selection charged inventory");
        item.useOn(b);h.assertTrue(blocks(h,RECT)==PoolLayout.of(RECT).cells().size(),"Confirmed room build failed");h.succeed();
    }
    @GameTest(template="pool_empty",timeoutTicks=80) public static void pool_ducks_float_inside_the_circular_water(GameTestHelper h){
        var p=player(h,CIRCLE,false);var owner=place(h,CIRCLE,p);owner.fluidHandler().fill(new FluidStack(Fluids.WATER,owner.layout().capacity()),IFluidHandler.FluidAction.EXECUTE);
        var pos=origin(h).offset(2,0,0);var ducks=new ArrayList<RubberDuckEntity>();
        for(var item:List.of(ItemRegister.RUBBER_DUCK.get(),ItemRegister.RUBBER_DUCK_DELUXE.get())){
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));click(h,p,pos);
            var added=h.getLevel().getEntitiesOfClass(RubberDuckEntity.class,new AABB(h.absolutePos(BlockPos.ZERO),h.absolutePos(new BlockPos(24,8,24)))).stream().filter(d->d.appearance().is(item)).toList();
            h.assertTrue(added.size()==1,"Native pool duck placement");ducks.add(added.get(0));
        }
        h.runAtTickTime(40,()->{try{
            for(var duck:ducks){h.assertTrue(!duck.isRemoved()&&CIRCLE.contains(duck.getX()-origin(h).getX(),duck.getZ()-origin(h).getZ(),.49),"Duck crossed circular rim");
                h.assertTrue(duck.getY()>origin(h).getY()+.65&&duck.getY()<origin(h).getY()+1.1,"Duck lost water surface");}
            h.succeed();
        }finally{ducks.forEach(Entity::discard);}});
    }
    @GameTest(template="pool_empty") public static void pool_refinish_preserves_fluid_and_refunds_old_material_once(GameTestHelper h){
        var p=player(h,RECT,false);var owner=place(h,RECT,p);var handler=owner.fluidHandler();var uuid=owner.poolId();
        handler.fill(CustomFluidAPI.createCustomFluidStack(new ResourceLocation("hotbath:golden_bath"),2000),IFluidHandler.FluidAction.EXECUTE);
        int cost=owner.layout().materialCost();p.getInventory().setItem(10,new ItemStack(Blocks.OAK_LOG,cost+2));var sample=new ItemStack(Blocks.OAK_LOG);
        var finish=new PoolSettings(RECT,new ResourceLocation("minecraft:oak_log"),BathtubMaterialFace.TOP,false);
        h.assertTrue(PoolStructure.refinish(owner,finish,p,sample),"Change whole pool material");
        h.assertTrue(owner.poolId().equals(uuid)&&owner.fluidHandler()==handler&&owner.fluid().getAmount()==2000,"Refinish replaced structure or fluid");
        h.assertTrue(PoolStructure.available(p,Blocks.QUARTZ_BLOCK.asItem())==cost+3,"Original material refund");
        h.assertTrue(PoolStructure.available(p,Blocks.OAK_LOG.asItem())==3&&sample.isEmpty(),"New material charge");
        for(var cell:owner.layout().cells())h.assertTrue(((PoolBlockEntity)h.getLevel().getBlockEntity(origin(h).offset(cell.x(),0,cell.z()))).settings().material().equals(finish.material()),"Only part of pool changed");
        var side=new PoolSettings(RECT,finish.material(),BathtubMaterialFace.SIDE,false);
        h.assertTrue(PoolStructure.refinish(owner,side,p,ItemStack.EMPTY)&&PoolStructure.available(p,Blocks.OAK_LOG.asItem())==3,"Same material face switch charged again");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_unaffordable_refinish_changes_no_state(GameTestHelper h){
        var p=player(h,RECT,false);var owner=place(h,RECT,p);var before=owner.settings();var sample=new ItemStack(Blocks.OAK_LOG);
        h.assertTrue(!PoolStructure.refinish(owner,new PoolSettings(RECT,new ResourceLocation("minecraft:oak_log"),BathtubMaterialFace.TOP,false),p,sample),"Unpaid material change accepted");
        h.assertTrue(owner.settings().equals(before)&&sample.getCount()==1&&PoolStructure.available(p,Blocks.QUARTZ_BLOCK.asItem())==3,"Failed change charged or refunded materials");h.succeed();
    }

    @GameTest(template="pool_empty") public static void pool_external_fluid_capability_uses_controller_and_retires(GameTestHelper h){
        var p=player(h,RECT,false);var owner=place(h,RECT,p);var member=origin(h).offset(2,0,2);
        var exposed=h.getLevel().getBlockEntity(member).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER);
        var handler=exposed.resolve().orElseThrow();
        h.assertTrue(handler.fill(new FluidStack(Fluids.WATER,1234),IFluidHandler.FluidAction.EXECUTE)==1234,"Native block capability fill");
        h.assertTrue(owner.fluid().getAmount()==1234,"Capability did not address the shared tank");
        PoolStructure.dismantle(owner,true);
        h.assertTrue(!exposed.isPresent(),"Removed member kept its cached capability valid");
        h.assertTrue(handler.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==0&&handler.drain(1000,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"External cached tank remains writable after removal");h.succeed();
    }

    private static void externalFlow(GameTestHelper h, PoolSpec spec, Block source) {
        var p=player(h,spec,false);var owner=place(h,spec,p);var layout=owner.layout();var id=owner.poolId();
        BlockPos.betweenClosedStream(origin(h).offset(-1,-1,-1),origin(h).offset(spec.width(),-1,spec.length()))
                .forEach(pos->h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3));
        h.assertTrue(owner.fluidHandler().fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==1000,"Initial pool water");
        h.getLevel().setBlock(origin(h).offset(2,2,-1),source.defaultBlockState(),3);
        h.runAtTickTime(100,()->{
            h.assertTrue(!h.getLevel().getFluidState(origin(h).offset(2,0,-1)).isEmpty(),"External liquid did not reach the pool wall");
            h.assertTrue(blocks(h,spec)==layout.cells().size(),"External liquid displaced a structural member");
            h.assertTrue(owner.active()&&owner.poolId().equals(id)&&owner.fluid().getAmount()==1000,"External liquid changed the shared tank or owner");
            h.assertTrue(drops(h)==0,"External liquid dismantled or refunded the pool");
            h.getLevel().destroyBlock(origin(h).offset(2,0,2),true,p);
            h.assertTrue(blocks(h,spec)==0&&drops(h)==layout.materialCost(),"Player mining must still dismantle and refund once");
            h.succeed();
        });
    }
    @GameTest(template="pool_empty",timeoutTicks=130) public static void pool_rectangle_resists_water_flow(GameTestHelper h){externalFlow(h,RECT,Blocks.WATER);}
    @GameTest(template="pool_empty",timeoutTicks=130) public static void pool_rounded_resists_water_flow(GameTestHelper h){externalFlow(h,ROUND,Blocks.WATER);}
    @GameTest(template="pool_empty",timeoutTicks=130) public static void pool_circle_resists_water_flow(GameTestHelper h){externalFlow(h,CIRCLE,Blocks.WATER);}
    @GameTest(template="pool_empty",timeoutTicks=130) public static void pool_rectangle_resists_lava_flow(GameTestHelper h){externalFlow(h,RECT,Blocks.LAVA);}
    @GameTest(template="pool_empty",timeoutTicks=130) public static void pool_rounded_resists_lava_flow(GameTestHelper h){externalFlow(h,ROUND,Blocks.LAVA);}
    @GameTest(template="pool_empty",timeoutTicks=130) public static void pool_circle_resists_lava_flow(GameTestHelper h){externalFlow(h,CIRCLE,Blocks.LAVA);}


    private static void moveToWater(Player p,BlockPos o,double x,double z) {
        p.setPos(o.getX()+x,o.getY()+PoolSpec.FLOOR_Y,o.getZ()+z);
        p.setNoGravity(true);p.setOnGround(true);p.setDeltaMovement(Vec3.ZERO);p.setSpeed(.1F);
        p.baseTick();
    }
    private static void nativeWater(GameTestHelper h,PoolSpec spec) {
        var p=player(h,spec,false);var pool=place(h,spec,p);
        pool.fluidHandler().fill(new FluidStack(Fluids.WATER,pool.layout().capacity()),IFluidHandler.FluidAction.EXECUTE);
        moveToWater(p,origin(h),spec.width()/2.0,spec.length()/2.0);
        h.assertTrue(p.isInWater(),"Filled pool must participate in native water detection");
        double depth=pool.waterY()-PoolSpec.FLOOR_Y-.001;
        h.assertTrue(Math.abs(p.getFluidHeight(net.minecraft.tags.FluidTags.WATER)-depth)<1e-6,"Native fluid depth must follow the rendered level");
        h.assertTrue(h.getLevel().getFluidState(p.blockPosition()).isEmpty(),"Tank water must not become a flowing world fluid");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_rectangle_native_water_contact(GameTestHelper h){nativeWater(h,RECT);}
    @GameTest(template="pool_empty") public static void pool_rounded_native_water_contact(GameTestHelper h){nativeWater(h,ROUND);}
    @GameTest(template="pool_empty") public static void pool_circle_native_water_contact(GameTestHelper h){nativeWater(h,CIRCLE);}

    @GameTest(template="pool_empty") public static void pool_native_contact_clears_on_drain_and_exit(GameTestHelper h) {
        var p=player(h,RECT,false);var pool=place(h,RECT,p);moveToWater(p,origin(h),2.5,2.5);
        h.assertTrue(!p.isInWater(),"Empty basin must be dry");
        pool.fluidHandler().fill(new FluidStack(Fluids.WATER,pool.layout().capacity()/4),IFluidHandler.FluidAction.EXECUTE);
        p.baseTick();
        h.assertTrue(p.isInWater()&&Math.abs(p.getFluidHeight(net.minecraft.tags.FluidTags.WATER)-(pool.waterY()-PoolSpec.FLOOR_Y-.001))<1e-6,"Partial tank height");
        pool.fluidHandler().drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.EXECUTE);p.baseTick();
        h.assertTrue(!p.isInWater()&&p.getFluidHeight(net.minecraft.tags.FluidTags.WATER)==0,"Drained pool retained fluid contact");
        pool.fluidHandler().fill(new FluidStack(Fluids.WATER,pool.layout().capacity()),IFluidHandler.FluidAction.EXECUTE);p.baseTick();
        h.assertTrue(p.isInWater(),"Refill must restore native contact");moveToWater(p,origin(h),9,9);
        h.assertTrue(!p.isInWater()&&p.getFluidHeight(net.minecraft.tags.FluidTags.WATER)==0,"Leaving the pool retained fluid contact");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_native_contact_excludes_dry_rim_and_curved_corners(GameTestHelper h) {
        var p=player(h,CIRCLE,false);var pool=place(h,CIRCLE,p);
        pool.fluidHandler().fill(new FluidStack(Fluids.WATER,pool.layout().capacity()),IFluidHandler.FluidAction.EXECUTE);
        moveToWater(p,origin(h),.3,.3);h.assertTrue(!p.isInWater(),"Outside circle corner must remain dry");
        p.setPos(origin(h).getX()+.125,origin(h).getY()+PoolSpec.RIM_Y,origin(h).getZ()+2.5);p.baseTick();
        h.assertTrue(!p.isInWater(),"Standing on the dry rim must not slow the player");h.succeed();
    }
    private static double nativeStep(Player p,BlockPos o,double x,double z) {
        moveToWater(p,o,x,z);double before=p.getX();p.travel(new Vec3(1,0,0));return p.getX()-before;
    }
    @GameTest(template="pool_empty") public static void pool_water_movement_matches_native_world_water(GameTestHelper h) {
        var p=player(h,RECT,false);var pool=place(h,RECT,p);
        double dry=nativeStep(p,origin(h),2.5,2.5);
        pool.fluidHandler().fill(new FluidStack(Fluids.WATER,pool.layout().capacity()),IFluidHandler.FluidAction.EXECUTE);
        double wet=nativeStep(p,origin(h),2.5,2.5);
        var control=origin(h).offset(10,0,0);h.getLevel().setBlock(control,Blocks.WATER.defaultBlockState(),3);
        double vanilla=nativeStep(p,control,.5,.5);
        h.assertTrue(p.isInWater()&&vanilla>0,"Native world-water movement control");
        h.assertTrue(Math.abs(wet-vanilla)<1e-6&&wet<dry*.5,"Pool movement must use native water resistance: dry="+dry+", pool="+wet+", world="+vanilla);h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_crossing_member_boundaries_does_not_stack_resistance(GameTestHelper h) {
        var p=player(h,RECT,false);var pool=place(h,RECT,p);
        pool.fluidHandler().fill(new FluidStack(Fluids.WATER,pool.layout().capacity()),IFluidHandler.FluidAction.EXECUTE);
        double single=nativeStep(p,origin(h),2.5,2.5),four=nativeStep(p,origin(h),2,2);
        h.assertTrue(p.isInWater(),"Native water contact at a member boundary");
        h.assertTrue(single>0&&Math.abs(single-four)<1e-6,"Resistance stacked per structural member");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_liquid_contact_preserves_mod_and_lava_fluid_types(GameTestHelper h) {
        var p=player(h,RECT,false);var pool=place(h,RECT,p);
        var custom=CustomFluidAPI.createCustomFluidStack(new ResourceLocation("hotbath:golden_bath"),pool.layout().capacity());
        var hot=net.minecraftforge.fluids.FluidUtil.getFluidContained(bucket("hot_water")).orElseThrow();hot.setAmount(pool.layout().capacity());
        for(var fluid:List.of(new FluidStack(Fluids.WATER,pool.layout().capacity()),hot,custom,new FluidStack(Fluids.LAVA,pool.layout().capacity()))) {
            pool.fluidHandler().drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.EXECUTE);pool.fluidHandler().fill(fluid,IFluidHandler.FluidAction.EXECUTE);
            moveToWater(p,origin(h),2.5,2.5);var type=fluid.getFluid().getFluidType();
            h.assertTrue(p.getFluidTypeHeight(type)>0&&p.getMaxHeightFluidType()==type,"Tank liquid lost its registered native fluid type");
            if(type!=Fluids.WATER.getFluidType())h.assertTrue(p.getFluidTypeHeight(Fluids.WATER.getFluidType())==0,"Non-water fluid was relabeled as the vanilla water type");
            if(fluid.getFluid()==Fluids.LAVA)h.assertTrue(p.isInLava()&&!p.isInWater(),"Lava must retain native lava behavior");
        }h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_mod_liquid_movement_matches_native_world_fluid(GameTestHelper h) {
        var p=player(h,RECT,false);var pool=place(h,RECT,p);
        var fluid=CustomFluidAPI.createCustomFluidStack(new ResourceLocation("hotbath:golden_bath"),pool.layout().capacity());
        pool.fluidHandler().fill(fluid,IFluidHandler.FluidAction.EXECUTE);double wet=nativeStep(p,origin(h),2.5,2.5);
        var control=origin(h).offset(10,0,0);h.getLevel().setBlock(control,fluid.getFluid().defaultFluidState().createLegacyBlock(),3);
        double nativeFluid=nativeStep(p,control,.5,.5);
        h.assertTrue(p.getFluidTypeHeight(fluid.getFluid().getFluidType())>0&&nativeFluid>0,"Mod-fluid world control");
        h.assertTrue(Math.abs(wet-nativeFluid)<1e-6,"Pool bypassed the registered fluid movement behavior");h.succeed();
    }
    @GameTest(template="pool_empty") public static void pool_swimming_eye_contact_follows_actual_waterline(GameTestHelper h) {
        var p=player(h,RECT,false);var pool=place(h,RECT,p);
        p.setPose(net.minecraft.world.entity.Pose.SWIMMING);p.refreshDimensions();
        pool.fluidHandler().fill(new FluidStack(Fluids.WATER,pool.layout().capacity()),IFluidHandler.FluidAction.EXECUTE);moveToWater(p,origin(h),2.5,2.5);
        h.assertTrue(p.getEyeY()<origin(h).getY()+pool.waterY()&&p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER),"Submerged eyes must detect the actual tank liquid");
        pool.fluidHandler().drain(pool.layout().capacity()*3/4,IFluidHandler.FluidAction.EXECUTE);p.baseTick();
        h.assertTrue(!p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER),"Eyes above a shallow waterline stayed submerged");h.succeed();
    }


    private static final class LavaContactPig extends net.minecraft.world.entity.animal.Pig {
        int lavaCalls;
        LavaContactPig(net.minecraft.server.level.ServerLevel level) {
            super(net.minecraft.world.entity.EntityType.PIG,level);
        }
        @Override public void lavaHurt() {
            lavaCalls++;
            super.lavaHurt();
        }
    }

    @GameTest(template="pool_empty")
    public static void pool_lava_damage_dispatch_matches_native_world_lava(GameTestHelper h) {
        var owner=player(h,RECT,false);
        var pool=place(h,RECT,owner);
        var pooled=new LavaContactPig(h.getLevel());
        var world=new LavaContactPig(h.getLevel());
        var control=origin(h).offset(10,0,0);
        try {
            var lava=new FluidStack(Fluids.LAVA,pool.layout().capacity());
            h.assertTrue(pool.fluidHandler().fill(lava,IFluidHandler.FluidAction.EXECUTE)==lava.getAmount(),"Native pool lava fill");
            pooled.setPos(origin(h).getX()+2.5,origin(h).getY()+2,origin(h).getZ()+2.5);
            world.setPos(control.getX()+.5,control.getY()+2,control.getZ()+.5);
            pooled.baseTick();world.baseTick();
            h.assertTrue(pooled.lavaCalls==0&&world.lavaCalls==0,"Dry controls received lava contact");
            h.getLevel().setBlock(control,Blocks.LAVA.defaultBlockState(),3);
            pooled.setPos(origin(h).getX()+2.5,origin(h).getY()+.3,origin(h).getZ()+2.5);
            world.setPos(control.getX()+.5,control.getY()+.3,control.getZ()+.5);
            pooled.baseTick();world.baseTick();
            if(System.getProperty("case.evidence")!=null) {
                try {
                    var evidence=java.nio.file.Path.of(System.getProperty("case.evidence"),"pool-lava-dispatch.json");
                    java.nio.file.Files.writeString(evidence,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(
                            java.util.Map.of("poolLavaCalls",pooled.lavaCalls,"worldLavaCalls",world.lavaCalls,
                                    "poolHealth",pooled.getHealth(),"worldHealth",world.getHealth(),
                                    "poolNativeLava",pooled.isInLava(),"worldNativeLava",world.isInLava())),
                            java.nio.file.StandardOpenOption.CREATE_NEW);
                }catch(java.io.IOException error){throw new RuntimeException(error);}
            }
            h.assertTrue(pooled.isInLava()&&world.isInLava(),"Matched native lava contact controls");
            h.assertTrue(world.lavaCalls==1,"Native world lava must dispatch once per base tick");
            h.assertTrue(pooled.lavaCalls==world.lavaCalls,"Pool dispatched lava damage more often than native world lava: pool="+pooled.lavaCalls+", world="+world.lavaCalls);
        } finally {
            pooled.discard();world.discard();PoolStructure.dismantle(pool,false);
            h.getLevel().setBlock(control,Blocks.AIR.defaultBlockState(),3);
        }
        h.succeed();
    }
}
