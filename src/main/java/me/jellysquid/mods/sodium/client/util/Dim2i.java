package me.jellysquid.mods.sodium.client.util;

public record Dim2i(int x, int y, int width, int height) implements Point2i {
    public int getLimitX() {
        return this.x + this.width;
    }

    public int getLimitY() {
        return this.y + this.height;
    }

    public boolean containsCursor(double x, double y) {
        return x >= this.x && x < this.getLimitX() && y >= this.y && y < this.getLimitY();
    }

    public int getCenterX() {
        return this.x + (this.width / 2);
    }

    public int getCenterY() {
        return this.y + (this.height / 2);
    }

    public Dim2i withHeight(int newHeight) {
        return new Dim2i(x, y, width, newHeight);
    }

    public Dim2i withWidth(int newWidth) {
        return new Dim2i(x, y, newWidth, height);
    }

    public Dim2i withX(int newX) {
        return new Dim2i(newX, y, width, height);
    }

    public Dim2i withY(int newY) {
        return new Dim2i(x, newY, width, height);
    }

    public boolean canFitDimension(Dim2i anotherDim) {
        return this.x() <= anotherDim.x() && this.y() <= anotherDim.y() && this.getLimitX() >= anotherDim.getLimitX() && this.getLimitY() >= anotherDim.getLimitY();
    }

    public boolean overlapsWith(Dim2i other) {
        return this.x() < other.getLimitX() && this.getLimitX() > other.x() && this.y() < other.getLimitY() && this.getLimitY() > other.y();
    }

    public Dim2i withParentOffset(Point2i parent) {
        return new Dim2i(parent.x() + x, parent.y() + y, width, height);
    }
}
