package mod.crabmod.showercore.gametest;

import com.crabmod.hotbath.custom_fluid.*;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.pool.*;
import mod.crabmod.showercore.utils.CoreUtils;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("showercore") @PrefixGameTestTemplate(false)
public final class BathFluidContractGameTests {
    @GameTest(template="pool_empty")
    public static void builtin_temperature_is_shared_and_follows_reload(GameTestHelper h) {
        var original=BuiltinBathDefinitions.serverJsons();
        try {
            for(float temperature:new float[]{18,61}) {
                var definitions=new HashMap<ResourceLocation,BuiltinBathDefinition>();
                var jsons=new ArrayList<String>();
                for(String input:original) {
                    var json=com.google.gson.JsonParser.parseString(input).getAsJsonObject();
                    json.addProperty("temperature",temperature);json.addProperty("cleans_dirtiness",temperature<35);
                    var def=BuiltinBathDefinition.parse(json);definitions.put(def.id(),def);jsons.add(json.toString());
                }
                BuiltinBathDefinitions.replace(BuiltinBathDefinitions.catalog(definitions,jsons));
                for(var type:BathtubBlock.LiquidType.values()) {
                    if(type==BathtubBlock.LiquidType.EMPTY||type==BathtubBlock.LiquidType.WATER||type==BathtubBlock.LiquidType.CUSTOM)continue;
                    var fluid=BuiltInRegistries.FLUID.get(ResourceLocation.parse("hotbath:"+type.getSerializedName()+"_fluid"));
                    compare(h,type,new FluidStack(fluid,1000),temperature,temperature>=35);
                }
            }
        } finally {
            var definitions=new HashMap<ResourceLocation,BuiltinBathDefinition>();
            for(String json:original){var def=BuiltinBathDefinition.parse(com.google.gson.JsonParser.parseString(json));definitions.put(def.id(),def);}
            BuiltinBathDefinitions.replace(BuiltinBathDefinitions.catalog(definitions,original));
        }
        h.succeed();
    }

