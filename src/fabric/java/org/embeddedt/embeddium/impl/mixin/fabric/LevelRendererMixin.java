package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LevelRenderer.class, priority = 100)
public class LevelRendererMixin {
    @Shadow
    @Nullable
    private Frustum capturedFrustum;

    @Shadow
    private Frustum cullingFrustum;

    public Frustum getFrustum() {
        return this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
    }

    public void iterateVisibleBlockEntities(java.util.function.Consumer<BlockEntity> consumer) {}
}
