package mod.crabmod.showercore.gametest;

import com.crabmod.hotbath.api.HotBathApi;
import com.crabmod.hotbath.custom_fluid.*;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.pool.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("showercore") @PrefixGameTestTemplate(false)
public final class BathDatapackGameTests {
    private static final ResourceLocation ID=new ResourceLocation("hotbath:contract_bath");
    private static final List<String> STYLES=List.of("world","bathtub_white","bathtub_clawfoot_white","bathtub_ofuro_birch","bathtub_seashell_pink","rectangle","rounded","circle");
    private record Fixture(BlockPos pos,PoolBlockEntity pool,List<BlockPos> blocks) {
        void close(GameTestHelper h) {
            if(pool!=null)PoolStructure.dismantle(pool,false);
            for(var p:blocks)h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);
        }
    }
    private static net.minecraft.world.entity.player.Player creative(GameTestHelper h) {
        var player=h.makeMockPlayer();player.getAbilities().instabuild=true;return player;
    }
    private static Fixture fixture(GameTestHelper h,String style) {
        return fixture(h,style,CustomFluidAPI.createCustomFluidStack(ID,1000));
    }
    private static Fixture fixture(GameTestHelper h,String style,FluidStack fluid) {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(3,1,3));
        if(style.equals("world")) {
            level.setBlock(origin,fluid.getFluid().defaultFluidState().createLegacyBlock(),3);
            if(CustomFluidAPI.isDynamicCustomFluid(fluid))((CustomFluidBlockEntity)level.getBlockEntity(origin)).setFluidId(ID);
            var shell=List.of(origin,origin.below(),origin.north(),origin.south(),origin.east(),origin.west());
            for(var pos:shell)if(!pos.equals(origin))level.setBlock(pos,Blocks.QUARTZ_BLOCK.defaultBlockState(),3);
            return new Fixture(origin,null,shell);
        }
        if(style.startsWith("bathtub")) {
            var block=BuiltInRegistries.BLOCK.get(new ResourceLocation("showercore:"+style));
            var liquid=BuiltinBathDefinitions.forFluid(fluid.getFluid()).map(d->java.util.Arrays.stream(BathtubBlock.LiquidType.values()).filter(t->(t.getSerializedName()+"_fluid").equals(d.id().getPath())).findFirst().orElseThrow()).orElse(BathtubBlock.LiquidType.CUSTOM);
            var state=block.defaultBlockState().setValue(BathtubBlock.FACING,Direction.NORTH).setValue(BathtubBlock.LIQUID,liquid);
            for(var part:List.of(BedPart.FOOT,BedPart.HEAD)) {
                var p=part==BedPart.FOOT?origin:origin.north();level.setBlock(p,state.setValue(BathtubBlock.PART,part),3);
                var tub=(BathtubBlockEntity)level.getBlockEntity(p);tub.getFluidTank().setFluid(fluid.copy());tub.setCustomFluidId(CustomFluidAPI.isDynamicCustomFluid(fluid)?ID:null);
            }
            return new Fixture(origin,null,List.of(origin,origin.north()));
        }
        var shape=PoolSpec.Shape.valueOf(style.toUpperCase(Locale.ROOT));
        var spec=new PoolSpec(shape,5,5,shape==PoolSpec.Shape.ROUNDED?4:0);
        BlockPos.betweenClosedStream(origin.offset(-1,-1,-1),origin.offset(6,-1,6)).forEach(p->level.setBlock(p,Blocks.STONE.defaultBlockState(),3));
        h.assertTrue(PoolStructure.place(level,origin,new PoolSettings(spec,new ResourceLocation("minecraft:quartz_block"),BathtubMaterialFace.SIDE,false),creative(h)),"Native pool placement");
        var owner=(PoolBlockEntity)level.getBlockEntity(origin.offset(PoolLayout.of(spec).controllerX(),0,0));
        fluid.setAmount(owner.layout().capacity());h.assertTrue(owner.fluidHandler().fill(fluid,IFluidHandler.FluidAction.EXECUTE)==fluid.getAmount(),"Native full pool fill");
        return new Fixture(origin.offset(2,0,2),owner,List.of());
    }

    @GameTest(template="pool_empty")
    public static void native_properties_follow_definition_in_all_container_styles(GameTestHelper h) {
        var definition=CustomFluidAPI.getFluidDefinition(ID).orElseThrow();
        var stack=CustomFluidAPI.createCustomFluidStack(ID,1000);var type=stack.getFluid().getFluidType();
        h.assertTrue(type.getDensity(stack)==definition.density()&&type.getViscosity(stack)==definition.viscosity(),"Stack density/viscosity differ from data");
        h.assertTrue(type.getLightLevel(stack)==definition.luminosity()&&type.getTemperature(stack)==(int)(definition.temperature()+273),"Stack light/temperature differ from data");
        for(String style:STYLES) {
            var f=fixture(h,style);
            try {
                var state=stack.getFluid().defaultFluidState();
                var properties=HotBathApi.fluidProperties(stack,false);
                if(style.equals("world")) {
                    h.assertTrue(type.getDensity(state,h.getLevel(),f.pos())==definition.density()&&type.getViscosity(state,h.getLevel(),f.pos())==definition.viscosity(),"Native world physical properties");
                    h.assertTrue(type.getTemperature(state,h.getLevel(),f.pos())==(int)(definition.temperature()+273)&&type.getLightLevel(state,h.getLevel(),f.pos())==definition.luminosity(),"Native world heat/light properties");
                }
                h.assertTrue(properties.density()==definition.density()&&properties.viscosity()==definition.viscosity(),style+" stack physical properties");
                h.assertTrue(properties.temperatureCelsius()==definition.temperature()&&properties.luminosity()==definition.luminosity(),style+" stack heat/light properties");
                if(h.getLevel().getBlockEntity(f.pos()) instanceof BathFluidSource container) {
                    h.assertTrue(container.getCustomFluidDefinition().orElseThrow().equals(definition),style+" complete definition identity");
                    var copy=CustomFluidAPI.createBucket(container.getCustomFluidDefinition().orElseThrow());
                    var restored=CustomFluidAPI.getFluidFromItem(copy).orElseThrow();
                    h.assertTrue(restored.equals(definition)&&restored.nutrition()==7&&restored.thirst()==9,style+" bucket roundtrip preserves drinking properties");
                }
            } finally {f.close(h);}
        }
        h.succeed();
    }

    private static void delayedEffects(GameTestHelper h,String style) {
        var f=fixture(h,style);var pig=new Pig(EntityType.PIG,h.getLevel());
        pig.setPos(f.pos().getX()+.5,f.pos().getY()+.3,f.pos().getZ()+.5);pig.tickCount=100;
        var count=new java.util.concurrent.atomic.AtomicInteger();
        h.onEachTick(()->{
            int tick=count.incrementAndGet();pig.tickCount++;
            if(tick<=48||tick>=71) {
                var state=h.getLevel().getBlockState(f.pos());state.entityInside(h.getLevel(),f.pos(),pig);
            }
        });
        h.runAtTickTime(25,()->h.assertTrue(!pig.hasEffect(MobEffects.MOVEMENT_SPEED),style+" effect fired before configured delay"));
        h.runAtTickTime(49,()->{
            var effect=pig.getEffect(MobEffects.MOVEMENT_SPEED);
            h.assertTrue(effect!=null&&effect.getAmplifier()==2&&effect.getDuration()==80&&effect.isAmbient()&&!effect.isVisible()&&!effect.showIcon(),style+" effect payload differs from data");
            pig.removeAllEffects();
        });
        h.runAtTickTime(90,()->h.assertTrue(!pig.hasEffect(MobEffects.MOVEMENT_SPEED),style+" re-entry bypassed configured delay"));
        h.runAtTickTime(118,()->{h.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SPEED),style+" effect missing after re-entry delay");f.close(h);pig.discard();h.succeed();});
    }
    @GameTest(template="pool_empty",timeoutTicks=150) public static void world_custom_effect_delay_and_reentry(GameTestHelper h){delayedEffects(h,"world");}
    @GameTest(template="pool_empty",timeoutTicks=150) public static void standard_custom_effect_delay_and_reentry(GameTestHelper h){delayedEffects(h,"bathtub_white");}
    @GameTest(template="pool_empty",timeoutTicks=150) public static void clawfoot_custom_effect_delay_and_reentry(GameTestHelper h){delayedEffects(h,"bathtub_clawfoot_white");}
    @GameTest(template="pool_empty",timeoutTicks=150) public static void ofuro_custom_effect_delay_and_reentry(GameTestHelper h){delayedEffects(h,"bathtub_ofuro_birch");}
    @GameTest(template="pool_empty",timeoutTicks=150) public static void seashell_custom_effect_delay_and_reentry(GameTestHelper h){delayedEffects(h,"bathtub_seashell_pink");}
    @GameTest(template="pool_empty",timeoutTicks=150) public static void rectangle_custom_effect_delay_and_reentry(GameTestHelper h){delayedEffects(h,"rectangle");}
    @GameTest(template="pool_empty",timeoutTicks=150) public static void rounded_custom_effect_delay_and_reentry(GameTestHelper h){delayedEffects(h,"rounded");}
    @GameTest(template="pool_empty",timeoutTicks=150) public static void circle_custom_effect_delay_and_reentry(GameTestHelper h){delayedEffects(h,"circle");}

    @GameTest(template="pool_empty",batch="bath-profile-contract",timeoutTicks=500)
    public static void builtin_bath_profiles_execute_in_every_container(GameTestHelper h) {
        var originals=BuiltinBathDefinitions.serverJsons();
        class Run {
            Fixture fixture;Pig pig;boolean finished;
            void close() {if(pig!=null)pig.discard();if(fixture!=null)fixture.close(h);pig=null;fixture=null;}
            void restore() {
                var original=new HashMap<ResourceLocation,BuiltinBathDefinition>();
                for(String text:originals){var def=BuiltinBathDefinition.parse(com.google.gson.JsonParser.parseString(text));original.put(def.id(),def);}
                BuiltinBathDefinitions.replace(BuiltinBathDefinitions.catalog(original,originals));
            }
            void guard(Runnable action) {
                if(finished)return;
                try {action.run();}catch(Throwable error) {finished=true;close();restore();h.fail(error.toString());}
            }
        }
        var run=new Run();
            var next=new HashMap<ResourceLocation,BuiltinBathDefinition>();var jsons=new ArrayList<String>();
            for(String text:originals) {
                var json=com.google.gson.JsonParser.parseString(text).getAsJsonObject();
                var profile=com.google.gson.JsonParser.parseString("{\"effects\":[{\"effect\":\"minecraft:speed\",\"duration\":81,\"amplifier\":2,\"ambient\":true,\"show_particles\":false,\"show_icon\":false,\"after_ticks\":2,\"minimum_entries\":0}],\"instant_healing\":1,\"extinguish\":true,\"cleansing\":{\"after_ticks\":2,\"count\":10,\"category\":\"harmful\",\"selection\":\"first\",\"exclude\":[\"minecraft:slowness\"],\"include\":[]}}");
                json.add("bath",profile);var definition=BuiltinBathDefinition.parse(json);next.put(definition.id(),definition);jsons.add(json.toString());
            }
            BuiltinBathDefinitions.replace(BuiltinBathDefinitions.catalog(next,jsons));

        h.onEachTick(()->run.guard(()->{
            if(run.pig==null)return;
            run.pig.tickCount++;
            var state=h.getLevel().getBlockState(run.fixture.pos());
            state.entityInside(h.getLevel(),run.fixture.pos(),run.pig);
            float health=run.pig.getHealth();
            state.entityInside(h.getLevel(),run.fixture.pos(),run.pig);
            h.assertTrue(run.pig.getHealth()==health,"Duplicate same-world-tick callback repeated instant healing");
        }));
        int index=0;
        for(var definition:next.values())for(String style:STYLES) {
            int tick=1+index++*8;
            h.runAtTickTime(tick,()->run.guard(()->{
                run.fixture=fixture(h,style,new FluidStack(BuiltInRegistries.FLUID.get(definition.id()),1000));
                run.pig=new Pig(EntityType.PIG,h.getLevel());
                var pos=run.fixture.pos();run.pig.setPos(pos.getX()+.5,pos.getY()+.3,pos.getZ()+.5);run.pig.setHealth(5);
                run.pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON,200,0));
                run.pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,0));run.pig.setRemainingFireTicks(100);
            }));
            h.runAtTickTime(tick+1,()->run.guard(()->h.assertTrue(!run.pig.hasEffect(MobEffects.MOVEMENT_SPEED),style+" ignored its configured delay")));
            h.runAtTickTime(tick+5,()->run.guard(()->{
                var effect=run.pig.getEffect(MobEffects.MOVEMENT_SPEED);
                h.assertTrue(effect!=null&&effect.getAmplifier()==2&&effect.getDuration()==81&&effect.isAmbient()&&!effect.isVisible()&&!effect.showIcon(),style+" "+definition.id()+" profile payload");
                h.assertTrue(run.pig.getHealth()>5&&!run.pig.isOnFire()&&!run.pig.hasEffect(MobEffects.POISON)&&run.pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),style+" healing/extinguish/cleansing exclusions");
                run.close();
            }));
        }
        h.runAtTickTime(1+index*8,()->run.guard(()->{run.close();run.restore();run.finished=true;h.succeed();}));
    }

    @GameTest(template="pool_empty")
    public static void custom_definition_reload_updates_existing_containers(GameTestHelper h) {
        var originals=new ArrayList<>(CustomFluidAPI.getAllFluids());var definition=CustomFluidAPI.getFluidDefinition(ID).orElseThrow();
        var changed=new CustomFluidDefinition(definition.id(),0x123456,18f,700,900,0,.2f,7,9,false,false,false,definition.effects(),0,
                definition.textureFlowing(),definition.textureStill(),definition.nameKey(),definition.translations());
        try {
            for(String style:STYLES) {
                CustomFluidRegistry.onFluidsReloaded(originals);var f=fixture(h,style);
                try {
                    var reloaded=new ArrayList<>(originals);reloaded.removeIf(d->d.id().equals(ID));reloaded.add(changed);CustomFluidRegistry.onFluidsReloaded(reloaded);
                    var stack=CustomFluidAPI.createCustomFluidStack(ID,1000);var type=stack.getFluid().getFluidType();var state=stack.getFluid().defaultFluidState();
                var properties=HotBathApi.fluidProperties(stack,false);
                h.assertTrue(properties.density()==changed.density()&&properties.viscosity()==changed.viscosity(),style+" stack physical properties");
                    if(h.getLevel().getBlockEntity(f.pos()) instanceof BathFluidSource source)h.assertTrue(source.temperature()==18&&!source.isHot()&&!source.emitsSteam(),style+" cached old heat/steam after reload");
                } finally {f.close(h);}
            }
        } finally {CustomFluidRegistry.onFluidsReloaded(originals);}
        h.succeed();
    }
}
