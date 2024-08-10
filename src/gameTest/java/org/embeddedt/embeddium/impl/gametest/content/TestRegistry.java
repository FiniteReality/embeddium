package org.embeddedt.embeddium.impl.gametest.content;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.gametest.content.client.TestModel;
import org.embeddedt.embeddium.impl.gametest.tests.EmbeddiumGameTests;
import org.embeddedt.embeddium.impl.gametest.util.TestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

@Mod.EventBusSubscriber(modid = EmbeddiumConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TestRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EmbeddiumConstants.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EmbeddiumConstants.MODID);

    public static final RegistryObject<TestBlock> TEST_BLOCK = BLOCKS.register("test_block", TestBlock::new);
    public static final RegistryObject<BlockEntityType<?>> TEST_BLOCK_ENTITY = BLOCK_ENTITIES.register("test_block_entity", () -> BlockEntityType.Builder.of(TestBlockEntity::new, TEST_BLOCK.get()).build(null));

    public static final ResourceLocation EMPTY_TEMPLATE = new ResourceLocation(EmbeddiumConstants.MODID, "empty_structure");
    public static final String EMPTY_TEMPLATE_STR = EMPTY_TEMPLATE.toString();

    /**
     * Register our test content with various registries.
     */
    @SubscribeEvent
    public static void register(FMLConstructModEvent event) {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MinecraftForge.EVENT_BUS.addListener(TestRegistry::createEmptyTemplate);
        MinecraftForge.EVENT_BUS.addListener(TestRegistry::onBakingModify);
        MinecraftForge.EVENT_BUS.addListener(TestRegistry::onServerTick);
    }

    /**
     * Inject the custom model for the test block.
     */
    public static void onBakingModify(ModelEvent.ModifyBakingResult event) {
        var testModel = new TestModel(event.getModels().get(ModelBakery.MISSING_MODEL_LOCATION));
        for(BlockState state : TEST_BLOCK.get().getStateDefinition().getPossibleStates()) {
            event.getModels().put(BlockModelShaper.stateToModelLocation(state), testModel);
        }
    }

    /**
     * Inject the fake structure template used to allow creating gametests without dedicated structures.
     */
    public static void createEmptyTemplate(ServerAboutToStartEvent event) {
        var structureManager = event.getServer().getStructureManager();
        StructureTemplate template = structureManager.getOrCreate(EMPTY_TEMPLATE);
        ObfuscationReflectionHelper.setPrivateValue(StructureTemplate.class, template, new Vec3i(9, 9, 9), "size");
        template.setAuthor("Embeddium");
    }

    private static void testRunner(GameTestHelper helper, Method testMethod) {
        TestUtils.movePlayerToPosition(helper, new BlockPos(4, 12, 4));
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

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {

        }
    }
}
