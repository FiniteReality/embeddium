package org.embeddedt.embeddium.gui.options.control;

import org.embeddedt.embeddium.gui.options.Option;
import org.embeddedt.embeddium.util.Dim2i;

public interface Control<T> {
    Option<T> getOption();

    ControlElement<T> createElement(Dim2i dim);

    int getMaxWidth();
}
