package org.embeddedt.embeddium.api.options.structure;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public interface OptionControlElement<T> extends Renderable, GuiEventListener, NarratableEntry {
    Option<T> getOption();
}
