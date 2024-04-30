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
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.resource.ResourcePackLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class TabHeaderWidget extends FlatButtonWidget {
    private static final ResourceLocation FALLBACK_LOCATION = new ResourceLocation("textures/misc/unknown_pack.png");

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
            final PathPackResources resourcePack = ResourcePackLoader.getPackFor(modId)
                    .orElse(ResourcePackLoader.getPackFor("forge").
                            orElseThrow(()->new RuntimeException("Can't find forge, WHAT!")));
            try {
                IoSupplier<InputStream> logoResource = resourcePack.getRootResource(logoFile.get());
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

        ResourceLocation icon = Objects.requireNonNullElse(this.logoTexture, FALLBACK_LOCATION);
        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int imgY = this.dim.getCenterY() - (fontHeight / 2);
        drawContext.blit(icon, this.dim.x() + 5, imgY, 0.0f, 0.0f, fontHeight, fontHeight, fontHeight, fontHeight);
    }
}
