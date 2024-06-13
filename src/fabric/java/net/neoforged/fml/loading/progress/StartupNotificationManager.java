package net.neoforged.fml.loading.progress;

import java.util.Optional;
import java.util.function.Consumer;

public class StartupNotificationManager {
    public static Optional<Consumer<String>> modLoaderConsumer() {
        return Optional.empty();
    }
}
