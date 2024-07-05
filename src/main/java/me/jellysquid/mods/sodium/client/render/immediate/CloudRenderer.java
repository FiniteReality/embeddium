package me.jellysquid.mods.sodium.client.render.immediate;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ColorVertex;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.api.render.clouds.ModifyCloudRenderingEvent;
import org.embeddedt.embeddium.render.ShaderModBridge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;

public class CloudRenderer {
    private static final ResourceLocation CLOUDS_TEXTURE_ID = new ResourceLocation("textures/environment/clouds.png");

    private static final int CLOUD_COLOR_NEG_Y = ColorABGR.pack(0.7F, 0.7F, 0.7F, 1.0f);
    private static final int CLOUD_COLOR_POS_Y = ColorABGR.pack(1.0f, 1.0f, 1.0f, 1.0f);
    private static final int CLOUD_COLOR_NEG_X = ColorABGR.pack(0.9F, 0.9F, 0.9F, 1.0f);
    private static final int CLOUD_COLOR_POS_X = ColorABGR.pack(0.9F, 0.9F, 0.9F, 1.0f);
    private static final int CLOUD_COLOR_NEG_Z = ColorABGR.pack(0.8F, 0.8F, 0.8F, 1.0f);
    private static final int CLOUD_COLOR_POS_Z = ColorABGR.pack(0.8F, 0.8F, 0.8F, 1.0f);

    private static final int DIR_NEG_Y = 1 << 0;
    private static final int DIR_POS_Y = 1 << 1;
    private static final int DIR_NEG_X = 1 << 2;
    private static final int DIR_POS_X = 1 << 3;
    private static final int DIR_NEG_Z = 1 << 4;
    private static final int DIR_POS_Z = 1 << 5;

    // 256x256 px cloud.png is 12x12 units
    // 3072 / 256 = 12
    // 3072 / 1024 = 3
    private static final int MAX_SINGLE_CLOUD_SIZE = 3072;

    // 256x256 px cloud.png starts fog 8x from cloud render distance
    // 2048 / 256 = 8
    // 2048 / 1024 = 2
    private static final int CLOUD_PIXELS_TO_FOG_DISTANCE = 2048;

    // 256x256 px cloud.png results in a minimum render distance of 32
    // 256 / 8 = 32
    // 1024 / 8 = 128
    private static final float CLOUD_PIXELS_TO_MINIMUM_RENDER_DISTANCE = 0.125F;

    // 256x256 px cloud.png results in render distance multiplier of 2
    // 256 / 128 = 2
    // 1024 / 128 = 8
    private static final float CLOUD_PIXELS_TO_MAXIMUM_RENDER_DISTANCE = 0.0078125F;

    private VertexBuffer vertexBuffer;
    private CloudEdges edges;
    private ShaderInstance shader;
    private final FogRenderer.FogData fogData = new FogRenderer.FogData(FogRenderer.FogMode.FOG_TERRAIN);

    private int prevCenterCellX, prevCenterCellY, cachedRenderDistance;
    private float cloudSizeX, cloudSizeZ, fogDistanceMultiplier;
    private int cloudDistanceMinimum, cloudDistanceMaximum;
    private CloudStatus cloudRenderMode;

    public CloudRenderer(ResourceProvider factory) {
        this.reloadTextures(factory);
    }

    private static int fireModifyCloudRenderDistanceEvent(int distance) {
        var event = new ModifyCloudRenderingEvent(distance);
        ModifyCloudRenderingEvent.BUS.post(event);
        return event.getCloudRenderDistance();
    }

