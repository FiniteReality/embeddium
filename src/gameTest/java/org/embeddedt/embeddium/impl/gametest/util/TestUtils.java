package org.embeddedt.embeddium.impl.gametest.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.embeddedt.embeddium.impl.gametest.network.SyncS2CPacket;
import org.slf4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

public class TestUtils {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ExecutorService WAITING_EXECUTOR = Executors.newCachedThreadPool();

    public static Vec3 getClientPosition() {
        return Minecraft.getInstance().submit(() -> {
            var clientPlayer = Minecraft.getInstance().player;
            return clientPlayer != null ? clientPlayer.position() : Vec3.ZERO;
        }).join();
    }

    public static boolean isChunkVisible(Vec3 position) {
        return Minecraft.getInstance().submit(() -> {
            // Verify chunk is rendered
            BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
            return Minecraft.getInstance().levelRenderer.isChunkCompiled(pos);
        }).join();
    }

    public static void sleepForMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitForConditionMetOnClient(BooleanSupplier condition) {
        var future = CompletableFuture.runAsync(() -> {
            while(!condition.getAsBoolean()) {
                sleepForMillis(100);
            }
        }, WAITING_EXECUTOR);

        try {
            future.get(10, TimeUnit.SECONDS);
        } catch(TimeoutException | InterruptedException | ExecutionException e) {
            throw new RuntimeException("Client did not meet condition quickly enough", e);
        }
    }

    public static void waitTillClientIsCloseTo(Vec3 position) {
        // Stall until client confirms new position
        var positionFuture = CompletableFuture.runAsync(() -> {
            Vec3 playerPos = getClientPosition();
            while(playerPos == Vec3.ZERO || playerPos.distanceToSqr(position) > 1) {
                sleepForMillis(100);
                playerPos = getClientPosition();
            }
            // Client is at new position, wait for chunk underneath to render
            Minecraft.getInstance().submit(() -> Minecraft.getInstance().levelRenderer.allChanged()).join();
            while(!isChunkVisible(position)) {
                sleepForMillis(100);
            }
        }, WAITING_EXECUTOR);

        try {
            positionFuture.get(10, TimeUnit.SECONDS);
        } catch(TimeoutException | InterruptedException | ExecutionException e) {
            throw new RuntimeException("Client position did not update", e);
        }
    }

    /**
     * Move the player to a fixed position and ensure they remain there.
     */
    public static void movePlayerToPosition(GameTestHelper helper, BlockPos pos) {
        BlockPos realPos = helper.absolutePos(pos);
        var playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        if(playerList.size() != 1) {
            throw new IllegalStateException("Unexpected number of players: " + playerList.size());
        }
        var player = playerList.get(0);
        var abilities = player.getAbilities();
        if(!abilities.mayfly || !abilities.flying || !abilities.invulnerable) {
            // Make camera stationary
            abilities.mayfly = true;
            abilities.flying = true;
            abilities.invulnerable = true;
            player.onUpdateAbilities();
        }
        // Move camera
        player.setXRot(90);
        player.setYRot(0);
        player.teleportTo(realPos.getX(), realPos.getY(), realPos.getZ());
        clientBarrier();
        waitTillClientIsCloseTo(new Vec3(realPos.getX(), realPos.getY(), realPos.getZ()));
        // Update camera on client
        Minecraft.getInstance().submit(() -> {
            var cplayer = Minecraft.getInstance().player;
            cplayer.setXRot(90);
            cplayer.setYRot(0);
        }).join();
    }

    public static void obtainScreenshot(String name) {
        var mc = Minecraft.getInstance();
        mc.submit(() -> {
            mc.getMainRenderTarget().bindWrite(true);
            // Render a frame by force
            mc.gameRenderer.render(1f, 0, true);
            mc.getMainRenderTarget().unbindWrite();
            NativeImage nativeimage = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            File screenShotDir = new File(mc.gameDirectory, "screenshots");
            screenShotDir.mkdir();
            File screenShot = new File(screenShotDir, name + ".png");
            screenShot.delete();
            try {
                nativeimage.writeToFile(screenShot);
            } catch(Exception e) {
                LOGGER.warn("Screenshot failed", e);
            } finally {
                nativeimage.close();
            }
        }).join();
    }

    /**
     * Wait until the client thread has processed all packets up to this point.
     */
    public static void clientBarrier() {
        // Broadcast chunk changes
        ServerLifecycleHooks.getCurrentServer().overworld().getChunkSource().tick(() -> false, true);
        // Send barrier packet
        SyncS2CPacket packet = new SyncS2CPacket();
        packet.applyBarrier();
    }

    private static final Method getBoundsMethod = ObfuscationReflectionHelper.findMethod(GameTestHelper.class, "getBounds");

    public static boolean isAABBLoaded(AABB bounds) {
        int minX = SectionPos.posToSectionCoord(bounds.minX - 0.5D);
        int minY = SectionPos.posToSectionCoord(bounds.minY - 0.5D);
        int minZ = SectionPos.posToSectionCoord(bounds.minZ - 0.5D);

        int maxX = SectionPos.posToSectionCoord(bounds.maxX + 0.5D);
        int maxY = SectionPos.posToSectionCoord(bounds.maxY + 0.5D);
        int maxZ = SectionPos.posToSectionCoord(bounds.maxZ + 0.5D);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        var levelRenderer = Minecraft.getInstance().levelRenderer;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x << 4, y << 4, z << 4);
                    if (!levelRenderer.isChunkCompiled(pos)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static void waitForTestAreaToLoad(GameTestHelper helper) {
        AABB bounds;
        try {
            bounds = (AABB)getBoundsMethod.invoke(helper);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        waitForConditionMetOnClient(() -> isAABBLoaded(bounds));
    }
}
