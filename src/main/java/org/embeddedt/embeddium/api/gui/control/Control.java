package org.embeddedt.embeddium.api.gui.control;

import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionControlElement;
import org.embeddedt.embeddium.api.math.Dim2i;

public interface Control<T> {
    Option<T> getOption();

    OptionControlElement<T> createElement(Dim2i dim);

    int getMaxWidth();
}
