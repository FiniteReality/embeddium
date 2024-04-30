package org.embeddedt.embeddium.gui.frame.tab;

import com.mojang.blaze3d.platform.NativeImage;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TabHeaderWidget extends FlatButtonWidget {
    private static final Set<String> erroredLogos = new HashSet<>();
    private final ResourceLocation logoTexture;

    private static Component getLabel(String modId) {
        return switch(modId) {
            // TODO handle long mod names better, this is the only one we know of right now
            case "sspb" -> Component.literal("SSPB");
            default -> Tab.idComponent(modId);
        };
    }

    public TabHeaderWidget(Dim2i dim, String modId) {
        super(dim, getLabel(modId), () -> {});
        Optional<Path> logoFile = erroredLogos.contains(modId) ? Optional.empty() : FabricLoader.getInstance().getModContainer(modId).flatMap(c -> c.getMetadata().getIconPath(32).flatMap(c::findPath));
        ResourceLocation texture = null;
        if(logoFile.isPresent()) {
            try(InputStream is = Files.newInputStream(logoFile.get())) {
                if (is != null) {
                    NativeImage logo = NativeImage.read(is);
                    if(logo.getWidth() != logo.getHeight()) {
                        logo.close();
                        throw new IOException("Logo " + logoFile.get() + " for " + modId + " is not square");
                    }
                    texture = new ResourceLocation(SodiumClientMod.MODID, "logo/" + modId);
                    Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(logo));
                }
            } catch(IOException e) {
                erroredLogos.add(modId);
                SodiumClientMod.logger().error("Exception reading logo for " + modId, e);
            }
        }
        this.logoTexture = texture;
    }

    @Override
    protected int getLeftAlignedTextOffset() {
        return super.getLeftAlignedTextOffset() + Minecraft.getInstance().font.lineHeight;
    }

    @Override
    protected boolean isHovered(int mouseX, int mouseY) {
        return false;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        if(this.logoTexture != null) {
            int fontHeight = Minecraft.getInstance().font.lineHeight;
            int imgY = this.dim.getCenterY() - (fontHeight / 2);
            drawContext.blit(this.logoTexture, this.dim.x() + 5, imgY, 0.0f, 0.0f, fontHeight, fontHeight, fontHeight, fontHeight);
        }
    }
}