    private static void compare(GameTestHelper h,BathtubBlock.LiquidType type,FluidStack fluid,float temperature,boolean hot) {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(3,1,3));
        var spec=new PoolSpec(PoolSpec.Shape.RECTANGLE,5,5,0);var player=h.makeMockPlayer(GameType.CREATIVE);
        BlockPos.betweenClosedStream(origin.offset(-1,-1,-1),origin.offset(7,-1,7))
                .forEach(pos->level.setBlock(pos,Blocks.STONE.defaultBlockState(),3));
        h.assertTrue(PoolStructure.place(level,origin,new PoolSettings(spec,ResourceLocation.parse("minecraft:quartz_block"),BathtubMaterialFace.SIDE,false),player),"Native pool placement");
        var pool=(PoolBlockEntity)level.getBlockEntity(origin.offset(PoolLayout.of(spec).controllerX(),0,0));
        var foot=origin.offset(7,0,2);var head=foot.north();
        var block=(BathtubBlock)BuiltInRegistries.BLOCK.get(ResourceLocation.parse("showercore:bathtub_white"));
        var state=block.defaultBlockState().setValue(BathtubBlock.FACING,Direction.NORTH).setValue(BathtubBlock.LIQUID,type);
        try {
            var full=fluid.copy();full.setAmount(pool.layout().capacity());
            h.assertTrue(pool.fluidHandler().fill(full,IFluidHandler.FluidAction.EXECUTE)==full.getAmount(),"Native pool fill");
            h.assertTrue(pool.isHot()==hot,"Pool heat must read the reloaded built-in definition");
            h.assertTrue(pool.emitsSteam()==hot,"Pool steam must follow the built-in temperature");
            player.setPos(origin.getX()+2.5,origin.getY()+.2,origin.getZ()+2.5);
            h.assertTrue(Math.abs(CoreUtils.getShowerCoreBathTemperature(player)-temperature)<.001,"Pool compatibility temperature differs from Hot Bath");
            h.assertTrue(CoreUtils.isPlayerInShowerCoreCleansingFluid(player)==(temperature<35),"Cleansing must follow its own setting independently of heat");
            level.setBlock(foot,state.setValue(BathtubBlock.PART,BedPart.FOOT),3);
            level.setBlock(head,state.setValue(BathtubBlock.PART,BedPart.HEAD),3);
            ((BathtubBlockEntity)level.getBlockEntity(foot)).getFluidTank().setFluid(fluid.copy());
            ((BathtubBlockEntity)level.getBlockEntity(head)).getFluidTank().setFluid(fluid.copy());
            player.setPos(foot.getX()+.5,foot.getY()+.3,foot.getZ()+.5);
            h.assertTrue(CoreUtils.isEntityInAnyHotBathtub(player)==hot,"Ordinary bathtub heat differs from Hot Bath");
            h.assertTrue(Math.abs(CoreUtils.getShowerCoreBathTemperature(player)-temperature)<.001,"Ordinary bathtub compatibility temperature differs from Hot Bath");
            h.assertTrue(CoreUtils.isPlayerInShowerCoreCleansingFluid(player)==(temperature<35),"Ordinary bath cleansing must follow data independently of heat");
        } finally {
            PoolStructure.dismantle(pool,false);level.setBlock(head,Blocks.AIR.defaultBlockState(),3);level.setBlock(foot,Blocks.AIR.defaultBlockState(),3);
            for(var entity:level.getEntitiesOfClass(mod.crabmod.showercore.entity.FaucetInteractionEntity.class,h.getBounds()))entity.discard();
        }
    }

    @GameTest(template="pool_empty")
    public static void hot_native_fluid_keeps_temperature_compatibility(GameTestHelper h) {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(3,1,3));var spec=new PoolSpec(PoolSpec.Shape.RECTANGLE,5,5,0);
        var player=h.makeMockPlayer(GameType.CREATIVE);
        BlockPos.betweenClosedStream(origin.offset(-1,-1,-1),origin.offset(6,-1,6)).forEach(pos->level.setBlock(pos,Blocks.STONE.defaultBlockState(),3));
        h.assertTrue(PoolStructure.place(level,origin,new PoolSettings(spec,ResourceLocation.parse("minecraft:quartz_block"),BathtubMaterialFace.SIDE,false),player),"Native hot-fluid pool");
        var pool=(PoolBlockEntity)level.getBlockEntity(origin.offset(PoolLayout.of(spec).controllerX(),0,0));
        try {
            var lava=new FluidStack(net.minecraft.world.level.material.Fluids.LAVA,pool.layout().capacity());
            h.assertTrue(pool.fluidHandler().fill(lava,IFluidHandler.FluidAction.EXECUTE)==lava.getAmount(),"Native lava capability fill");
            player.setPos(origin.getX()+2.5,origin.getY()+.3,origin.getZ()+2.5);
            player.baseTick();
            float expected=lava.getFluid().getFluidType().getTemperature(lava)-273f;
            h.assertTrue(CoreUtils.isPlayerInShowerCoreHotWater(player),"Existing hot-fluid contact disappeared");
            h.assertTrue(Math.abs(com.crabmod.hotbath.util.CustomFluidHandler.getBathTemperature(player)-expected)<.001,"Warmth detection and compatibility temperature disagree");
            pool.fluidHandler().drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.EXECUTE);
            var water=new FluidStack(net.minecraft.world.level.material.Fluids.WATER,pool.layout().capacity());pool.fluidHandler().fill(water,IFluidHandler.FluidAction.EXECUTE);
            player.baseTick();
            h.assertTrue(!com.crabmod.hotbath.util.CustomFluidHandler.isPlayerInHotBath(player),"Ordinary water became a hot bath");
        } finally {PoolStructure.dismantle(pool,false);}
        h.succeed();
    }
}
