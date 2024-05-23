package org.embeddedt.embeddium.impl.compatibility.environment;

import java.util.Locale;

public class OSInfo {
    public enum OS {
        WINDOWS,
        LINUX,
        UNKNOWN
    }
    public static OS getOS() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if(s.contains("win"))
            return OS.WINDOWS;
        else if(s.contains("linux") || s.contains("unix"))
            return OS.LINUX;

        return OS.UNKNOWN;
    }
}
