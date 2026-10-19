package mod.crabmod.showercore.pool;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

/** Fluid identity shared by seated effects and temperature/cleanliness integration. */
public interface BathFluidSource {
    @Nullable ResourceLocation getCustomFluidId();
    default Optional<CustomFluidDefinition> getCustomFluidDefinition() {
        var id=getCustomFluidId();
        return id==null?Optional.empty():CustomFluidAPI.getFluidDefinition(id);
    }
}
