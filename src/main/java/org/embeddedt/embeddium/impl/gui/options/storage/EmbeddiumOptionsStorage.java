package org.embeddedt.embeddium.impl.gui.options.storage;

import org.embeddedt.embeddium.impl.Embeddium;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.gui.EmbeddiumOptions;

import java.io.IOException;

public class EmbeddiumOptionsStorage implements OptionStorage<EmbeddiumOptions> {
    private final EmbeddiumOptions options;

    public EmbeddiumOptionsStorage() {
        this.options = Embeddium.options();
    }

    @Override
    public EmbeddiumOptions getData() {
        return this.options;
    }

    @Override
    public void save() {
        try {
            EmbeddiumOptions.writeToDisk(this.options);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't save configuration changes", e);
        }

        Embeddium.logger().info("Flushed changes to Embeddium configuration");
    }
}
