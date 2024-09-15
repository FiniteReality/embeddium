package org.embeddedt.embeddium.impl.gametest.content;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.gametest.content.client.InstrumentingModelWrapper;
import org.embeddedt.embeddium.impl.gametest.content.client.TestModel;
import org.embeddedt.embeddium.impl.gametest.network.SyncS2CPacket;
import org.embeddedt.embeddium.impl.gametest.tests.EmbeddiumGameTests;
import org.embeddedt.embeddium.impl.gametest.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestRegistry {
    public static final Logger LOGGER = LoggerFactory.getLogger("Embeddium/TestSystem");

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EmbeddiumConstants.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EmbeddiumConstants.MODID);

    public static final RegistryObject<TestBlock> TEST_BLOCK = BLOCKS.register("test_block", TestBlock::new);
    public static final RegistryObject<NotAnAirBlock> NOT_AN_AIR_BLOCK = BLOCKS.register("not_an_air_block", NotAnAirBlock::new);
    public static final RegistryObject<BlockEntityType<?>> TEST_BLOCK_ENTITY = BLOCK_ENTITIES.register("test_block_entity", () -> BlockEntityType.Builder.of(TestBlockEntity::new, TEST_BLOCK.get()).build(null));

    public static final ResourceLocation EMPTY_TEMPLATE = new ResourceLocation(EmbeddiumConstants.MODID, "empty_structure");
    public static final String EMPTY_TEMPLATE_STR = EMPTY_TEMPLATE.toString();

    public static final boolean IS_AUTOMATED_TEST_RUN = Boolean.getBoolean("embeddium.runAutomatedTests");

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EmbeddiumConstants.MODID, "gametests"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * Register our test content with various registries.
     */
    @SubscribeEvent
    public static void register(FMLConstructModEvent event) {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MinecraftForge.EVENT_BUS.register(GameEvents.class);

        NETWORK_CHANNEL.registerMessage(0, SyncS2CPacket.class, SyncS2CPacket::encode, SyncS2CPacket::new, SyncS2CPacket::handle);
    }

    private static void registerModelForAllStates(ModelEvent.ModifyBakingResult event, Block block, BakedModel model) {
        for(BlockState state : block.getStateDefinition().getPossibleStates()) {
            event.getModels().put(BlockModelShaper.stateToModelLocation(state), model);
        }
    }

    /**
     * Inject custom models for test content.
     */
    @SubscribeEvent
    public static void onBakingModify(ModelEvent.ModifyBakingResult event) {
        registerModelForAllStates(event, TEST_BLOCK.get(), new TestModel(event.getModels().get(ModelBakery.MISSING_MODEL_LOCATION)));
        registerModelForAllStates(event, NOT_AN_AIR_BLOCK.get(), new InstrumentingModelWrapper<>(event.getModels().get(BlockModelShaper.stateToModelLocation(Blocks.STONE.defaultBlockState()))));
    }

    static class GameEvents {
        /**
         * Inject the fake structure template used to allow creating gametests without dedicated structures.
         */
        @SubscribeEvent
        public static void createEmptyTemplate(ServerAboutToStartEvent event) {
            var structureManager = event.getServer().getStructureManager();
            StructureTemplate template = structureManager.getOrCreate(EMPTY_TEMPLATE);
            ObfuscationReflectionHelper.setPrivateValue(StructureTemplate.class, template, new Vec3i(9, 9, 9), "size");
            template.setAuthor("Embeddium");
        }

        private static boolean hasSeenMainMenu = false;

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if(IS_AUTOMATED_TEST_RUN && (event.getScreen() instanceof TitleScreen || event.getScreen() instanceof AccessibilityOnboardingScreen) && !hasSeenMainMenu) {
                // Go for main engine start (create a new superflat automatically)
                hasSeenMainMenu = true;
                var mc = Minecraft.getInstance();
                mc.options.renderDebug = true;
                mc.forceSetScreen(new GenericDirtMessageScreen(Component.literal("Bootstrapping gametests...")));
                String levelName = "embeddium-test-" + UUID.randomUUID();
                LevelSettings settings = new LevelSettings(levelName, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(1024, false, false), registry -> {
                    return registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions();
                });
            }
        }

        private static final Set<NamedGuiOverlay> HIDDEN_OVERLAYS = Stream.of(VanillaGuiOverlay.CHAT_PANEL, VanillaGuiOverlay.VIGNETTE).map(VanillaGuiOverlay::type).collect(Collectors.toUnmodifiableSet());

        @SubscribeEvent
        public static void hideGuiLayers(RenderGuiOverlayEvent.Pre event) {
            if(IS_AUTOMATED_TEST_RUN && HIDDEN_OVERLAYS.contains(event.getOverlay())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onScreenOpen(ScreenEvent.Opening event) {
            if(IS_AUTOMATED_TEST_RUN && event.getScreen() instanceof PauseScreen && !Minecraft.getInstance().isWindowActive()) {
                // Prevent pause screen from opening if window isn't active
                event.setCanceled(true);
            }
        }

        private static final Semaphore LATCH = new Semaphore(0);

        @SubscribeEvent
        public static void onScreenChange(ScreenEvent.Closing event) {
            if(IS_AUTOMATED_TEST_RUN && event.getScreen() instanceof ReceivingLevelScreen) {
                // Booster ignition
                LATCH.release();
            }
        }

        private static MultipleTestTracker testTracker;

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if(event.phase == TickEvent.Phase.START) {
                if(LATCH.tryAcquire()) {
                    // Liftoff
                    Collection<GameTestInfo> collection = GameTestRunner.runTestBatches(
                            List.of(new GameTestBatch("Embeddium", GameTestRegistry.getAllTestFunctions(), l -> {}, l -> {})),
                            new BlockPos(0, -60, 0),
                            Rotation.NONE,
                            event.getServer().overworld(),
                            GameTestTicker.SINGLETON,
                            8);
                    testTracker = new MultipleTestTracker(collection);
                } else if(testTracker != null) {
                    if(testTracker.isDone()) {
                        LOGGER.info(testTracker.getProgressBar());
                        GlobalTestReporter.finish();
                        LOGGER.info("Completed {} tests", testTracker.getTotalCount());
                        int exitCode;
                        if(testTracker.hasFailedRequired()) {
                            testTracker.getFailedRequired().forEach(info -> {
                                LOGGER.info("Test {} failed", info.getTestName(), info.getError());
                            });
                            exitCode = 1;
                        } else {
                            exitCode = 0;
                        }
                        Minecraft.getInstance().execute(() -> System.exit(exitCode));
                        testTracker = null;
                    } else if(event.getServer().overworld().getGameTime() % 20 == 0) {
                        LOGGER.info(testTracker.getProgressBar());
                    }
                }
            }
        }
    }

    private static void testRunner(GameTestHelper helper, Method testMethod) {
        // Run automated (server-side) component of test
        try {
            testMethod.invoke(null, helper);
        } catch (InvocationTargetException e) {
            if(e.getCause() instanceof RuntimeException re) {
                throw re;
            } else {
                throw new RuntimeException("Error running test", e);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error invoking method", e);
        } finally {
            // Run client side of test by executing any side effects that may occur on the block
            TestUtils.movePlayerToPosition(helper, new BlockPos(4, 12, 4));
            TestUtils.waitForTestAreaToLoad(helper);
            // Capture screenshot
            TestUtils.obtainScreenshot(testMethod.getName());
        }
    }

    /**
     * Bootstrap the game tests, with our injected empty structure template from above.
     */
    @SubscribeEvent
    public static void createGameTests(RegisterGameTestsEvent event) {
        Collection<TestFunction> functions = ObfuscationReflectionHelper.getPrivateValue(GameTestRegistry.class, null, "TEST_FUNCTIONS");
        Set<String> classNames = ObfuscationReflectionHelper.getPrivateValue(GameTestRegistry.class, null, "TEST_CLASS_NAMES");
        classNames.add(EmbeddiumGameTests.class.getSimpleName());
        for(Method m : EmbeddiumGameTests.class.getDeclaredMethods()) {
            GameTest gametest = m.getAnnotation(GameTest.class);
            if(gametest != null) {
                functions.add(new TestFunction(
                    gametest.batch(),
                    m.getName(),
                    EMPTY_TEMPLATE_STR,
                    Rotation.NONE,
                    gametest.timeoutTicks(),
                    gametest.setupTicks(),
                    gametest.required(),
                    gametest.requiredSuccesses(),
                    gametest.attempts(),
                    helper -> testRunner(helper, m)
                ));
            }
        }
    }
}
