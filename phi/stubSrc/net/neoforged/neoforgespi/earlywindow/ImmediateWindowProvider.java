package net.neoforged.neoforgespi.earlywindow;

public interface ImmediateWindowProvider {
    default String name() {
        return "dummy";
    }

    default Runnable initialize(String[] args) {
        return () -> {};
    }
}
