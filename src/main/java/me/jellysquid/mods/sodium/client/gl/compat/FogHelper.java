package me.jellysquid.mods.sodium.client.gl.compat;

import org.lwjgl.opengl.GL20;

import com.mojang.blaze3d.platform.GlStateManager;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;

public class FogHelper {
    private static final float FAR_PLANE_THRESHOLD_EXP = (float) Math.log(1.0f / 0.0019f);
    private static final float FAR_PLANE_THRESHOLD_EXP2 = Mth.sqrt(FAR_PLANE_THRESHOLD_EXP);

    public static float getFogEnd() {
        return GlStateManager.FOG.end;
    }

    public static float getFogStart() {
        return GlStateManager.FOG.start;
    }

    public static float getFogDensity() {
        return GlStateManager.FOG.density;
    }

    public static float getFogCutoff() {
        int mode = GlStateManager.FOG.mode;

        switch (mode) {
            case GL20.GL_LINEAR:
                return getFogEnd();
            case GL20.GL_EXP:
                return FAR_PLANE_THRESHOLD_EXP / getFogDensity();
            case GL20.GL_EXP2:
                return FAR_PLANE_THRESHOLD_EXP2 / getFogDensity();
            default:
                return 0.0f;
        }
    }

    public static float[] getFogColor() {
        return new float[]{FogRenderer.fogRed, FogRenderer.fogGreen, FogRenderer.fogBlue, 1.0F};
    }
}
