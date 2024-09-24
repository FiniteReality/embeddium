package net.neoforged.neoforge.client.model.data;

public class ModelData {
    public static final ModelData EMPTY = new ModelData();

    public ModelData derive() {
        return this;
    }

    public ModelData build() {
        return this;
    }

    public <T> ModelData with(ModelProperty<T> prop, T val) {
        return this;
    }

    public <T> T get(ModelProperty<T> prop) {
        return null;
    }
}
