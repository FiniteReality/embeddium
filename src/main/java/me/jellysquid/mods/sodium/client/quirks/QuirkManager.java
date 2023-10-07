package me.jellysquid.mods.sodium.client.quirks;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class QuirkManager {
    /**
     * Force rebinding of the 2nd texture (lightmap) if Quartz and Lodestone are installed. This fixes
     * <a href="https://github.com/LodestarMC/Lodestone/issues/9">LodestarMC/Lodestone#9</a>.
     * <p></p>
     * Only tested on 18.2.
     */
    public static final boolean REBIND_LIGHTMAP_TEXTURE = Stream.of("quartz", "lodestone").allMatch(QuirkManager::isLoaded);

    static {
        try {
            List<String> enabledQuirks = new ArrayList<>();
            for(Field f : QuirkManager.class.getDeclaredFields()) {
                if(f.getType() == boolean.class && Modifier.isStatic(f.getModifiers())) {
                    if(f.getBoolean(null))
                        enabledQuirks.add(f.getName());
                }
            }
            if(enabledQuirks.size() > 0)
                SodiumClientMod.logger().warn("Enabled the following quirks in QuirkManager: [{}]", String.join(", ", enabledQuirks));
        } catch(ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
