package org.embeddedt.embeddium.gui.frame;

import com.google.common.base.Predicates;
import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.lang3.Validate;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;
import org.embeddedt.embeddium.gui.theme.DefaultColors;
import org.embeddedt.embeddium.util.PlatformUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class OptionPageFrame extends AbstractFrame {
    protected final OptionPage page;
    private long lastTime = 0;
    private ControlElement<?> lastHoveredElement = null;
    protected final Predicate<Option<?>> optionFilter;

    public OptionPageFrame(Dim2i dim, boolean renderOutline, OptionPage page, Predicate<Option<?>> optionFilter) {
        super(dim, renderOutline);
        this.page = page;
        this.optionFilter = optionFilter;
        this.setupFrame();
        this.buildFrame();
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public void setupFrame() {
        this.children.clear();
        this.drawable.clear();
        this.controlElements.clear();

        int y = 0;
        if (!this.page.getGroups().isEmpty()) {
            OptionGroup lastGroup = this.page.getGroups().get(this.page.getGroups().size() - 1);

            for (OptionGroup group : this.page.getGroups()) {
                int visibleOptionCount = (int)group.getOptions().stream().filter(optionFilter::test).count();
                y += visibleOptionCount * 18;
                if (visibleOptionCount > 0 && group != lastGroup) {
                    y += 4;
                }
            }
        }

        this.dim = this.dim.withHeight(y);
    }

    @Override
    public void buildFrame() {
        if (this.page == null) return;

        this.children.clear();
        this.drawable.clear();
        this.controlElements.clear();

        int y = 0;
        for (OptionGroup group : this.page.getGroups()) {
            boolean needPadding = false;
            // Add each option's control element
            for (Option<?> option : group.getOptions()) {
                if(!optionFilter.test(option)) {
                    continue;
                }
                Control<?> control = option.getControl();
                Dim2i dim = new Dim2i(0, y, this.dim.width(), 18).withParentOffset(this.dim);
                ControlElement<?> element = control.createElement(dim);
                this.children.add(element);

                // Move down to the next option
                y += 18;
                needPadding = true;
            }

            if(needPadding) {
                // Add padding beneath each option group
                y += 4;
            }
        }

        super.buildFrame();
    }

    @Override
    public void render(PoseStack drawContext, int mouseX, int mouseY, float delta) {
        ControlElement<?> hoveredElement = this.controlElements.stream()
                .filter(ControlElement::isHovered)
                .findFirst()
                .orElse(this.controlElements.stream()
                        .filter(ControlElement::isFocused)
                        .findFirst()
                        .orElse(null));
        super.render(drawContext, mouseX, mouseY, delta);
        if (hoveredElement != null && this.lastHoveredElement == hoveredElement &&
                ((this.dim.containsCursor(mouseX, mouseY) && hoveredElement.isHovered() && hoveredElement.isMouseOver(mouseX, mouseY))
                        || hoveredElement.isFocused())) {
            if (this.lastTime == 0) {
                this.lastTime = System.currentTimeMillis();
            }
            this.renderOptionTooltip(drawContext, hoveredElement);
        } else {
            this.lastTime = 0;
            this.lastHoveredElement = hoveredElement;
        }
    }

    private static String normalizeModForTooltip(@Nullable String mod) {
        if(mod == null) {
            return null;
        } else {
            return switch(mod) {
                case "minecraft" -> "embeddium";
                default -> mod;
            };
        }
    }

    private void renderOptionTooltip(PoseStack drawContext, ControlElement<?> element) {
        if (this.lastTime + 500 > System.currentTimeMillis()) return;

        Dim2i dim = element.getDimensions();

        int textPadding = 3;
        int boxPadding = 3;

        int boxWidth = dim.width();

        //Offset based on mouse position, width and height of content and width and height of the window
        int boxY = dim.getLimitY();
        int boxX = dim.x();

        Option<?> option = element.getOption();
        List<FormattedText> tooltip = new ArrayList<>(Minecraft.getInstance().font.split(option.getTooltip(), boxWidth - (textPadding * 2)));

        OptionImpact impact = option.getImpact();

        if (impact != null) {
            tooltip.add(new TranslatableComponent("sodium.options.performance_impact_string", impact.getLocalizedName()).withStyle(ChatFormatting.GRAY));
        }

        var id = option.getId();

        if (OptionIdentifier.isPresent(page.getId()) && OptionIdentifier.isPresent(id) && !Objects.equals(normalizeModForTooltip(page.getId().getModId()), normalizeModForTooltip(id.getModId()))) {
            tooltip.add(new TranslatableComponent("embeddium.options.added_by_mod_string", new TextComponent(PlatformUtil.getModName(id.getModId())).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        }

        int boxHeight = (tooltip.size() * 12) + boxPadding;
        int boxYLimit = boxY + boxHeight;
        int boxYCutoff = this.dim.getLimitY();

        // If the box is going to be cutoff on the Y-axis, move it back up the difference
        if (boxYLimit > boxYCutoff) {
            boxY -= boxHeight + dim.height();
        }

        if (boxY < 0) {
            boxY = dim.getLimitY();
        }

        drawContext.pushPose();
        drawContext.translate(0, 0, 90);
        this.drawRect(drawContext, boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);
        this.drawBorder(drawContext, boxX, boxY, boxX + boxWidth, boxY + boxHeight, DefaultColors.ELEMENT_ACTIVATED);

        for (int i = 0; i < tooltip.size(); i++) {
            Minecraft.getInstance().font.draw(drawContext, tooltip.get(i), boxX + textPadding, boxY + textPadding + (i * 12), 0xFFFFFFFF);
        }
        drawContext.popPose();
    }

    public static class Builder {
        private Dim2i dim;
        private boolean renderOutline;
        private OptionPage page;
        private Predicate<Option<?>> optionFilter = Predicates.alwaysTrue();

        public Builder setDimension(Dim2i dim) {
            this.dim = dim;
            return this;
        }

        public Builder shouldRenderOutline(boolean renderOutline) {
            this.renderOutline = renderOutline;
            return this;
        }

        public Builder setOptionPage(OptionPage page) {
            this.page = page;
            return this;
        }

        public Builder setOptionFilter(Predicate<Option<?>> optionFilter) {
            this.optionFilter = optionFilter;
            return this;
        }

        public OptionPageFrame build() {
            Validate.notNull(this.dim, "Dimension must be specified");
            Validate.notNull(this.page, "Option Page must be specified");

            return new OptionPageFrame(this.dim, this.renderOutline, this.page, this.optionFilter);
        }
    }
}
