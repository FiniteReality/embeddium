package org.embeddedt.embeddium.impl.model.light;

import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.model.light.flat.FlatLightPipeline;
import org.embeddedt.embeddium.impl.model.light.smooth.SmoothLightPipeline;
import net.neoforged.neoforge.common.NeoForgeConfig;
import org.embeddedt.embeddium.impl.render.chunk.light.ForgeLightPipeline;

import java.util.EnumMap;

public class LightPipelineProvider {
    private final EnumMap<LightMode, LightPipeline> lighters = new EnumMap<>(LightMode.class);
    private final LightDataAccess lightData;

    public LightPipelineProvider(LightDataAccess cache) {
        this.lightData = cache;
        if (NeoForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get()) {
            this.lighters.put(LightMode.SMOOTH, ForgeLightPipeline.smooth(cache));
            this.lighters.put(LightMode.FLAT, ForgeLightPipeline.flat(cache));
        } else {
            this.lighters.put(LightMode.SMOOTH, new SmoothLightPipeline(cache));
            this.lighters.put(LightMode.FLAT, new FlatLightPipeline(cache));
        }
    }

    public LightPipeline getLighter(LightMode type) {
        LightPipeline pipeline = this.lighters.get(type);

        if (pipeline == null) {
            throw new NullPointerException("No lighter exists for mode: " + type.name());
        }

        return pipeline;
    }

    public LightDataAccess getLightData() {
        return this.lightData;
    }

    public void reset() {
        for (LightPipeline pipeline : this.lighters.values()) {
            pipeline.reset();
        }
    }
}
