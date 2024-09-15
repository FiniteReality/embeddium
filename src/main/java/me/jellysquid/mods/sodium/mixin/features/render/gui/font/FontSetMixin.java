package me.jellysquid.mods.sodium.mixin.features.render.gui.font;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntFunction;

@Mixin(value = FontSet.class, priority = 500)
public abstract class FontSetMixin {
    @Shadow
    protected abstract FontSet.GlyphInfoFilter computeGlyphInfo(int p_243321_);

    @Shadow
    protected abstract BakedGlyph computeBakedGlyph(int p_232565_);

    /**
     * @author embeddedt
     * @reason avoid lambda allocation from method reference in vanilla
     */
    @Redirect(method = "getGlyphInfo", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;computeIfAbsent(ILit/unimi/dsi/fastutil/ints/Int2ObjectFunction;)Ljava/lang/Object;"))
    private Object getGlyphInfoFast(Int2ObjectMap<FontSet.GlyphInfoFilter> instance, int i, Int2ObjectFunction<FontSet.GlyphInfoFilter> methodRef) {
        FontSet.GlyphInfoFilter info = instance.get(i);

        if (info == null) {
            info = this.computeGlyphInfo(i);
            instance.put(i, info);
        }

        return info;
    }

    /**
     * @author embeddedt
     * @reason avoid lambda allocation from method reference in vanilla
     */
    @Redirect(method = "getGlyph", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;computeIfAbsent(ILit/unimi/dsi/fastutil/ints/Int2ObjectFunction;)Ljava/lang/Object;"))
    private Object getGlyphFast(Int2ObjectMap<BakedGlyph> instance, int i, Int2ObjectFunction<BakedGlyph> methodRef) {
        BakedGlyph glyph = instance.get(i);

        if (glyph == null) {
            glyph = this.computeBakedGlyph(i);
            instance.put(i, glyph);
        }

        return glyph;
    }

}
