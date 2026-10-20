package mod.crabmod.showercore.gametest;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("showercore")
@PrefixGameTestTemplate(false)
public final class ApiBoundaryGameTests {
    private record Tub(net.minecraft.core.BlockPos pos) {
        void close(GameTestHelper helper) {
            helper.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            helper.getLevel().setBlock(pos.north(),Blocks.AIR.defaultBlockState(),3);
        }
    }
    private static Tub tub(GameTestHelper helper,net.neoforged.neoforge.fluids.FluidStack fluid) {
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(3,1,3));
        var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("showercore:bathtub_white"));
        var liquid=com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions.forFluid(fluid.getFluid()).isPresent()
                ?mod.crabmod.showercore.block.BathtubBlock.LiquidType.HOT_WATER:mod.crabmod.showercore.block.BathtubBlock.LiquidType.CUSTOM;
        var state=block.defaultBlockState().setValue(mod.crabmod.showercore.block.BathtubBlock.FACING,net.minecraft.core.Direction.NORTH)
                .setValue(mod.crabmod.showercore.block.BathtubBlock.LIQUID,liquid);
        for(var part:java.util.List.of(net.minecraft.world.level.block.state.properties.BedPart.FOOT,net.minecraft.world.level.block.state.properties.BedPart.HEAD)) {
            var at=part==net.minecraft.world.level.block.state.properties.BedPart.FOOT?pos:pos.north();
            helper.getLevel().setBlock(at,state.setValue(mod.crabmod.showercore.block.BathtubBlock.PART,part),3);
            var bath=(BathtubBlockEntity)helper.getLevel().getBlockEntity(at);bath.getFluidTank().setFluid(fluid.copy());
            bath.setCustomFluidId(CustomFluidAPI.getCustomFluidId(fluid));
        }
        return new Tub(pos);
    }
    @GameTest(template="pool_empty", timeoutTicks=110)
    public static void changing_custom_fluid_resets_immersion_delay(GameTestHelper helper) {
        var first = ResourceLocation.parse("hotbath:contract_bath");
        var second = ResourceLocation.parse("hotbath:contract_quiet");
        var fixture = tub(helper,CustomFluidAPI.createCustomFluidStack(first,1000));
        var pig = new Pig(EntityType.PIG,helper.getLevel());
        pig.setPos(fixture.pos().getX()+.5,fixture.pos().getY()+.3,fixture.pos().getZ()+.5);
        pig.tickCount=100;
        int[] elapsed={0};
        helper.onEachTick(()->{
            int tick=++elapsed[0];
            ((BathtubBlockEntity)helper.getLevel().getBlockEntity(fixture.pos())).applyBathEffects(pig);
            if(tick==45) {
            helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SPEED),"First liquid never reached its configured delay");
            pig.removeAllEffects();
            for(var pos:java.util.List.of(fixture.pos(),fixture.pos().north())) {
                var tub=(BathtubBlockEntity)helper.getLevel().getBlockEntity(pos);
                tub.getFluidTank().setFluid(CustomFluidAPI.createCustomFluidStack(second,1000));
                tub.setCustomFluidId(second);
            }
            }
            if(tick==48)helper.assertTrue(!pig.hasEffect(MobEffects.MOVEMENT_SPEED),"Changing custom fluid reused the previous liquid's immersion timer");
            if(tick==90) {
            helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SPEED),"Second liquid did not apply after its own configured delay");
            fixture.close(helper);pig.discard();helper.succeed();
            }
        });
    }

    @GameTest(template="pool_empty")
    public static void bathtub_effects_do_not_invoke_world_bubble_column_physics(GameTestHelper helper) {
        var fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.parse("hotbath:hot_water_fluid"));
        var fixture=tub(helper,new net.neoforged.neoforge.fluids.FluidStack(fluid,1000));
        helper.getLevel().setBlock(fixture.pos().below(),Blocks.SOUL_SAND.defaultBlockState(),3);
        var pig=new Pig(EntityType.PIG,helper.getLevel());
        pig.setPos(fixture.pos().getX()+.5,fixture.pos().getY()+.3,fixture.pos().getZ()+.5);
        pig.tickCount=100;pig.setDeltaMovement(Vec3.ZERO);
        ((BathtubBlockEntity)helper.getLevel().getBlockEntity(fixture.pos())).applyBathEffects(pig);
        helper.assertTrue(pig.getDeltaMovement().equals(Vec3.ZERO),"Bathtub effect dispatch invoked world bubble-column physics");
        fixture.close(helper);pig.discard();helper.succeed();
    }

    @GameTest(template="pool_empty")
    public static void repeated_callbacks_in_one_world_tick_do_not_advance_delay(GameTestHelper helper) {
        var identity=ResourceLocation.parse("hotbath:contract_bath");
        var fixture=tub(helper,CustomFluidAPI.createCustomFluidStack(identity,1000));
        var pig=new Pig(EntityType.PIG,helper.getLevel());
        pig.setPos(fixture.pos().getX()+.5,fixture.pos().getY()+.3,fixture.pos().getZ()+.5);
        for(int call=0;call<80;call++) {
            pig.tickCount++;
            ((BathtubBlockEntity)helper.getLevel().getBlockEntity(fixture.pos())).applyBathEffects(pig);
        }
        helper.assertTrue(!pig.hasEffect(MobEffects.MOVEMENT_SPEED),"Repeated callbacks advanced a world-tick-based immersion delay");
        fixture.close(helper);pig.discard();helper.succeed();
    }

    @GameTest(template="pool_empty")
    public static void legacy_custom_id_is_restored_into_the_tank_stack(GameTestHelper helper) {
        var identity=ResourceLocation.parse("hotbath:contract_bath");
        var raw=new net.neoforged.neoforge.fluids.FluidStack(com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get(),1000);
        var fixture=tub(helper,raw);
        var bath=(BathtubBlockEntity)helper.getLevel().getBlockEntity(fixture.pos());
        var tag=new net.minecraft.nbt.CompoundTag();var oldTank=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1000);
        oldTank.setFluid(raw.copy());
        oldTank.writeToNBT(helper.getLevel().registryAccess(),tag);
        tag.putString("CustomFluidId",identity.toString());
        bath.loadWithComponents(tag,helper.getLevel().registryAccess());
        helper.assertTrue(identity.equals(CustomFluidAPI.getCustomFluidId(bath.fluid())),"Legacy saved custom identity was lost by the stack API");
        helper.assertTrue(bath.properties().temperatureCelsius()==48&&bath.properties().luminosity()==11,"Legacy bath did not resolve its actual liquid properties");
        fixture.close(helper);helper.succeed();
    }

    @GameTest(template="pool_empty")
    public static void shower_and_immersion_contacts_keep_independent_lifecycles(GameTestHelper helper) {
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var identity=ResourceLocation.parse("hotbath:contract_quiet");
        var fluid=CustomFluidAPI.createCustomFluidStack(identity,1000);
        com.crabmod.hotbath.api.HotBathApi.reportFluidContact(player,fluid,player.getY()+.5);
        com.crabmod.hotbath.api.HotBathApi.reportShowerContact(player,40,true);
        var combined=com.crabmod.hotbath.api.HotBathApi.currentBathContact(player).orElseThrow();
        helper.assertTrue(combined.hot()&&combined.temperatureCelsius()==40&&combined.cleansing()&&combined.immersion(),"Independent hot shower was lost inside a cold bath");
        helper.assertTrue(identity.equals(combined.fluidId()),"Shower replaced the real liquid identity");
        helper.assertTrue(com.crabmod.hotbath.api.HotBathApi.fluidProperties(fluid,false).temperatureCelsius()==18,"Contact reduction changed the actual liquid data");
        com.crabmod.hotbath.api.HotBathApi.endShowerContact(player.getUUID());
        var cold=com.crabmod.hotbath.api.HotBathApi.currentBathContact(player).orElseThrow();
        helper.assertTrue(!cold.hot()&&cold.temperatureCelsius()==18&&cold.immersion(),"Stopped shower left stale thermal contact");
        com.crabmod.hotbath.api.HotBathApi.reportShowerContact(player,40,true);
        com.crabmod.hotbath.api.HotBathApi.endFluidContact(player);
        var shower=com.crabmod.hotbath.api.HotBathApi.currentBathContact(player).orElseThrow();
        helper.assertTrue(shower.hot()&&!shower.immersion()&&shower.fluidId()==null,"Leaving bath also removed the independent shower");
        com.crabmod.hotbath.api.HotBathApi.endShowerContact(player.getUUID());
        helper.assertTrue(com.crabmod.hotbath.api.HotBathApi.currentBathContact(player).isEmpty(),"Ended sources retained contact");helper.succeed();
    }

    @GameTest(template="pool_empty")
    public static void source_and_flowing_baths_report_the_same_definition_identity(GameTestHelper helper) {
        for(String json:com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions.serverJsons()) {
            var definition=com.crabmod.hotbath.custom_fluid.BuiltinBathDefinition.parse(com.google.gson.JsonParser.parseString(json));
            var source=(net.minecraft.world.level.material.FlowingFluid)net.minecraft.core.registries.BuiltInRegistries.FLUID.get(definition.id());
            for(var variant:java.util.List.of(source.getSource(),source.getFlowing())) {
                var pig=new Pig(EntityType.PIG,helper.getLevel());
                var fluid=new net.neoforged.neoforge.fluids.FluidStack(variant,1000);
                try {
                    com.crabmod.hotbath.api.HotBathApi.reportFluidContact(pig,fluid,pig.getY()+.5);
                    var contact=com.crabmod.hotbath.api.HotBathApi.currentBathContact(pig).orElseThrow();
                    helper.assertTrue(definition.id().equals(contact.fluidId()),"Flowing liquid lost its canonical data-pack identity: "+definition.id());
                    helper.assertTrue(contact.temperatureCelsius()==definition.temperature(),"Flowing liquid temperature differs from its definition");
                    helper.assertTrue(com.crabmod.hotbath.util.CustomFluidHandler.isEntityInHerbalBathBlock(pig)==definition.id().getPath().equals("herbal_bath_fluid"),"Named-liquid integration changed between source and flowing variants");
                } finally {
                    com.crabmod.hotbath.api.HotBathApi.endFluidContact(pig);
                    com.crabmod.hotbath.bathing.BathImmersion.forget(pig);pig.discard();
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template="pool_empty")
    public static void optional_integration_policy_preserves_core_contact_and_liquid_identity(GameTestHelper helper) {
        var actor=helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var fluid=CustomFluidAPI.createCustomFluidStack(ResourceLocation.parse("hotbath:contract_quiet"),1000);
        try {
            com.crabmod.hotbath.api.HotBathApi.reportFluidContact(actor,fluid,actor.getY()+.5,false);
            var liquid=com.crabmod.hotbath.api.HotBathApi.currentFluidContact(actor).orElseThrow();
            helper.assertTrue(liquid.bath()&&liquid.cleansing()&&!liquid.hot()&&liquid.temperatureCelsius()==18,"Disabling integrations changed core liquid data");
            helper.assertTrue(!com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(actor),"Source opt-out was ignored");
            com.crabmod.hotbath.api.HotBathApi.reportShowerContact(actor,40,true,true);
            var combined=com.crabmod.hotbath.api.HotBathApi.currentBathContact(actor).orElseThrow();
            helper.assertTrue(combined.hot()&&combined.temperatureCelsius()==40&&combined.cleansing(),"Optional integration policy suppressed core shower contact");
            helper.assertTrue(!com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(actor),"Overlap bypassed the source opt-out");
            helper.assertTrue(!com.crabmod.hotbath.util.CustomFluidHandler.isEntityInHotBathLiquid(actor),"Cold liquid borrowed an overlapping shower's heat");
            com.crabmod.hotbath.api.HotBathApi.endFluidContact(actor);
            helper.assertTrue(com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(actor),"Ended immersion left stale optional integration policy");
            helper.assertTrue(com.crabmod.hotbath.api.HotBathApi.currentFluidContact(actor).isEmpty(),"Shower fabricated an immersion identity");
            helper.assertTrue(!com.crabmod.hotbath.util.CustomFluidHandler.isEntityInHotBathLiquid(actor),"Shower-only contact was classified as liquid immersion");
            com.crabmod.hotbath.api.HotBathApi.reportFluidContact(actor,new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),actor.getY()+.5,true);
            helper.assertTrue(com.crabmod.hotbath.util.CustomFluidHandler.isEntityInHotBath(actor),"Unknown hot liquid lost thermal behavior");
            helper.assertTrue(!com.crabmod.hotbath.util.CustomFluidHandler.isEntityInHotBathLiquid(actor),"Unknown hot liquid acquired Hot Bath-specific effects");
        } finally {
            com.crabmod.hotbath.api.HotBathApi.endShowerContact(actor.getUUID());
            com.crabmod.hotbath.api.HotBathApi.endFluidContact(actor);
            com.crabmod.hotbath.bathing.BathImmersion.forget(actor);actor.discard();
        }
        helper.succeed();
    }
}
