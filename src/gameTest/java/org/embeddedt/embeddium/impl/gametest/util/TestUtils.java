package org.embeddedt.embeddium.impl.gametest.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;
import java.util.concurrent.*;

public class TestUtils {
    private static final ExecutorService WAITING_EXECUTOR = Executors.newCachedThreadPool();

    public static Vec3 getClientPosition() {
        return Minecraft.getInstance().submit(() -> {
            var clientPlayer = Minecraft.getInstance().player;
            return clientPlayer != null ? clientPlayer.position() : Vec3.ZERO;
        }).join();
    }

    public static boolean isChunkVisible(Vec3 position) {
        return Minecraft.getInstance().submit(() -> {
            return Minecraft.getInstance().levelRenderer.isChunkCompiled(BlockPos.containing(position.x, position.y, position.z));
        }).join();
    }

    public static void sleepForMillis(long millis) {
        try {
            Thread.sleep(100L);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
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
            Minecraft.getInstance().submit(() -> Minecraft.getInstance().levelRenderer.allChanged());
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
        waitTillClientIsCloseTo(new Vec3(realPos.getX(), realPos.getY(), realPos.getZ()));
        // Update camera on client
        Minecraft.getInstance().execute(() -> {
            var cplayer = Minecraft.getInstance().player;
            cplayer.setXRot(90);
            cplayer.setYRot(0);
        });
    }

    public static void obtainScreenshot(String name) {
        var mc = Minecraft.getInstance();
        mc.submit(() -> {
            RenderTarget rendertarget = null;
            try {
                rendertarget = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
                Screenshot.grab(mc.gameDirectory, name + ".png", rendertarget, component -> {});
            } finally {
                if(rendertarget != null) {
                    rendertarget.destroyBuffers();
                }
            }
        }).join();
    }
}
