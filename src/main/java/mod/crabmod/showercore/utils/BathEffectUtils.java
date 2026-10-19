package mod.crabmod.showercore.utils;

import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.crabmod.hotbath.registers.ParticleRegister;
import mod.crabmod.showercore.particle.ShowerParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;

public class BathEffectUtils {
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isEffectActive;
    private final List<FadeableSoundInstance> activeSounds = new ArrayList<>();

    public BathEffectUtils() {
        this.isEffectActive = new AtomicBoolean(false);
    }

    private void ensureScheduler() {
        if (this.scheduler == null || this.scheduler.isShutdown()) {
            this.scheduler = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r);
                t.setDaemon(true); // Ensure threads are daemon threads
                return t;
            });
        }
    }

    public void renderBathWater(Level level, Vec3 origin, java.util.function.Supplier<ParticleOptions> particleTypeSupplier, double width, double depth) {
        if (isEffectActive.get()) return;
        renderBathWater(level, origin, 2, particleTypeSupplier, width, depth);
        playBathSound(level, 200, origin);
    }

    private void generateOuterSteamParticles(Level worldIn, BlockPos pos, RandomSource rand, int radius) {
        List<BlockPos> airPositions = new ArrayList<>();

        // 遍历立方体范围，找到最外层的空气块，并存储到列表中
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // 确定是否在最外层
                    if (dx == -radius || dx == radius || dy == -radius || dy == radius || dz == -radius || dz == radius) {
                        BlockPos checkPos = pos.offset(dx, dy, dz);

                        if (worldIn.getBlockState(checkPos).isAir()) {
                            airPositions.add(checkPos); // 添加符合条件的位置
                        }
                    }
                }
            }
        }

        // 如果找到符合条件的位置，则随机选择一个生成粒子
        if (!airPositions.isEmpty()) {
            BlockPos selectedPos = airPositions.get(rand.nextInt(airPositions.size()));
            worldIn.addParticle(
                    (ParticleOptions) ParticleRegister.STEAM_PARTICLE.get(),
                    selectedPos.getX() + rand.nextDouble(),
                    selectedPos.getY() + rand.nextDouble(),
                    selectedPos.getZ() + rand.nextDouble(),
                    0.0,
                    0.02,
                    0.0
            );
        }
    }

    public void renderBathWater(Level level, Vec3 origin, int particleInterval, java.util.function.Supplier<ParticleOptions> particleTypeSupplier, double width, double depth) {
        // 如果效果已激活，则返回，避免重复开启
        if (isEffectActive.get()) {
            return;
        }
        isEffectActive.set(true);
        ensureScheduler();

        // 定期生成粒子效果
        scheduler.scheduleAtFixedRate(
                () -> {
                    if (!isEffectActive.get()) {
                        return; // 如果效果已停用，跳过任务
                    }

                    if (Minecraft.getInstance().level != level) {
                        this.shutdown();
                        return; // 如果世界已更改，停止任务
                    }

                    if (Minecraft.getInstance().isPaused()) {
                        return;
                    }

                    Minecraft.getInstance().execute(() -> {
                        // 设置相对高度范围
                        double invisibleHeightMin = origin.y - 0.38;
                        double invisibleHeightMax = origin.y + 3.22;

                        // 将高度范围设置到 ThreadLocal
                        ShowerParticle.INVISIBLE_HEIGHT_MIN.set(invisibleHeightMin);
                        ShowerParticle.INVISIBLE_HEIGHT_MAX.set(invisibleHeightMax);

                        for (int i = 0; i < 1; i++) {
                            double offsetX = origin.x + (level.random.nextDouble() - 0.5) * width;
                            double offsetY = origin.y;
                            double offsetZ = origin.z + (level.random.nextDouble() - 0.5) * depth;
                            double velocityY = 3;
                            // 添加粒子
                            level.addParticle(
                                    particleTypeSupplier.get(),
                                    offsetX,
                                    offsetY,
                                    offsetZ,
                                    0,
                                    velocityY,
                                    0
                            );

                            if (level.random.nextInt(5000) == 0) {
                                this.generateOuterSteamParticles(level, BlockPos.containing(origin), level.random, 1);
                            }
                        }

                        // 清理 ThreadLocal 防止数据泄漏
                        ShowerParticle.INVISIBLE_HEIGHT_MIN.remove();
                        ShowerParticle.INVISIBLE_HEIGHT_MAX.remove();
                    });
                },
                0,
                particleInterval,
                TimeUnit.MILLISECONDS
        );
    }

    public void playBathSound(Level level, int soundInterval, Vec3 origin) {
        ensureScheduler();
        // 定期播放声音效果
        scheduler.scheduleAtFixedRate(
                () -> {
                    if (!isEffectActive.get()) {
                        return; // 如果效果已停用，跳过任务
                    }

                    if (Minecraft.getInstance().level != level) {
                        this.shutdown();
                        return; // 如果世界已更改，停止任务
                    }

                    if (Minecraft.getInstance().isPaused()) {
                        return;
                    }

                    Minecraft.getInstance().execute(() -> {
                        if (Minecraft.getInstance().level == null || !isEffectActive.get()) return;
                        
                        FadeableSoundInstance soundInstance = new FadeableSoundInstance(
                                SoundEvents.WEATHER_RAIN,
                                SoundSource.BLOCKS,
                                0.2F,
                                1.0F,
                                RandomSource.create(),
                                origin
                        );
                        Minecraft.getInstance().getSoundManager().play(soundInstance);
                        activeSounds.add(soundInstance);
                        
                        // Cleanup inactive sounds
                        activeSounds.removeIf(instance -> !Minecraft.getInstance().getSoundManager().isActive(instance));
                    });
                },
                0,
                soundInterval,
                TimeUnit.MILLISECONDS
        );
    }

    // 停止粒子效果
    public void stopBathEffect() {
        isEffectActive.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        
        Minecraft.getInstance().execute(() -> {
            for (FadeableSoundInstance sound : activeSounds) {
                sound.triggerFadeOut();
            }
            activeSounds.clear();
        });
    }

    private static class FadeableSoundInstance extends AbstractTickableSoundInstance {
        private boolean fadingOut = false;
        private int fadeTicks = 0;
        private final int fadeDuration = 20; // 1 second
        private final float originalVolume;

        public FadeableSoundInstance(SoundEvent sound, SoundSource source, float volume, float pitch, RandomSource random, Vec3 origin) {
            super(sound, source, random);
            this.volume = volume;
            this.pitch = pitch;
            this.x = origin.x;
            this.y = origin.y;
            this.z = origin.z;
            this.originalVolume = volume;
        }

        @Override
        public void tick() {
            if (fadingOut) {
                fadeTicks++;
                if (fadeTicks >= fadeDuration) {
                    this.stop();
                } else {
                    this.volume = originalVolume * (1.0f - (float)fadeTicks / fadeDuration);
                }
            }
        }

        public void triggerFadeOut() {
            this.fadingOut = true;
        }
    }

    public void shutdown() {
        stopBathEffect();
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
