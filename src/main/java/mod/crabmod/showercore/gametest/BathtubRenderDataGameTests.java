package mod.crabmod.showercore.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.Direction;
import mod.crabmod.showercore.registers.BlocksRegister;
import java.util.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
@GameTestHolder("showercore") @PrefixGameTestTemplate(false)
public final class BathtubRenderDataGameTests {
 @GameTest(template="empty",timeoutTicks=100) public static void render_data_legacy(GameTestHelper h){var fluids=new ArrayList<>(List.of("water","hot_water"));fluids.addAll(BathtubFluidTestSupport.CUSTOM);BathtubFluidTestSupport.check(h,BlocksRegister.BATHTUB_GREEN.get(),Direction.NORTH,fluids,false);}
 @GameTest(template="empty",timeoutTicks=100) public static void render_data_clawfoot(GameTestHelper h){var fluids=new ArrayList<>(List.of("water","hot_water"));fluids.addAll(BathtubFluidTestSupport.CUSTOM);BathtubFluidTestSupport.check(h,BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),Direction.NORTH,fluids,false);}
}
