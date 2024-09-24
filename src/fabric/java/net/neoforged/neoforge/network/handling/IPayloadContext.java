package net.neoforged.neoforge.network.handling;

public interface IPayloadContext {
    void enqueueWork(Runnable runnable);
}
