package net.neoforged.neoforge.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

public class ClientHooks {
    public static void dispatchRenderStage(RenderType renderLayer, LevelRenderer levelRenderer, Matrix4f pose, Matrix4f matrix, int ticks, Camera mainCamera, Frustum frustum) {
    }
}
