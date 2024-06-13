package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.bus.api.Event;

import java.util.List;
import java.util.function.Function;

public class AddSectionGeometryEvent extends Event {
    public AddSectionGeometryEvent(BlockPos pos, BlockAndTintGetter world) {}

    public List<Renderer> getAdditionalRenderers() {
        return List.of();
    }

    public record SectionRenderingContext(Function<RenderType, VertexConsumer> provider, BlockAndTintGetter world, PoseStack stack) {}

    public interface Renderer {
        void render(SectionRenderingContext context);
    }
}
