package mod.crabmod.showercore.entity;

import mod.crabmod.showercore.block.ShowerHeadBlock;
import mod.crabmod.showercore.Config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mod.crabmod.showercore.base.BaseShowerHeadBlockEntity;
import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import mod.crabmod.showercore.client.ShowerHeadClientHelper;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import mod.crabmod.showercore.utils.CoreUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.effect.MobEffect;

public class ShowerHeadContainerEntity extends BaseShowerHeadBlockEntity {
  private net.minecraft.resources.ResourceLocation materialBlockId;
  private mod.crabmod.showercore.material.BathtubMaterialFace materialFace = mod.crabmod.showercore.material.BathtubMaterialFace.LEGACY;
  public net.minecraft.resources.ResourceLocation getMaterialBlockId() { return materialBlockId; }
  public mod.crabmod.showercore.material.BathtubMaterialFace getMaterialFace() { return materialFace; }
  public record HorizontalMount(float x, float z) {
    public HorizontalMount { if(!Float.isFinite(x)||!Float.isFinite(z)||Math.abs(x)>1||Math.abs(z)>1)throw new IllegalArgumentException("Invalid horizontal mount"); }
  }
  private HorizontalMount horizontalMount = new HorizontalMount(0,0);
  public HorizontalMount horizontalMount() { return horizontalMount; }
  public void setHorizontalMount(float x,float z) {
    var next=new HorizontalMount(x,z);if(next.equals(horizontalMount))return;
    horizontalMount=next;setChanged();
    if(level!=null){if(level.isClientSide)requestModelDataUpdate();else level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
  }
  private boolean effectActive = false;
  private Object bathEffectUtils;
  private final Map<UUID, Integer> timeUnderShower = new HashMap<>();

  /**
   * Tracks player UUIDs that are currently under an active shower head with a core installed.
   * Updated every server tick by the shower head entity tick method.
   * Used by temperature mod compat handlers to detect shower usage.
   */
  private static final ActiveShowerPlayers<ShowerHeadContainerEntity> PLAYERS_UNDER_ACTIVE_SHOWER = new ActiveShowerPlayers<>();

  /**
   * Checks if a player is currently under an active shower head with a core installed.
   * @param playerUUID The UUID of the player to check
   * @return true if the player is under an active shower
   */
  public static boolean isPlayerUnderActiveShower(UUID playerUUID) {
    return PLAYERS_UNDER_ACTIVE_SHOWER.contains(playerUUID);
  }

  /**
   * Removes a player from the active shower tracking set.
   * Called during cleanup on logout/death.
   * @param playerUUID The UUID of the player to remove
   */
  public static void cleanupPlayer(UUID playerUUID) {
    PLAYERS_UNDER_ACTIVE_SHOWER.remove(playerUUID);
    com.crabmod.hotbath.api.HotBathApi.endShowerContact(playerUUID);
  }

  public ShowerHeadContainerEntity(BlockPos pos, BlockState state) {
    super(BlockEntitiesRegister.RAIN_SHOWER_HEAD_CONTAINER.get(), pos, state);
  }

  @Override
  protected Component getDefaultName() {
    return Component.translatable("container.shower_head");
  }

  // 切换效果状态的方法
  public void toggleEffect() {
    effectActive = !effectActive;
    if (!effectActive) clearTrackedPlayers();
    this.setChanged();
  }

  // 获取当前效果状态的方法
  public boolean isEffectActive() {
    return effectActive;
  }

  public net.minecraft.core.particles.SimpleParticleType getParticleType() {
    return CoreUtils.getParticleForCore(this.getItem(0));
  }

  @Override
  public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
    return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public net.minecraft.nbt.CompoundTag getUpdateTag() {
    return this.saveWithoutMetadata();
  }

  @Override
  public void load(net.minecraft.nbt.CompoundTag tag) {
    super.load(tag);
    this.effectActive = tag.getBoolean("EffectActive");
    String material = tag.getString(mod.crabmod.showercore.material.BathroomMaterials.MATERIAL_ID);
    materialBlockId = material.isEmpty() ? null : net.minecraft.resources.ResourceLocation.tryParse(material);
    materialFace = mod.crabmod.showercore.material.BathtubMaterialFace.read(tag.getString(mod.crabmod.showercore.material.BathtubMaterialFace.TAG));
    var beforeMount = horizontalMount;
    float mx=tag.getFloat("MountX"),mz=tag.getFloat("MountZ");
    horizontalMount=Float.isFinite(mx)&&Float.isFinite(mz)&&Math.abs(mx)<=1&&Math.abs(mz)<=1?new HorizontalMount(mx,mz):new HorizontalMount(0,0);
    if(level!=null&&level.isClientSide&&!beforeMount.equals(horizontalMount))stopEffect();
    if (level != null && level.isClientSide) {
      requestModelDataUpdate();
      if(!beforeMount.equals(horizontalMount))level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    if (this.level != null && this.level.isClientSide) {
      if (this.effectActive) {
        this.startEffect();
      } else {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShowerHeadClientHelper.stopBathEffect(this.bathEffectUtils));
      }
    }
  }

  @Override
  public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
    net.minecraft.nbt.CompoundTag tag = pkt.getTag();
    if (tag != null) {
        this.load(tag);
    }
  }

