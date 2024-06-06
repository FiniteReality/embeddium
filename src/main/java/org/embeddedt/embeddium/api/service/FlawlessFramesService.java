package org.embeddedt.embeddium.api.service;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An interface that mods may implement as a service to receive the FREX Flawless Frames entrypoint at initialization
 * time. The semantics of the provided argument are the same as in FREX. That is, you should call it with a unique
 * string representing your mod, and you receive a {@code Consumer<Boolean>} that may be used to enable and disable
 * flawless frames. The status is retained across multiple frames until changed again.
 */
public interface FlawlessFramesService {
    void acceptController(Function<String, Consumer<Boolean>> controller);
}
