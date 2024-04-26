package org.embeddedt.embeddium.chunk;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.LocalSectionIndex;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

import java.util.Iterator;
import java.util.Objects;

public class VanillaShaderChunkRenderer extends DefaultChunkRenderer {

    public VanillaShaderChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);
    }

    private boolean isVanillaPass(TerrainRenderPass pass) {
        return SodiumClientMod.canUseVanillaVertices();
    }

    @Override
    protected void begin(TerrainRenderPass pass) {
        if(isVanillaPass(pass)) {
            pass.startDrawing();
        } else {
            super.begin(pass);
        }
    }

    @Override
    protected void end(TerrainRenderPass pass) {
        if(isVanillaPass(pass)) {
            pass.endDrawing();
        } else {
            super.end(pass);
        }
    }

    private void applyShaderState(ShaderInstance shaderinstance, ChunkRenderMatrices matrices) {
        for(int i = 0; i < 12; ++i) {
            int j1 = RenderSystem.getShaderTexture(i);
            shaderinstance.setSampler("Sampler" + i, j1);
        }

        if (shaderinstance.MODEL_VIEW_MATRIX != null) {
            shaderinstance.MODEL_VIEW_MATRIX.set((Matrix4f)matrices.modelView());
        }

        if (shaderinstance.PROJECTION_MATRIX != null) {
            shaderinstance.PROJECTION_MATRIX.set((Matrix4f)matrices.projection());
        }

        if (shaderinstance.COLOR_MODULATOR != null) {
            shaderinstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        if (shaderinstance.GLINT_ALPHA != null) {
            shaderinstance.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }

        if (shaderinstance.FOG_START != null) {
            shaderinstance.FOG_START.set(RenderSystem.getShaderFogStart());
        }

        if (shaderinstance.FOG_END != null) {
            shaderinstance.FOG_END.set(RenderSystem.getShaderFogEnd());
        }

        if (shaderinstance.FOG_COLOR != null) {
            shaderinstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }

        if (shaderinstance.FOG_SHAPE != null) {
            shaderinstance.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }

        if (shaderinstance.TEXTURE_MATRIX != null) {
            shaderinstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (shaderinstance.GAME_TIME != null) {
            shaderinstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        RenderSystem.setupShaderLights(shaderinstance);
        shaderinstance.apply();
    }

    private void renderRegion(TerrainRenderPass renderPass, Uniform chunkOffset, ChunkRenderList renderList, CameraTransform camera, CommandList commandList) {
        var region = renderList.getRegion();
        var storage = region.getStorage(renderPass);

        if(storage == null) {
            return;
        }

        var sectionIterator = renderList.sectionsWithGeometryIterator(renderPass.isReverseOrder());

        if(sectionIterator == null) {
            return;
        }

        boolean useBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;

        var batch = this.batch;
        var indexBuffer = this.sharedIndexBuffer;

        while(sectionIterator.hasNext()) {
            int sectionIndex = sectionIterator.nextByteAsInt();

            int originX = region.getChunkX() + LocalSectionIndex.unpackX(sectionIndex);
            int originY = region.getChunkY() + LocalSectionIndex.unpackY(sectionIndex);
            int originZ = region.getChunkZ() + LocalSectionIndex.unpackZ(sectionIndex);

            var pMeshData = storage.getDataPointer(sectionIndex);

            if (chunkOffset != null) {
                GL30C.glUniform3f(chunkOffset.getLocation(), (float)((double)(originX << 4) - camera.x), (float)((double)(originY << 4) - camera.y), (float)((double)(originZ << 4) - camera.z));
            }

            int slices;

            if (useBlockFaceCulling && (!renderPass.isReverseOrder() || !SodiumClientMod.canApplyTranslucencySorting())) {
                slices = getVisibleFaces(camera.intX, camera.intY, camera.intZ, originX, originY, originZ);
            } else {
                slices = ModelQuadFacing.ALL;
            }

            slices &= SectionRenderDataUnsafe.getSliceMask(pMeshData);

            if (slices != 0) {
                // Draw this section
                addDrawCommands(batch, pMeshData, slices);
                indexBuffer.ensureCapacity(commandList, batch.getIndexBufferSize());
                var tessellation = this.prepareTessellation(commandList, region);
                executeDrawBatch(commandList, tessellation, batch);
                batch.clear();
            }
        }
    }

    @Override
    public void render(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass renderPass, CameraTransform camera) {
        if(isVanillaPass(renderPass)) {
            begin(renderPass);
            ShaderInstance shaderinstance = Objects.requireNonNull(RenderSystem.getShader(), "Shader not bound");

            applyShaderState(shaderinstance, matrices);

            Uniform chunkOffset = shaderinstance.CHUNK_OFFSET;

            Iterator<ChunkRenderList> iterator = renderLists.iterator(renderPass.isReverseOrder());

            while (iterator.hasNext()) {
                ChunkRenderList renderList = iterator.next();

                renderRegion(renderPass, chunkOffset, renderList, camera, commandList);
            }

            if(chunkOffset != null) {
                chunkOffset.set(0f, 0f, 0f);
                GL30C.glUniform3f(chunkOffset.getLocation(), 0f, 0f, 0f);
            }

            shaderinstance.clear();

            end(renderPass);
        } else {
            super.render(matrices, commandList, renderLists, renderPass, camera);
        }
    }
}
