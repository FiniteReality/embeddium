package org.embeddedt.embeddium.api.render.cloud;

@FunctionalInterface
public interface CloudDistanceFilter {
    CloudDistanceFilter DEFAULT = x -> x;

    int getCloudDistance(int renderDistance);
}
