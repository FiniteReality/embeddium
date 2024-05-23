package org.embeddedt.embeddium.gui.options.storage;

import org.embeddedt.embeddium.Embeddium;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.gui.EmbeddiumOptions;

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
            this.options.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't save configuration changes", e);
        }

        Embeddium.logger().info("Flushed changes to Embeddium configuration");
    }
}
