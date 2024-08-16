package org.embeddedt.embeddium.gui.frame;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractFrame extends AbstractWidget implements ContainerEventHandler {
    protected Dim2i dim;
    protected final List<AbstractWidget> children = new ArrayList<>();
    protected final List<Renderable> drawable = new ArrayList<>();
    protected final List<ControlElement<?>> controlElements = new ArrayList<>();
    protected boolean renderOutline;
    private GuiEventListener focused;
    private boolean dragging;
    private Consumer<GuiEventListener> focusListener;

    public AbstractFrame(Dim2i dim, boolean renderOutline) {
        this.dim = dim;
        this.renderOutline = renderOutline;
    }

    public void buildFrame() {
        for (GuiEventListener element : this.children) {
            if (element instanceof AbstractFrame) {
                this.controlElements.addAll(((AbstractFrame) element).controlElements);
            }
            if (element instanceof ControlElement<?>) {
                this.controlElements.add((ControlElement<?>) element);
            }
            if (element instanceof Renderable) {
                this.drawable.add((Renderable) element);
            }
        }
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (this.renderOutline) {
            this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), 0xFFAAAAAA);
        }
        for (Renderable drawable : this.drawable) {
            drawable.render(drawContext, mouseX, mouseY, delta);
        }
    }

    @Deprecated
    public void applyScissor(int x, int y, int width, int height, Runnable action) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        RenderSystem.enableScissor((int) (x * scale), (int) (Minecraft.getInstance().getWindow().getHeight() - (y + height) * scale),
                (int) (width * scale), (int) (height * scale));
        action.run();
        RenderSystem.disableScissor();
    }

    public void registerFocusListener(Consumer<GuiEventListener> focusListener) {
        this.focusListener = focusListener;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        this.focused = focused;
        if (this.focusListener != null) {
            this.focusListener.accept(focused);
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        return ContainerEventHandler.super.nextFocusPath(navigation);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }

    public Dim2i getDimensions() {
        return this.dim;
    }
}
