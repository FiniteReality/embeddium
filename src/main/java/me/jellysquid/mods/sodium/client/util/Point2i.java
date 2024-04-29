package me.jellysquid.mods.sodium.client.util;

public interface Point2i {
    Point2i ZERO = new Point2i() {
        @Override
        public int x() {
            return 0;
        }
        @Override
        public int y() {
            return 0;
        }
    };

    int x();

    int y();
}
