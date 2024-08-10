package org.embeddedt.embeddium.impl.gametest.tests;

import org.embeddedt.embeddium.api.service.FlawlessFramesService;
import org.embeddedt.embeddium.impl.gametest.content.TestRegistry;

import java.util.function.Consumer;
import java.util.function.Function;

public class TestFlawlessFramesService implements FlawlessFramesService {
    @Override
    public void acceptController(Function<String, Consumer<Boolean>> controller) {
        if (TestRegistry.IS_AUTOMATED_TEST_RUN) {
            controller.apply("embeddium").accept(true);
        }
    }
}
