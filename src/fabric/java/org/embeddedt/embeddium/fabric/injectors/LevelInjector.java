package org.embeddedt.embeddium.fabric.injectors;

import net.neoforged.neoforge.client.model.data.ModelDataManager;

public interface LevelInjector {
    default ModelDataManager getModelDataManager() {
        return ModelDataManager.INSTANCE;
    }
}
