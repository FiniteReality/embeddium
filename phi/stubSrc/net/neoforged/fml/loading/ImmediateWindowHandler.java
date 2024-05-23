package net.neoforged.fml.loading;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class ImmediateWindowHandler {
    public static long setupMinecraftWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        return 0;
    }

    public static void acceptGameLayer(ModuleLayer moduleLayer) {

    }
}
