package org.embeddedt.embeddium.gui.frame.tab;

import com.mojang.blaze3d.platform.NativeImage;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.resource.ResourcePackLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TabHeaderWidget extends FlatButtonWidget {
    private static final Set<String> erroredLogos = new HashSet<>();
    private final ResourceLocation logoTexture;

    public static MutableComponent getLabel(String modId) {
        return (switch(modId) {
            // TODO handle long mod names better, this is the only one we know of right now
            case "sspb" -> Component.literal("SSPB");
            default -> Tab.idComponent(modId);
        }).withStyle(s -> s.withUnderlined(true));
    }

    public TabHeaderWidget(Dim2i dim, String modId) {
        super(dim, getLabel(modId), () -> {});
        Optional<String> logoFile = erroredLogos.contains(modId) ? Optional.empty() : ModList.get().getModContainerById(modId).flatMap(c -> c.getModInfo().getLogoFile());
        ResourceLocation texture = null;
        if(logoFile.isPresent()) {
            final Pack.ResourcesSupplier supplier = ResourcePackLoader.getPackFor(modId).orElse(ResourcePackLoader.getPackFor("neoforge").orElseThrow(()->new RuntimeException("Can't find neoforge, WHAT!")));
            try(PackResources pack = supplier.openPrimary(new PackLocationInfo("mod:" + modId, Component.empty(), PackSource.BUILT_IN, Optional.empty()))) {
                IoSupplier<InputStream> logoResource = pack.getRootResource(logoFile.get());
                if (logoResource != null) {
                    NativeImage logo = NativeImage.read(logoResource.get());
                    if(logo.getWidth() != logo.getHeight()) {
                        logo.close();
                        throw new IOException("Logo " + logoFile.get() + " for " + modId + " is not square");
                    }
                    texture = new ResourceLocation(SodiumClientMod.MODID, "logo/" + modId);
                    Minecraft.getInstance().textureManager.register(texture, new DynamicTexture(logo));
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
