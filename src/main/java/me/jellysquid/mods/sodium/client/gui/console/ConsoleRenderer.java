package me.jellysquid.mods.sodium.client.gui.console;

import me.jellysquid.mods.sodium.client.gui.console.message.Message;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Gui;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class ConsoleRenderer {
    static final ConsoleRenderer INSTANCE = new ConsoleRenderer();

    private final LinkedList<ActiveMessage> activeMessages = new LinkedList<>();

    public void update(Console console, double currentTime) {
        this.purgeMessages(currentTime);
        this.pollMessages(console, currentTime);
    }

    private void purgeMessages(double currentTime) {
        this.activeMessages.removeIf(message ->
                currentTime > message.timestamp() + message.duration());
    }

    private void pollMessages(Console console, double currentTime) {
        var log = console.getMessageDrain();

        while (!log.isEmpty()) {
            this.activeMessages.add(ActiveMessage.create(log.poll(), currentTime));
        }
    }

    public void draw(PoseStack matrices) {
        var currentTime = GLFW.glfwGetTime();

        Minecraft client = Minecraft.getInstance();

        matrices.pushPose();
        matrices.translate(0.0f, 0.0f, 1000.0f);


        var paddingWidth = 3;
        var paddingHeight = 1;

        var renders = new ArrayList<MessageRender>();

        {
            int x = 4;
            int y = 4;

            for (ActiveMessage message : this.activeMessages) {
                double opacity = getMessageOpacity(message, currentTime);

                if (opacity < 0.025D) {
                    continue;
                }

                List<FormattedText> lines;

                var messageWidth = 270;

                StringSplitter textHandler = client.font.getSplitter();
                lines = textHandler.splitLines(message.text(), messageWidth - 20, Style.EMPTY, null);

                var messageHeight = (client.font.lineHeight * lines.size()) + (paddingHeight * 2);

                renders.add(new MessageRender(x, y, messageWidth, messageHeight, message.level(), lines, opacity));

                y += messageHeight;
            }
        }

        var mouseX = client.mouseHandler.xpos() / client.getWindow().getGuiScale();
        var mouseY = client.mouseHandler.ypos() / client.getWindow().getGuiScale();

        boolean hovered = false;

        for (var render : renders) {
            if (mouseX >= render.x && mouseX < render.x + render.width && mouseY >= render.y && mouseY < render.y + render.height) {
                hovered = true;
                break;
            }
        }

        for (var render : renders) {
            var x = render.x();
            var y = render.y();

            var width = render.width();
            var height = render.height();

            var colors = COLORS.get(render.level());
            var opacity = render.opacity();

            if (hovered) {
                opacity *= 0.4D;
            }

            // message background
            Gui.fill(matrices, x, y, x + width, y + height,
                    ColorARGB.withAlpha(colors.background(), weightAlpha(opacity)));

            // message colored stripe
            Gui.fill(matrices, x, y, x + 1, y + height,
                    ColorARGB.withAlpha(colors.foreground(), weightAlpha(opacity)));

            for (var line : render.lines()) {
                // message text
                client.font.draw(matrices, line, x + paddingWidth + 3, y + paddingHeight,
                        ColorARGB.withAlpha(colors.text(), weightAlpha(opacity)));

                y += client.font.lineHeight;
            }
        }

        matrices.popPose();
    }

    private static double getMessageOpacity(ActiveMessage message, double time) {
        double midpoint = message.timestamp() + (message.duration() / 2.0D);

        if (time > midpoint) {
            return getFadeOutOpacity(message, time);
        } else if (time < midpoint) {
            return getFadeInOpacity(message, time);
        } else {
            return 1.0D;
        }
    }

    private static double getFadeInOpacity(ActiveMessage message, double time) {
        var animationDuration = 0.25D;

        var animationStart = message.timestamp();
        var animationEnd = message.timestamp() + animationDuration;

        return getAnimationProgress(time, animationStart, animationEnd);
    }

    private static double getFadeOutOpacity(ActiveMessage message, double time) {
        // animation duration is 1/5th the message's duration, or 0.5 seconds, whichever is smaller
        var animationDuration = Math.min(0.5D, message.duration() * 0.20D);

        var animationStart = message.timestamp() + message.duration() - animationDuration;
        var animationEnd = message.timestamp() + message.duration();

        return 1.0D - getAnimationProgress(time, animationStart, animationEnd);
    }

    private static double getAnimationProgress(double currentTime, double startTime, double endTime) {
        return Mth.clamp(Mth.inverseLerp(currentTime, startTime, endTime), 0.0D, 1.0D);
    }

    private static int weightAlpha(double scale) {
        return ColorU8.normalizedFloatToByte((float) scale);
    }

    private record ActiveMessage(MessageLevel level, Component text, double duration, double timestamp) {

        public static ActiveMessage create(Message message, double timestamp) {
            var text = message.text()
                    .copy()
                    .withStyle((style) -> style.withFont(Minecraft.UNIFORM_FONT));

            return new ActiveMessage(message.level(), text, message.duration(), timestamp);
        }
    }

    private static final EnumMap<MessageLevel, ColorPalette> COLORS = new EnumMap<>(MessageLevel.class);

    static {
        COLORS.put(MessageLevel.INFO, new ColorPalette(
                ColorARGB.pack(255, 255, 255),
                ColorARGB.pack( 15,  15,  15),
                ColorARGB.pack( 15,  15,  15)
        ));

        COLORS.put(MessageLevel.WARN, new ColorPalette(
                ColorARGB.pack(224, 187,   0),
                ColorARGB.pack( 25,  21,   0),
                ColorARGB.pack(180, 150,   0)
        ));

        COLORS.put(MessageLevel.SEVERE, new ColorPalette(
                ColorARGB.pack(220,   0,   0),
                ColorARGB.pack( 25,   0,   0),
                ColorARGB.pack(160,   0,   0)
        ));
    }

    private record ColorPalette(int text, int background, int foreground) {

    }

    private record MessageRender(int x, int y, int width, int height, MessageLevel level, List<FormattedText> lines, double opacity) {

    }
}
