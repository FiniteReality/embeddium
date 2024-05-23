package org.embeddedt.embeddium.gui.prompt;

import org.embeddedt.embeddium.util.Dim2i;
import org.jetbrains.annotations.Nullable;

@Deprecated(forRemoval = true)
public interface ScreenPromptable {
    void setPrompt(@Nullable ScreenPrompt prompt);

    @Nullable ScreenPrompt getPrompt();

    Dim2i getDimensions();
}
