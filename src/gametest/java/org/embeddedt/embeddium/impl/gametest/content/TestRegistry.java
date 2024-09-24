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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.gametest.content.client.InstrumentingModelWrapper;
import org.embeddedt.embeddium.impl.gametest.content.client.TestModel;
import org.embeddedt.embeddium.impl.gametest.network.SyncS2CPacket;
import org.embeddedt.embeddium.impl.gametest.tests.EmbeddiumGameTests;
import org.embeddedt.embeddium.impl.gametest.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class TestRegistry {
    public static final Logger LOGGER = LoggerFactory.getLogger("Embeddium/TestSystem");

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, EmbeddiumConstants.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EmbeddiumConstants.MODID);

    public static final DeferredHolder<Block, TestBlock> TEST_BLOCK = BLOCKS.register("test_block", TestBlock::new);
    public static final DeferredHolder<Block, NotAnAirBlock> NOT_AN_AIR_BLOCK = BLOCKS.register("not_an_air_block", NotAnAirBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> TEST_BLOCK_ENTITY = BLOCK_ENTITIES.register("test_block_entity", () -> BlockEntityType.Builder.of(TestBlockEntity::new, TEST_BLOCK.get()).build(null));

    public static final ResourceLocation EMPTY_TEMPLATE = ResourceLocation.fromNamespaceAndPath(EmbeddiumConstants.MODID, "empty_structure");
    public static final String EMPTY_TEMPLATE_STR = EMPTY_TEMPLATE.toString();

    public static final boolean IS_AUTOMATED_TEST_RUN = Boolean.getBoolean("embeddium.runAutomatedTests");

    /**
     * Register our test content with various registries.
     */
    @SubscribeEvent
    public static void register(FMLConstructModEvent event) {
        var bus = ModLoadingContext.get().getActiveContainer().getEventBus();
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        NeoForge.EVENT_BUS.register(GameEvents.class);
    }

    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        // Sets the current network version
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncS2CPacket.TYPE, SyncS2CPacket.STREAM_CODEC, SyncS2CPacket::handle);
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
        registerModelForAllStates(event, TEST_BLOCK.get(), new TestModel(event.getModels().get(ModelBakery.MISSING_MODEL_VARIANT)));
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
                var debugOverlay = mc.gui.getDebugOverlay();
                debugOverlay.reset();
                debugOverlay.toggleOverlay();
                var messageScreen = new GenericMessageScreen(Component.literal("Bootstrapping gametests..."));
                mc.forceSetScreen(messageScreen);
                String levelName = "embeddium-test-" + UUID.randomUUID();
                LevelSettings settings = new LevelSettings(levelName, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(1024, false, false), registry -> {
                    return registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions();
                }, messageScreen);
            }
        }

        private static final Set<ResourceLocation> HIDDEN_OVERLAYS = Set.of(VanillaGuiLayers.CHAT, VanillaGuiLayers.CAMERA_OVERLAYS);

        @SubscribeEvent
        public static void hideGuiLayers(RenderGuiLayerEvent.Pre event) {
            if(IS_AUTOMATED_TEST_RUN && HIDDEN_OVERLAYS.contains(event.getName())) {
                event.setCanceled(true);
            }
        }

        private static WeakReference<Screen> nextOpeningScreen;

        @SubscribeEvent
        public static void onScreenOpen(ScreenEvent.Opening event) {
            if(IS_AUTOMATED_TEST_RUN && event.getScreen() instanceof PauseScreen && !Minecraft.getInstance().isWindowActive()) {
                // Prevent pause screen from opening if window isn't active
                event.setCanceled(true);
                return;
            }

            nextOpeningScreen = new WeakReference<>(event.getScreen());
        }

        private static final Semaphore LATCH = new Semaphore(0);

        @SubscribeEvent
        public static void onScreenChange(ScreenEvent.Closing event) {
            if(IS_AUTOMATED_TEST_RUN && !(nextOpeningScreen.get() instanceof ReceivingLevelScreen) && event.getScreen() instanceof ReceivingLevelScreen) {
                // Booster ignition
                LATCH.release();
            }
            nextOpeningScreen = new WeakReference<>(null);
        }

        private static MultipleTestTracker testTracker;

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Pre event) {
            if(true) {
                if(LATCH.tryAcquire()) {
                    // Liftoff
                    var level = event.getServer().overworld();
                    var runner = GameTestRunner.Builder
                            .fromBatches(
                                    GameTestBatchFactory.fromTestFunction(GameTestRegistry.getAllTestFunctions(), level),
                                    level
                            )
                            .newStructureSpawner(new StructureGridSpawner(new BlockPos(0, -60, 0), 8, false))
                            .build();
                    runner.start();
                    testTracker = new MultipleTestTracker(runner.getTestInfos());
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
                    gametest.manualOnly(),
                    gametest.requiredSuccesses(),
                    gametest.attempts(),
                    gametest.skyAccess(),
                    helper -> testRunner(helper, m)
                ));
            }
        }
    }
}