    public void render(@Nullable ClientLevel world, LocalPlayer player, PoseStack matrices, Matrix4f projectionMatrix, float ticks, float tickDelta, double cameraX, double cameraY, double cameraZ) {
        if (world == null) {
            return;
        }

        float cloudHeight = world.effects().getCloudHeight();

        // Vanilla uses NaN height as a way to disable cloud rendering
        if (Float.isNaN(cloudHeight)) {
            return;
        }

        Vec3 color = world.getCloudColor(tickDelta);

        double cloudTime = (ticks + tickDelta) * 0.03F;
        double cloudCenterX = (cameraX + cloudTime);
        double cloudCenterZ = (cameraZ) + 3.96D;

        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
        // This insanity (as opposed to just wrapping the call) is necessary to preserve the original assignment for mixin compat
        renderDistance = fireModifyCloudRenderDistanceEvent(renderDistance);
        int cloudDistance = Math.max(this.cloudDistanceMinimum, (renderDistance * this.cloudDistanceMaximum) + 9);

        int centerCellX = (int) (Math.floor(cloudCenterX / this.cloudSizeX));
        int centerCellZ = (int) (Math.floor(cloudCenterZ / this.cloudSizeZ));

        if (this.vertexBuffer == null || this.prevCenterCellX != centerCellX || this.prevCenterCellY != centerCellZ || this.cachedRenderDistance != renderDistance || cloudRenderMode != Minecraft.getInstance().options.getCloudsType()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            this.cloudRenderMode = Minecraft.getInstance().options.getCloudsType();

            this.rebuildGeometry(bufferBuilder, cloudDistance, centerCellX, centerCellZ);

            if (this.vertexBuffer == null) {
                this.vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            }

            this.vertexBuffer.bind();
            this.vertexBuffer.upload(bufferBuilder.end());

            VertexBuffer.unbind();

            this.prevCenterCellX = centerCellX;
            this.prevCenterCellY = centerCellZ;
            this.cachedRenderDistance = renderDistance;
        }

        float previousEnd = RenderSystem.getShaderFogEnd();
        float previousStart = RenderSystem.getShaderFogStart();
        this.fogData.end = cloudDistance * this.fogDistanceMultiplier;
        this.fogData.start = (cloudDistance * this.fogDistanceMultiplier) - 16;

        applyFogModifiers(world, this.fogData, player, (int)(cloudDistance * this.fogDistanceMultiplier), tickDelta);


        RenderSystem.setShaderFogEnd(this.fogData.end);
        RenderSystem.setShaderFogStart(this.fogData.start);

        float translateX = (float) (cloudCenterX - (centerCellX * this.cloudSizeX));
        float translateZ = (float) (cloudCenterZ - (centerCellZ * this.cloudSizeZ));

        RenderSystem.enableDepthTest();

        this.vertexBuffer.bind();

        boolean insideClouds = cameraY < cloudHeight + 4.5f && cameraY > cloudHeight - 0.5f;
        boolean fastClouds = cloudRenderMode == CloudStatus.FAST;

        if (insideClouds || fastClouds) {
            RenderSystem.disableCull();
        } else {
            RenderSystem.enableCull();
        }

        RenderSystem.setShaderColor((float) color.x, (float) color.y, (float) color.z, 0.8f);

        matrices.pushPose();

        Matrix4f modelViewMatrix = matrices.last().pose();
        modelViewMatrix.translate(-translateX, cloudHeight - (float) cameraY + 0.33F, -translateZ);

        // PASS 1: Set up depth buffer
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);

        this.vertexBuffer.drawWithShader(modelViewMatrix, projectionMatrix, this.shader);

