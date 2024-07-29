package me.jellysquid.mods.sodium.mixin.features.render.gui.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.RawGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntFunction;

@Mixin(value = FontSet.class, priority = 500)
public abstract class FontSetMixin {
    @Shadow
    @Final
    private static GlyphInfo SPACE_INFO;

    @Shadow
    protected abstract RawGlyph getRaw(int i);

    @Shadow
    protected abstract BakedGlyph stitch(RawGlyph glyphInfo);

    @Shadow
    @Final
    private static EmptyGlyph SPACE_GLYPH;

    /**
     * @author embeddedt
     * @reason avoid lambda allocation from method reference in vanilla
     */
    @Redirect(method = "getGlyphInfo", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;computeIfAbsent(ILjava/util/function/IntFunction;)Ljava/lang/Object;"))
    private Object getGlyphInfoFast(Int2ObjectMap<GlyphInfo> instance, int i, IntFunction<GlyphInfo> methodRef) {
        GlyphInfo info = instance.get(i);

        if (info == null) {
            info = i == 32 ? SPACE_INFO : this.getRaw(i);
            instance.put(i, info);
        }

        return info;
    }

    /**
     * @author embeddedt
     * @reason avoid lambda allocation from method reference in vanilla
     */
    @Redirect(method = "getGlyph", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;computeIfAbsent(ILjava/util/function/IntFunction;)Ljava/lang/Object;"))
    private Object getGlyphFast(Int2ObjectMap<BakedGlyph> instance, int i, IntFunction<BakedGlyph> methodRef) {
        BakedGlyph glyph = instance.get(i);

        if (glyph == null) {
            glyph = i == 32 ? SPACE_GLYPH : this.stitch(this.getRaw(i));
            instance.put(i, glyph);
        }

        return glyph;
    }

}
