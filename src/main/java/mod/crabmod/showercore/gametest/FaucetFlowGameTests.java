package mod.crabmod.showercore.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.Direction;
import mod.crabmod.showercore.registers.BlocksRegister;
import java.util.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
@GameTestHolder("showercore") @PrefixGameTestTemplate(false)
public final class FaucetFlowGameTests {
 @GameTest(template="empty",timeoutTicks=100) public static void faucet_north(GameTestHelper h){BathtubFluidTestSupport.check(h,BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),Direction.NORTH,List.of("water","hot_water","golden_bath"),true);}
 @GameTest(template="empty",timeoutTicks=100) public static void faucet_east(GameTestHelper h){BathtubFluidTestSupport.check(h,BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),Direction.EAST,List.of("water","hot_water","golden_bath"),true);}
 @GameTest(template="empty",timeoutTicks=100) public static void faucet_south(GameTestHelper h){BathtubFluidTestSupport.check(h,BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),Direction.SOUTH,List.of("water","hot_water","golden_bath"),true);}
 @GameTest(template="empty",timeoutTicks=100) public static void faucet_west(GameTestHelper h){BathtubFluidTestSupport.check(h,BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),Direction.WEST,List.of("water","hot_water","golden_bath"),true);}
 @GameTest(template="empty",timeoutTicks=100) public static void faucet_legacy_control(GameTestHelper h){BathtubFluidTestSupport.check(h,BlocksRegister.BATHTUB_GREEN.get(),Direction.NORTH,List.of("water"),true);}
}