        // PASS 2: Render geometry
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL30C.GL_EQUAL);
        RenderSystem.colorMask(true, true, true, true);

        this.vertexBuffer.drawWithShader(modelViewMatrix, projectionMatrix, this.shader);

        matrices.popPose();

        VertexBuffer.unbind();

        RenderSystem.disableBlend();
        RenderSystem.depthFunc(GL30C.GL_LEQUAL);

        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        RenderSystem.setShaderFogEnd(previousEnd);
        RenderSystem.setShaderFogStart(previousStart);
    }

    private void applyFogModifiers(ClientLevel world, FogRenderer.FogData fogData, LocalPlayer player, int cloudDistance, float tickDelta) {
        if (Minecraft.getInstance().gameRenderer == null || Minecraft.getInstance().gameRenderer.getMainCamera() == null) {
            return;
        }

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        FogType cameraSubmersionType = camera.getFluidInCamera();
        if (cameraSubmersionType == FogType.LAVA) {
            if (player.isSpectator()) {
                fogData.start = -8.0f;
                fogData.end = (cloudDistance) * 0.5f;
            } else if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                fogData.start = 0.0f;
                fogData.end = 3.0f;
            } else {
                fogData.start = 0.25f;
                fogData.end = 1.0f;
            }
        } else if (cameraSubmersionType == FogType.POWDER_SNOW) {
            if (player.isSpectator()) {
                fogData.start = -8.0f;
                fogData.end = (cloudDistance) * 0.5f;
            } else {
                fogData.start = 0.0f;
                fogData.end = 2.0f;
            }
        } else if (cameraSubmersionType == FogType.WATER) {
            fogData.start = -8.0f;
            fogData.end = 96.0f;
            fogData.end *= Math.max(0.25f, player.getWaterVision());
            if (fogData.end > (cloudDistance)) {
                fogData.end = cloudDistance;
                fogData.shape = FogShape.CYLINDER;
            }
        } else if (world.effects().isFoggyAt(Mth.floor(camera.getPosition().x), Mth.floor(camera.getPosition().z)) || Minecraft.getInstance().gui.getBossOverlay().shouldCreateWorldFog()) {
            fogData.start = (cloudDistance) * 0.05f;
            fogData.end = Math.min((cloudDistance), 192.0f) * 0.5f;
        }

        FogRenderer.MobEffectFogFunction fogModifier = FogRenderer.getPriorityFogFunction(player, tickDelta);
        if (fogModifier != null) {
            MobEffectInstance statusEffectInstance = player.getEffect(fogModifier.getMobEffect());
            if (statusEffectInstance != null) {
                fogModifier.setupFog(fogData, player, statusEffectInstance, cloudDistance, tickDelta);
            }
        }
    }

    private void rebuildGeometry(BufferBuilder bufferBuilder, int cloudDistance, int centerCellX, int centerCellZ) {
        var writer = VertexBufferWriter.of(bufferBuilder);

        boolean fastClouds = cloudRenderMode == CloudStatus.FAST;

        for (int offsetX = -cloudDistance; offsetX < cloudDistance; offsetX++) {
            for (int offsetZ = -cloudDistance; offsetZ < cloudDistance; offsetZ++) {
                int connectedEdges = this.edges.getEdges(centerCellX + offsetX, centerCellZ + offsetZ);

                if (connectedEdges == 0) {
                    continue;
                }

                int texel = this.edges.getColor(centerCellX + offsetX, centerCellZ + offsetZ);

                float x = offsetX * this.cloudSizeX;
                float z = offsetZ * this.cloudSizeZ;

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    final long buffer = stack.nmalloc((fastClouds ? 4 : (6 * 4)) * ColorVertex.STRIDE);

                    long ptr = buffer;
                    int count = 0;

                    // -Y
                    if ((connectedEdges & DIR_NEG_Y) != 0) {
                        int mixedColor = ColorMixer.mul(texel, fastClouds ? CLOUD_COLOR_POS_Y : CLOUD_COLOR_NEG_Y);

                        ptr = writeVertex(ptr, x + this.cloudSizeX, 0.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + this.cloudSizeX, 0.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // Only emit -Y geometry to emulate vanilla fast clouds
                    if (fastClouds) {
                        writer.push(stack, buffer, count, ColorVertex.FORMAT);
                        continue;
                    }

                    // +Y
                    if ((connectedEdges & DIR_POS_Y) != 0) {
                        int mixedColor = ColorMixer.mul(texel, CLOUD_COLOR_POS_Y);

                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + this.cloudSizeX, 4.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + this.cloudSizeX, 4.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // -X
                    if ((connectedEdges & DIR_NEG_X) != 0) {
                        int mixedColor = ColorMixer.mul(texel, CLOUD_COLOR_NEG_X);

                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // +X
                    if ((connectedEdges & DIR_POS_X) != 0) {
                        int mixedColor = ColorMixer.mul(texel, CLOUD_COLOR_POS_X);

                        ptr = writeVertex(ptr, x + this.cloudSizeX, 4.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + this.cloudSizeX, 0.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + this.cloudSizeX, 0.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + this.cloudSizeX, 4.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // -Z
                    if ((connectedEdges & DIR_NEG_Z) != 0) {
                        int mixedColor = ColorMixer.mul(texel, CLOUD_COLOR_NEG_Z);

                        ptr = writeVertex(ptr, x + this.cloudSizeX, 4.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + this.cloudSizeX, 0.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // +Z
                    if ((connectedEdges & DIR_POS_Z) != 0) {
                        int mixedColor = ColorMixer.mul(texel, CLOUD_COLOR_POS_Z);

                        ptr = writeVertex(ptr, x + this.cloudSizeX, 0.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + this.cloudSizeX, 4.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + this.cloudSizeZ, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + this.cloudSizeZ, mixedColor);

                        count += 4;
                    }

                    if (count > 0) {
                        writer.push(stack, buffer, count, ColorVertex.FORMAT);
                    }
                }
            }
        }
    }

    private static long writeVertex(long buffer, float x, float y, float z, int color) {
        ColorVertex.put(buffer, x, y, z, color);
        return buffer + ColorVertex.STRIDE;
    }

    public void reloadTextures(ResourceProvider factory) {
        this.destroy();

        this.edges = createCloudEdges();

        // FIXME this ugly code is needed because Iris, as usual, duplicates the original code and assumes the old sizes
        // If shaders are on we assume the vanilla size so that the hardcoded values will actually match up with our code.
        // This will be removed when Iris support is dropped.
        boolean shaderMod = ShaderModBridge.areShadersEnabled();
        float width = shaderMod ? 256 : this.edges.width;
        float height = shaderMod ? 256 : this.edges.height;

        this.cloudSizeX = MAX_SINGLE_CLOUD_SIZE / width;
        this.cloudSizeZ = MAX_SINGLE_CLOUD_SIZE / height;
        this.fogDistanceMultiplier = CLOUD_PIXELS_TO_FOG_DISTANCE / width;
        this.cloudDistanceMinimum = (int) (width * CLOUD_PIXELS_TO_MINIMUM_RENDER_DISTANCE);
        this.cloudDistanceMaximum = (int) (width * CLOUD_PIXELS_TO_MAXIMUM_RENDER_DISTANCE);

        try {
            this.shader = new ShaderInstance(factory, "clouds", DefaultVertexFormat.POSITION_COLOR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        if (this.shader != null) {
            this.shader.close();
            this.shader = null;
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }

    private static CloudEdges createCloudEdges() {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        Resource resource = resourceManager.getResource(CLOUDS_TEXTURE_ID)
                .orElseThrow();

        try (InputStream inputStream = resource.open()){
            try (NativeImage nativeImage = NativeImage.read(inputStream)) {
                return new CloudEdges(nativeImage);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load texture data", ex);
        }
    }

    private static class CloudEdges {
        private final byte[] edges;
        private final int[] colors;
        private final int width, height;

        public CloudEdges(NativeImage texture) {
            int width = texture.getWidth();
            int height = texture.getHeight();

            this.edges = new byte[width * height];
            this.colors = new int[width * height];

            this.width = width;
            this.height = height;

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    int index = index(x, z, width, height);
                    int cell = texture.getPixelRGBA(x, z);

                    this.colors[index] = cell;

                    int edges = 0;

                    if (isOpaqueCell(cell)) {
                        edges |= DIR_NEG_Y | DIR_POS_Y;

                        int negX = texture.getPixelRGBA(wrap(x - 1, width), wrap(z, height));

                        if (cell != negX) {
                            edges |= DIR_NEG_X;
                        }

                        int posX = texture.getPixelRGBA(wrap(x + 1, width), wrap(z, height));

                        if (!isOpaqueCell(posX) && cell != posX) {
                            edges |= DIR_POS_X;
                        }

                        int negZ = texture.getPixelRGBA(wrap(x, width), wrap(z - 1, height));

                        if (cell != negZ) {
                            edges |= DIR_NEG_Z;
                        }

                        int posZ = texture.getPixelRGBA(wrap(x, width), wrap(z + 1, height));

                        if (!isOpaqueCell(posZ) && cell != posZ) {
                            edges |= DIR_POS_Z;
                        }
                    }

                    this.edges[index] = (byte) edges;
                }
            }
        }

        private static boolean isOpaqueCell(int color) {
            return ColorARGB.unpackAlpha(color) > 1;
        }

        public int getEdges(int x, int z) {
            return this.edges[index(x, z, this.width, this.height)];
        }

        public int getColor(int x, int z) {
            return this.colors[index(x, z, this.width, this.height)];
        }

        private static int index(int posX, int posZ, int width, int height) {
            return (wrap(posX, width) * height) + wrap(posZ, height);
        }

        private static int wrap(int pos, int dim) {
            return Math.floorMod(pos, dim);
        }
    }
}
