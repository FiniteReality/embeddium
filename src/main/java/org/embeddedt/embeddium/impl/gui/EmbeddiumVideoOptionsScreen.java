package org.embeddedt.embeddium.impl.gui;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.embeddedt.embeddium.impl.Embeddium;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.impl.data.fingerprint.HashedFingerprint;
import org.embeddedt.embeddium.impl.gui.console.Console;
import org.embeddedt.embeddium.impl.gui.console.message.MessageLevel;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.gui.widgets.FlatButtonWidget;
import org.embeddedt.embeddium.api.math.Dim2i;
import org.embeddedt.embeddium.api.util.ColorARGB;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.frame.AbstractFrame;
import org.embeddedt.embeddium.impl.gui.frame.BasicFrame;
import org.embeddedt.embeddium.impl.gui.frame.components.SearchTextFieldComponent;
import org.embeddedt.embeddium.impl.gui.frame.components.SearchTextFieldModel;
import org.embeddedt.embeddium.impl.gui.frame.tab.Tab;
import org.embeddedt.embeddium.impl.gui.frame.tab.TabFrame;
import org.embeddedt.embeddium.impl.gui.screen.PromptScreen;
import org.embeddedt.embeddium.impl.gui.theme.DefaultColors;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.util.PlatformUtil;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class EmbeddiumVideoOptionsScreen extends Screen {
    private static final ResourceLocation LOGO_LOCATION = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "textures/embeddium/gui/logo_transparent.png");
    private static final int LOGO_SIZE = 256;

    private static final AtomicReference<Component> tabFrameSelectedTab = new AtomicReference<>(null);
    private final AtomicReference<Integer> tabFrameScrollBarOffset = new AtomicReference<>(0);
    private final AtomicReference<Integer> optionPageScrollBarOffset = new AtomicReference<>(0);

    private final Screen prevScreen;
    private final List<OptionPage> pages = new ArrayList<>();
    private AbstractFrame frame;
    private FlatButtonWidget applyButton, closeButton, undoButton;
    private FlatButtonWidget donateButton, hideDonateButton;

    private Dim2i logoDim;

    private boolean hasPendingChanges;

    private SearchTextFieldComponent searchTextField;
    private final SearchTextFieldModel searchTextModel;

    private boolean firstInit = true;

    public EmbeddiumVideoOptionsScreen(Screen prev, List<OptionPage> pages) {
        super(Component.literal("Embeddium Options"));
        this.prevScreen = prev;
        this.pages.addAll(pages);
        this.searchTextModel = new SearchTextFieldModel(this.pages, this);
        registerTextures();
    }

    public static List<OptionPage> makePages() {
        List<OptionPage> pages = new ArrayList<>();

        pages.add(EmbeddiumGameOptionPages.general());
        pages.add(EmbeddiumGameOptionPages.quality());
        pages.add(EmbeddiumGameOptionPages.performance());
        pages.add(EmbeddiumGameOptionPages.advanced());

        OptionGUIConstructionEvent.BUS.post(new OptionGUIConstructionEvent(pages));

        return List.copyOf(pages);
    }

    private void checkPromptTimers() {
        // Don't show the donation prompt in situations where we know it causes problems.
        if (PlatformUtil.isDevelopmentEnvironment()) {
            return;
        }

        var options = Embeddium.options();

        // If the user has disabled the nags forcefully (by config), or has already seen the prompt, don't show it again.
        if (options.notifications.forceDisableDonationPrompts || options.notifications.hasSeenDonationPrompt) {
            return;
        }

        HashedFingerprint fingerprint = null;

        try {
            fingerprint = HashedFingerprint.loadFromDisk();
        } catch (Throwable t) {
            Embeddium.logger()
                    .error("Failed to read the fingerprint from disk", t);
        }

        // If the fingerprint doesn't exist, or failed to be loaded, abort.
        if (fingerprint == null) {
            return;
        }

        // The fingerprint records the installation time. If it's been a while since installation, show the user
        // a prompt asking for them to consider donating.
        var now = Instant.now();
        var threshold = Instant.ofEpochSecond(fingerprint.timestamp())
                .plus(3, ChronoUnit.DAYS);

        if (now.isAfter(threshold)) {
            this.openDonationPrompt();

            options.notifications.hasSeenDonationPrompt = true;

            try {
                EmbeddiumOptions.writeToDisk(options);
            } catch (IOException e) {
                Embeddium.logger()
                        .error("Failed to update config file", e);
            }
        }
    }

    private void openDonationPrompt() {
        var prompt = new PromptScreen(this, DONATION_PROMPT_MESSAGE, 320, 190,
                new PromptScreen.Action(Component.literal("Support Sodium"), this::openDonationPage));

        this.minecraft.setScreen(prompt);
    }


    private void registerTextures() {
        Minecraft.getInstance().getTextureManager().register(LOGO_LOCATION, new SimpleTexture(LOGO_LOCATION));
    }


    public void rebuildUI() {
        // Remember if the search bar was previously focused since we'll lose that information after recreating
        // the widget.
        boolean wasSearchFocused = this.searchTextField.isFocused();
        this.rebuildWidgets();
        if(wasSearchFocused) {
            this.setFocused(this.searchTextField);
        }
    }

    @Override
    protected void init() {
        this.frame = this.parentFrameBuilder().build();
        this.addRenderableWidget(this.frame);

        this.setFocused(this.frame);

        if(firstInit) {
            this.setFocused(this.searchTextField);
            firstInit = false;
            this.checkPromptTimers();
        }
    }

    private static final float ASPECT_RATIO = 5f / 4f;
    private static final int MINIMUM_WIDTH = 550;

    protected BasicFrame.Builder parentFrameBuilder() {
        BasicFrame.Builder basicFrameBuilder;

        // Apply aspect ratio clamping on wide enough screens
        int newWidth = this.width;
        if (newWidth > MINIMUM_WIDTH && (float) this.width / (float) this.height > ASPECT_RATIO) {
            newWidth = Math.max(MINIMUM_WIDTH, (int) (this.height * ASPECT_RATIO));
        }

        Dim2i basicFrameDim = new Dim2i((this.width - newWidth) / 2, 0, newWidth, this.height);
        Dim2i tabFrameDim = new Dim2i(basicFrameDim.x() + basicFrameDim.width() / 20 / 2, basicFrameDim.y() + basicFrameDim.height() / 4 / 2, basicFrameDim.width() - (basicFrameDim.width() / 20), basicFrameDim.height() / 4 * 3);

        Dim2i undoButtonDim = new Dim2i(tabFrameDim.getLimitX() - 203, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i applyButtonDim = new Dim2i(tabFrameDim.getLimitX() - 134, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i closeButtonDim = new Dim2i(tabFrameDim.getLimitX() - 65, tabFrameDim.getLimitY() + 5, 65, 20);

        Component donationText = Component.translatable("sodium.options.buttons.donate");
        int donationTextWidth = this.minecraft.font.width(donationText);

        Dim2i donateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 32 - donationTextWidth, tabFrameDim.y() - 26, 10 + donationTextWidth, 20);
        Dim2i hideDonateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 20, tabFrameDim.y() - 26, 20, 20);

        int logoSizeOnScreen = 20;
        this.logoDim = new Dim2i(tabFrameDim.x(), tabFrameDim.getLimitY() + 25 - logoSizeOnScreen, logoSizeOnScreen, logoSizeOnScreen);

        this.undoButton = new FlatButtonWidget(undoButtonDim, Component.translatable("sodium.options.buttons.undo"), this::undoChanges);
        this.applyButton = new FlatButtonWidget(applyButtonDim, Component.translatable("sodium.options.buttons.apply"), this::applyChanges);
        this.closeButton = new FlatButtonWidget(closeButtonDim, Component.translatable("gui.done"), this::onClose);

        this.donateButton = new FlatButtonWidget(donateButtonDim, donationText, this::openDonationPage);
        this.hideDonateButton = new FlatButtonWidget(hideDonateButtonDim, Component.literal("x"), this::hideDonationButton);

        if (Embeddium.options().notifications.hasClearedDonationButton) {
            this.setDonationButtonVisibility(false);
        }

        Dim2i searchTextFieldDim;
        if (Embeddium.options().notifications.hasClearedDonationButton) {
            searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width(), 20);
        } else {
            searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width() - (tabFrameDim.getLimitX() - donateButtonDim.x()) - 2, 20);
        }

        basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);

        this.searchTextField = new SearchTextFieldComponent(searchTextFieldDim, this.pages, this.searchTextModel);

        basicFrameBuilder.addChild(dim -> this.searchTextField);

        return basicFrameBuilder;
    }

    private boolean canShowPage(OptionPage page) {
        if(page.getGroups().isEmpty()) {
            return false;
        }

        // Check if any options on this page are visible
        var predicate = searchTextModel.getOptionPredicate();

        for(OptionGroup group : page.getGroups()) {
            for(Option<?> option : group.getOptions()) {
                if(predicate.test(option)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void createShaderPackButton(Multimap<String, Tab<?>> tabs) {
        if(this.searchTextModel.getOptionPredicate().test(null) && ShaderModBridge.isShaderModPresent()) {
            String shaderModId = Stream.of("oculus", "iris").filter(PlatformUtil::modPresent).findFirst().orElse("iris");
            tabs.put(shaderModId, Tab.createBuilder()
                    .setTitle(Component.translatable("options.iris.shaderPackSelection"))
                    .setId(OptionIdentifier.create("iris", "shader_packs"))
                    .setOnSelectFunction(() -> {
                        if(ShaderModBridge.openShaderScreen(this) instanceof Screen screen) {
                            this.minecraft.setScreen(screen);
                        }
                        return false;
                    })
                    .build());
        }
    }

    private AbstractFrame createTabFrame(Dim2i tabFrameDim) {
        // TabFrame will automatically expand its height to fit all tabs, so the scrollable frame can handle it
        return TabFrame.createBuilder()
                .setDimension(tabFrameDim)
                .shouldRenderOutline(false)
                .setTabSectionScrollBarOffset(tabFrameScrollBarOffset)
                .setTabSectionSelectedTab(tabFrameSelectedTab)
                .addTabs(tabs -> this.pages
                        .stream()
                        .filter(this::canShowPage)
                        .forEach(page -> tabs.put(page.getId().getModId(), Tab.createBuilder().from(page, searchTextModel.getOptionPredicate(), optionPageScrollBarOffset)))
                )
                .addTabs(this::createShaderPackButton)
                .onSetTab(() -> {
                    optionPageScrollBarOffset.set(0);
                })
                .build();
    }

    public BasicFrame.Builder parentBasicFrameBuilder(Dim2i parentBasicFrameDim, Dim2i tabFrameDim) {
        return BasicFrame.createBuilder()
                .setDimension(parentBasicFrameDim)
                .shouldRenderOutline(false)
                .addChild(dim -> this.donateButton)
                .addChild(dim -> this.hideDonateButton)
                .addChild(parentDim -> this.createTabFrame(tabFrameDim))
                .addChild(dim -> this.undoButton)
                .addChild(dim -> this.applyButton)
                .addChild(dim -> this.closeButton);
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gfx, mouseX, mouseY, partialTick);

        // Render watermarks
        gfx.setColor(ColorARGB.unpackRed(DefaultColors.ELEMENT_ACTIVATED) / 255f, ColorARGB.unpackGreen(DefaultColors.ELEMENT_ACTIVATED) / 255f, ColorARGB.unpackBlue(DefaultColors.ELEMENT_ACTIVATED) / 255f, 0.8F);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        gfx.blit(LOGO_LOCATION, this.logoDim.x(), this.logoDim.y(), this.logoDim.width(), this.logoDim.height(), 0.0F, 0.0F, LOGO_SIZE, LOGO_SIZE, LOGO_SIZE, LOGO_SIZE);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackground(drawContext, mouseX, mouseY, delta);
        this.updateControls();
        this.frame.render(drawContext, mouseX, mouseY, delta);
    }

    private void updateControls() {
        boolean hasChanges = this.getAllOptions()
                .anyMatch(Option::hasChanged);

        for (OptionPage page : this.pages) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    hasChanges = true;
                }
            }
        }

        this.applyButton.setEnabled(hasChanges);
        this.undoButton.setVisible(hasChanges);
        this.closeButton.setEnabled(!hasChanges);

        this.hasPendingChanges = hasChanges;
    }

    private void setDonationButtonVisibility(boolean value) {
        this.donateButton.setVisible(value);
        this.hideDonateButton.setVisible(value);
    }

    private void hideDonationButton() {
        EmbeddiumOptions options = Embeddium.options();
        options.notifications.hasClearedDonationButton = true;

        try {
            EmbeddiumOptions.writeToDisk(options);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save configuration", e);
        }

        this.setDonationButtonVisibility(false);


        this.rebuildUI();
    }

    private void openDonationPage() {
        Util.getPlatform()
                .openUri("https://caffeinemc.net/donate");
    }

    private Stream<Option<?>> getAllOptions() {
        return this.pages.stream()
                .flatMap(s -> s.getOptions().stream());
    }

    private void applyChanges() {
        final HashSet<OptionStorage<?>> dirtyStorages = new HashSet<>();
        final EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);

        this.getAllOptions().forEach((option -> {
            if (!option.hasChanged()) {
                return;
            }

            option.applyChanges();

            flags.addAll(option.getFlags());
            dirtyStorages.add(option.getStorage());
        }));

        Minecraft client = Minecraft.getInstance();

        if (client.level != null) {
            if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
                client.levelRenderer.allChanged();
            } else if (flags.contains(OptionFlag.REQUIRES_RENDERER_UPDATE)) {
                client.levelRenderer.needsUpdate();
            }
        }

        if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
            client.updateMaxMipLevel(client.options.mipmapLevels().get());
            client.delayTextureReload();
        }

        if (flags.contains(OptionFlag.REQUIRES_GAME_RESTART)) {
            Console.instance().logMessage(MessageLevel.WARN,
                    Component.translatable("sodium.console.game_restart"), 10.0);
        }

        for (OptionStorage<?> storage : dirtyStorages) {
            storage.save();
        }
    }

    private void undoChanges() {
        this.getAllOptions()
                .forEach(Option::reset);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_P && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 && !(this.searchTextField != null && this.searchTextField.isFocused())) {
            Minecraft.getInstance().setScreen(new VideoSettingsScreen(this.prevScreen, Minecraft.getInstance(), Minecraft.getInstance().options));

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.hasPendingChanges;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.prevScreen);
    }

    public static final List<FormattedText> DONATION_PROMPT_MESSAGE;

    static {
        DONATION_PROMPT_MESSAGE = List.of(
                FormattedText.composite(Component.literal("Hello!")),
                FormattedText.composite(Component.literal("It seems that you've been enjoying "), Component.literal("Embeddium").setStyle(Style.EMPTY.withColor(0x27eb92)), Component.literal(", a fork of Sodium for Minecraft.")),
                FormattedText.composite(Component.literal("Sodium is complex, and requires "), Component.literal("thousands of hours").setStyle(Style.EMPTY.withColor(0xff6e00)), Component.literal(" of development, debugging, and tuning to create the experience that players have come to expect.")),
                FormattedText.composite(Component.literal("If you'd like to show a token of appreciation, and support the development of Sodium in the process, then consider "), Component.literal("buying them a coffee").setStyle(Style.EMPTY.withColor(0xed49ce)), Component.literal(".")),
                FormattedText.composite(Component.literal("And thanks again for using the mod! We hope it helps you (and your computer.)"))
        );
    }
}
