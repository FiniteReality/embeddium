package net.neoforged.fml.loading;

public class FMLConfig {
    public static class ConfigValue {
        public static final Object EARLY_WINDOW_CONTROL = false;
        public static final Object EARLY_WINDOW_PROVIDER = "dummy";
    }

    public static boolean getBoolConfigValue(Object key) {
        return (Boolean)key;
    }

    public static Object getConfigValue(Object key) {
        return key;
    }
}
