package me.jellysquid.mods.sodium.client.gui.screen;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConfigCorruptedScreen extends Screen {
    private static final String TEXT_BODY_RAW = """
        A problem occurred while trying to load the configuration file. This
        can happen when the file has been corrupted on disk, or when trying
        to manually edit the file by hand.
        
        We can attempt to fix this problem automatically by restoring the
        config file back to known-good defaults, but you will lose any
        changes that have since been made to your video settings.
        
        More information about the error can be found in the log file.
        """;

    private static final List<Component> TEXT_BODY = Arrays.stream(TEXT_BODY_RAW.split("\n"))
            .map(Component::literal)
            .collect(Collectors.toList());

    private static final Component TEXT_BUTTON_RESTORE_DEFAULTS = Component.literal("Restore defaults");
    private static final Component TEXT_BUTTON_CLOSE_GAME = Component.literal("Close game");

    private final Supplier<Screen> child;

    public ConfigCorruptedScreen(Supplier<Screen> child) {
        super(Component.literal("Config corruption detected"));

        this.child = child;
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(Button.builder(TEXT_BUTTON_RESTORE_DEFAULTS, (btn) -> {
            SodiumClientMod.restoreDefaultOptions();
            Minecraft.getInstance().setScreen(this.child.get());
        }).bounds(32, this.height - 40, 174, 20).build());

        this.addRenderableWidget(Button.builder(TEXT_BUTTON_CLOSE_GAME, (btn) -> {
            Minecraft.getInstance().stop();
        }).bounds(this.width - 174 - 32, this.height - 40, 174, 20).build());
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackground(drawContext);

        super.render(drawContext, mouseX, mouseY, delta);

        drawContext.drawString(this.font, Component.literal("Sodium Renderer"), 32, 32, 0xffffff);
        drawContext.drawString(this.font, Component.literal("Could not load configuration file"), 32, 48, 0xff0000);

        for (int i = 0; i < TEXT_BODY.size(); i++) {
            if (TEXT_BODY.get(i).getString().isEmpty()) {
                continue;
            }

            drawContext.drawString(this.font, TEXT_BODY.get(i), 32, 68 + (i * 12), 0xffffff);
        }
    }
}
