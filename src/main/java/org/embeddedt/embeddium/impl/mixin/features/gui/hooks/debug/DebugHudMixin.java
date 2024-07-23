package org.embeddedt.embeddium.impl.mixin.features.gui.hooks.debug;

import com.google.common.collect.Lists;
import org.embeddedt.embeddium.impl.Embeddium;
import org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer;
import org.embeddedt.embeddium.impl.util.MathUtil;
import org.embeddedt.embeddium.impl.util.NativeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.embeddedt.embeddium_integrity.MixinTaintDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;

import static org.embeddedt.embeddium.impl.Embeddium.MODNAME;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudMixin {
    @Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;", remap = false))
    private ArrayList<String> redirectRightTextEarly(Object[] elements) {
        ArrayList<String> strings = Lists.newArrayList((String[]) elements);
        strings.add("");
        strings.add("%s%s Renderer (%s)".formatted(MixinTaintDetector.getTaintingMods().isEmpty() ? ChatFormatting.GREEN : ChatFormatting.RED, MODNAME, Embeddium.getVersion()));

        // Embeddium: Show a lot less with reduced debug info
        if(Minecraft.getInstance().showOnlyReducedInfo()) {
           return strings;
        }

        var renderer = EmbeddiumWorldRenderer.instanceNullable();

        if (renderer != null) {
            strings.addAll(renderer.getDebugStrings());
        }

        for (int i = 0; i < strings.size(); i++) {
            String str = strings.get(i);

            if (str.startsWith("Allocated:")) {
                strings.add(i + 1, getNativeMemoryString());

                break;
            }
        }

        return strings;
    }

    @Unique
    private static String getNativeMemoryString() {
        return "Off-Heap: +" + MathUtil.toMib(getNativeMemoryUsage()) + "MB";
    }

    @Unique
    private static long getNativeMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() + NativeBuffer.getTotalAllocated();
    }
}
