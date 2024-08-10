package org.embeddedt.embeddium.impl.gametest.tests;

import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.gametest.content.TestRegistry;

public class EmbeddiumGameTests {
    /**
     * Test that the hidesNeighborFace Forge extension is used correctly.
     */
    @GameTest
    public static void testBlockHidingNeighborFace(GameTestHelper helper) {
        BlockOcclusionCache cache = new BlockOcclusionCache();

        BlockPos selfPos = new BlockPos(2, 2, 2);
        BlockState selfState = TestRegistry.TEST_BLOCK.get().defaultBlockState();
        helper.setBlock(selfPos, selfState);
        helper.setBlock(selfPos.relative(Direction.EAST), Blocks.STONE);
        helper.assertTrue(cache.shouldDrawSide(selfState, helper.getLevel(), helper.absolutePos(selfPos), Direction.EAST), "Did not show face of neighbor block as expected");
        helper.setBlock(selfPos.relative(Direction.EAST), selfState);
        helper.assertFalse(cache.shouldDrawSide(selfState, helper.getLevel(), helper.absolutePos(selfPos), Direction.EAST), "Did not hide face of neighbor block as expected");
        helper.succeed();
    }
}
