package mod.crabmod.showercore.pool;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BathFaucet {
    InteractionResult useFaucet(Level level,BlockPos pos,BlockState state,Player player);
}
