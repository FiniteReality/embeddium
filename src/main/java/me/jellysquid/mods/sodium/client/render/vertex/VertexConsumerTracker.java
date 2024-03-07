package me.jellysquid.mods.sodium.client.render.vertex;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static me.jellysquid.mods.sodium.client.SodiumClientMod.MODNAME;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class VertexConsumerTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME + "-VertexConsumerTracker");
    private static final ReferenceSet<Class<? extends VertexConsumer>> BAD_CONSUMERS = ReferenceSets.synchronize(new ReferenceOpenHashSet<>());
    private static final boolean WARN_NON_VBW_CONSUMERS = Boolean.getBoolean("embeddium.logBadVertexConsumers");

    public static void logBadConsumer(VertexConsumer consumer) {
        if (WARN_NON_VBW_CONSUMERS && BAD_CONSUMERS.add(consumer.getClass())) {
            LOGGER.warn("Class {} does not support optimized vertex writing code paths, which may cause reduced rendering performance",
                    consumer.getClass().getName());
        }
    }
}