  @Override
  protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
    super.saveAdditional(tag);
    tag.putBoolean("EffectActive", this.effectActive);
    tag.putFloat("MountX",horizontalMount.x());tag.putFloat("MountZ",horizontalMount.z());
    if (materialBlockId != null) {
      tag.putString(mod.crabmod.showercore.material.BathroomMaterials.MATERIAL_ID, materialBlockId.toString());
      tag.putString(mod.crabmod.showercore.material.BathtubMaterialFace.TAG, materialFace.serializedName());
    }
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (this.level != null && this.level.isClientSide) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.bathEffectUtils == null) {
                this.bathEffectUtils = ShowerHeadClientHelper.createBathEffectUtils();
            }
        });
        if (this.effectActive) {
            this.startEffect();
        }
    }
  }

  public void startEffect() {
      if (this.level != null && this.level.isClientSide && this.effectActive) {
            var state = getBlockState();
            var shower = (ShowerHeadBlock)state.getBlock();
            var origin = shower.sprayOrigin(level, worldPosition, state);

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (this.bathEffectUtils == null) {
                    this.bathEffectUtils = ShowerHeadClientHelper.createBathEffectUtils();
                }
                ShowerHeadClientHelper.renderBathWater(this.bathEffectUtils, this.level, origin, this::getParticleType, shower.sprayWidth(state), shower.sprayDepth(state));
            });
      }
  }

  public void stopEffect() {
    if (this.level != null && this.level.isClientSide) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShowerHeadClientHelper.stopBathEffect(this.bathEffectUtils));
    }
  }

  public void shutdownEffect() {
    if (this.level != null && this.level.isClientSide) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShowerHeadClientHelper.shutdown(this.bathEffectUtils));
    }
  }

  private void clearTrackedPlayers() {
    for (UUID player : timeUnderShower.keySet()) {
        PLAYERS_UNDER_ACTIVE_SHOWER.remove(player,this);
        if(!PLAYERS_UNDER_ACTIVE_SHOWER.contains(player))com.crabmod.hotbath.api.HotBathApi.endShowerContact(player);
    }
    timeUnderShower.clear();
  }

  @Override
  public void setRemoved() {
    clearTrackedPlayers();
    super.setRemoved();
    this.shutdownEffect();
  }

  public AABB sprayBounds() {
    var state = getBlockState();
    var shower = (ShowerHeadBlock)state.getBlock();
    var origin = shower.sprayOrigin(level, worldPosition, state);
    double bottomY = origin.y - 10;
    var check = BlockPos.containing(origin).mutable();
    for (int i = 0; i < 10; i++) {
      check.move(net.minecraft.core.Direction.DOWN);
      var collision = level.getBlockState(check).getCollisionShape(level, check);
      if (!collision.isEmpty()) {
        bottomY = Math.min(origin.y, check.getY() + collision.max(net.minecraft.core.Direction.Axis.Y));
        break;
      }
    }
    double halfWidth = shower.sprayWidth(state) / 2;
    double halfDepth = shower.sprayDepth(state) / 2;
    return new AABB(origin.x - halfWidth, bottomY, origin.z - halfDepth,
                    origin.x + halfWidth, origin.y, origin.z + halfDepth);
  }

  public static void tick(Level level, BlockPos pos, BlockState state, ShowerHeadContainerEntity entity) {
    if (level.isClientSide) return;

    if (!entity.isEffectActive() || entity.isEmpty()) {
      entity.clearTrackedPlayers();
      return;
    }

    ItemStack core = entity.getItem(0);
    if (core.isEmpty()) {
        entity.clearTrackedPlayers();
        return;
    }

    AABB detectionBox = entity.sprayBounds();

    List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, detectionBox);
    Set<UUID> currentEntities = new HashSet<>();

    for (LivingEntity livingEntity : entities) {
      UUID uuid = livingEntity.getUUID();
      currentEntities.add(uuid);
      int time = entity.timeUnderShower.getOrDefault(uuid, 0);
      time++;
      entity.timeUnderShower.put(uuid, time);

      // Track players under active shower for compat mod integration
      if (livingEntity instanceof net.minecraft.world.entity.player.Player) {
        PLAYERS_UNDER_ACTIVE_SHOWER.add(uuid, entity);
        com.crabmod.hotbath.api.HotBathApi.reportShowerContact(livingEntity,40f,true,Config.isModIntegrationsEnabled());
      }

      // Hot Bath's dirtiness consumer uses this shower contact alongside any immersion.

      if (time >= 100) { // 5 seconds
        applyEffects(livingEntity, core);
      }
    }

    // Remove players that left this shower from the tracking set
    for (UUID tracked : entity.timeUnderShower.keySet()) {
      if (!currentEntities.contains(tracked)) {
        PLAYERS_UNDER_ACTIVE_SHOWER.remove(tracked, entity);
        if(!PLAYERS_UNDER_ACTIVE_SHOWER.contains(tracked))com.crabmod.hotbath.api.HotBathApi.endShowerContact(tracked);
      }
    }

    entity.timeUnderShower.keySet().retainAll(currentEntities);
  }



  private static void applyEffects(LivingEntity entity, ItemStack core) {
    String path = ForgeRegistries.ITEMS.getKey(core.getItem()).getPath();
    long gameTime = entity.level().getGameTime();
    CompoundTag data = entity.getPersistentData();

    // Prevent double processing in the same tick for the same core type
    String tickKey = "showercore.tick." + path;
    if (data.getLong(tickKey) == gameTime) return;
    data.putLong(tickKey, gameTime);
    
    // Buff Refresh Logic (Every 10s)
    boolean shouldRefreshBuffs = false;
    String buffKey = "showercore.buff_refresh." + path;
    if (gameTime - data.getLong(buffKey) >= 200) {
        shouldRefreshBuffs = true;
        data.putLong(buffKey, gameTime);
    }

    switch (path) {
        case "hot_water_core":
            // Speed II
            if (shouldRefreshBuffs) addStackingEffect(entity, MobEffects.MOVEMENT_SPEED, 400, 1);
            break;
        case "milk_bath_core":
            // Regeneration II
            if (shouldRefreshBuffs) {
                addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                cureNegativeEffects(entity);
            }
            // Hunger: 1 every 15s (300 ticks)
            if (gameTime - data.getLong("showercore.last.milk_hunger") >= 300) {
                entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0, true, false));
                data.putLong("showercore.last.milk_hunger", gameTime);
            }
            break;
        case "herbal_bath_core":
            // Regeneration II, Resistance II
            if (shouldRefreshBuffs) {
                addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                addStackingEffect(entity, MobEffects.DAMAGE_RESISTANCE, 400, 1);
                cureNegativeEffects(entity);
            }
            
            // Undead Damage: 1 every 2s (40 ticks)
            if (entity.isInvertedHealAndHarm()) {
                if (gameTime - data.getLong("showercore.last.herbal_damage") >= 40) {
                    entity.hurt(entity.damageSources().magic(), 1.0f);
                    data.putLong("showercore.last.herbal_damage", gameTime);
                }
            }
            break;
        case "peony_bath_core":
            // Regeneration II, Luck II, Haste II
            if (shouldRefreshBuffs) {
                addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                addStackingEffect(entity, MobEffects.LUCK, 400, 1);
                addStackingEffect(entity, MobEffects.DIG_SPEED, 400, 1);
                entity.removeEffect(MobEffects.BAD_OMEN);
                cureNegativeEffects(entity);
            }
            break;
        case "honey_bath_core":
            // Regeneration II, Absorption IV
            if (shouldRefreshBuffs) {
                addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                addStackingEffect(entity, MobEffects.ABSORPTION, 400, 3);
            }
            // Hunger: 1 every 4s (80 ticks)
            if (gameTime - data.getLong("showercore.last.honey_hunger") >= 80) {
                entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0, true, false));
                data.putLong("showercore.last.honey_hunger", gameTime);
            }
            break;
        case "rose_bath_core":
            // Regeneration II, Strength II
            if (shouldRefreshBuffs) {
                addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                addStackingEffect(entity, MobEffects.DAMAGE_BOOST, 400, 1);
                entity.removeEffect(MobEffects.BAD_OMEN);
                cureNegativeEffects(entity);
            }
            break;
    }
  }

  static void addStackingEffect(LivingEntity entity, MobEffect effect, int durationIncrement, int amplifier) {
    CompoundTag data = entity.getPersistentData();
    String key = "showercore.last_stack." + effect.getDescriptionId();
    long gameTime = entity.level().getGameTime();
    
    // Prevent applying same effect multiple times in short window (e.g. 100 ticks)
    if (gameTime - data.getLong(key) < 100) return; 
    data.putLong(key, gameTime);

    MobEffectInstance current = entity.getEffect(effect);
    int currentDuration = (current != null) ? current.getDuration() : 0;
    int newDuration = Math.min(currentDuration + durationIncrement, 2400); // Cap at 2 mins (2400 ticks)
    if (newDuration < durationIncrement) newDuration = durationIncrement;
    
    entity.addEffect(new MobEffectInstance(effect, newDuration, amplifier, true, false));
  }

  static void cureNegativeEffects(LivingEntity entity) {
    entity.removeEffect(MobEffects.POISON);
    entity.removeEffect(MobEffects.WITHER);
    entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    entity.removeEffect(MobEffects.WEAKNESS);
    entity.removeEffect(MobEffects.CONFUSION);
    entity.removeEffect(MobEffects.BLINDNESS);
    entity.removeEffect(MobEffects.HUNGER);
  }

  static void cureNegativeEffectsExceptSlow(LivingEntity entity) {
    entity.removeEffect(MobEffects.POISON);
    entity.removeEffect(MobEffects.WITHER);
    entity.removeEffect(MobEffects.WEAKNESS);
    entity.removeEffect(MobEffects.CONFUSION);
    entity.removeEffect(MobEffects.BLINDNESS);
    entity.removeEffect(MobEffects.HUNGER);
  }
}
