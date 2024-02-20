package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.compat.forge.ForgeBlockRenderer;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderTranslucencySortTask;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.common.util.collections.DequeDrain;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkBuilder<T extends ChunkGraphicsState> {
    /**
     * The maximum number of jobs that can be queued for a given worker thread.
     */
    private static final int TASK_QUEUE_LIMIT_PER_WORKER = 2;

    private static final Logger LOGGER = LogManager.getLogger("ChunkBuilder");
    /**
     * Megabytes of heap required per chunk builder thread. This is used to cap the number of worker
     * threads when the game is given a small heap.
     */
    private static final int MBS_PER_CHUNK_BUILDER = 64;

    private final Deque<WrappedTask<T>> buildQueue = new ConcurrentLinkedDeque<>();
    private final Deque<ChunkBuildResult<T>> uploadQueue = new ConcurrentLinkedDeque<>();
    private final Deque<Throwable> failureQueue = new ConcurrentLinkedDeque<>();

    private final Object jobNotifier = new Object();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<Thread> threads = new ArrayList<>();
    private final List<ChunkRenderCacheLocal> chunkRenderCaches = new ArrayList<>();

    private ClonedChunkSectionCache sectionCache;

    private Level world;
    private Vec3 cameraPosition = Vec3.ZERO;
    private BlockRenderPassManager renderPassManager;

    private final int limitThreads;
    private final ChunkVertexType vertexType;
    private final ChunkRenderBackend<T> backend;

    public ChunkBuilder(ChunkVertexType vertexType, ChunkRenderBackend<T> backend) {
        this.vertexType = vertexType;
        this.backend = backend;
        this.limitThreads = getThreadCount();
    }

    /**
     * Returns the remaining number of build tasks which should be scheduled this frame. If an attempt is made to
     * spawn more tasks than the budget allows, it will block until resources become available.
     */
    public int getSchedulingBudget() {
        return Math.max(0, (this.limitThreads * TASK_QUEUE_LIMIT_PER_WORKER) - this.buildQueue.size());
    }

    /**
     * Spawns a number of work-stealing threads to process results in the build queue. If the builder is already
     * running, this method does nothing and exits.
     */
    public void startWorkers() {
        if (this.running.getAndSet(true)) {
            return;
        }

        if (!this.threads.isEmpty()) {
            throw new IllegalStateException("Threads are still alive while in the STOPPED state");
        }

        Minecraft client = Minecraft.getInstance();

        for (int i = 0; i < this.limitThreads; i++) {
            ChunkBuildBuffers buffers = new ChunkBuildBuffers(this.vertexType, this.renderPassManager);
            ChunkRenderCacheLocal pipeline = new ChunkRenderCacheLocal(client, this.world);

            WorkerRunnable worker = new WorkerRunnable(buffers, pipeline);

            Thread thread = new Thread(worker, "Chunk Render Task Executor #" + i);
            thread.setPriority(Math.max(0, Thread.NORM_PRIORITY - 2));
            thread.start();

            this.threads.add(thread);
            this.chunkRenderCaches.add(pipeline);
        }

        LOGGER.info("Started {} worker threads", this.threads.size());
    }

    /**
     * Notifies all worker threads to stop and blocks until all workers terminate. After the workers have been shut
     * down, all tasks are cancelled and the pending queues are cleared. If the builder is already stopped, this
     * method does nothing and exits.
     */
    public void stopWorkers() {
        if (!this.running.getAndSet(false)) {
            return;
        }

        if (this.threads.isEmpty()) {
            throw new IllegalStateException("No threads are alive but the executor is in the RUNNING state");
        }

        LOGGER.info("Stopping worker threads");

        // Notify all worker threads to wake up, where they will then terminate
        synchronized (this.jobNotifier) {
            this.jobNotifier.notifyAll();
        }

        // Wait for every remaining thread to terminate
        for (Thread thread : this.threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }

        this.threads.clear();
        this.chunkRenderCaches.clear();

        // Drop any pending work queues and cancel futures
        this.uploadQueue.clear();
        this.failureQueue.clear();

        for (WrappedTask<?> job : this.buildQueue) {
            job.future.cancel(true);
        }

        this.buildQueue.clear();

        this.world = null;
        this.sectionCache = null;
    }

    public void cleanupSectionCache() {
        this.sectionCache.cleanup();
    }

    /**
     * Prevent sort updates from taking priority over rebuild updates in the upload queue.
     */
    public Iterator<ChunkBuildResult<T>> filterChunkBuilds(Iterator<ChunkBuildResult<T>> uploadIterator) {
        Reference2ReferenceLinkedOpenHashMap<ChunkRenderContainer<T>, ChunkBuildResult<T>> map = new Reference2ReferenceLinkedOpenHashMap<>();

        while (uploadIterator.hasNext()) {
            ChunkBuildResult<T> result = uploadIterator.next();
            ChunkRenderContainer<T> section = result.render;

            ChunkBuildResult<T> oldResult = map.get(section);

            // Allow a result to replace the previous result in the map if one of the following conditions hold:
            // * There is no previous upload in the queue
            // * The new upload replaces more render types than the old one (in practice, is a rebuild while the other is a sort)
            if(oldResult == null || result.passesToUpload.length >= oldResult.passesToUpload.length) {
                map.put(section, result);
            }
        }

        return map.values().iterator();
    }

    /**
     * Processes all pending build task uploads using the chunk render backend.
     */
    // TODO: Limit the amount of time this can take per frame
    public boolean performPendingUploads() {
        if (this.uploadQueue.isEmpty()) {
            return false;
        }

        this.backend.upload(RenderDevice.INSTANCE.createCommandList(), filterChunkBuilds(new DequeDrain<>(this.uploadQueue)));

        return true;
    }

    public void handleFailures() {
        Iterator<Throwable> errorIterator = new DequeDrain<>(this.failureQueue);

        if (errorIterator.hasNext()) {
            // If there is any exception from the build failure queue, throw it
            Throwable ex = errorIterator.next();

            if (ex instanceof ReportedException) {
                // Propagate CrashExceptions directly to provide extra information
                throw (ReportedException)ex;
            } else {
                throw new RuntimeException("Chunk build failed", ex);
            }
        }
    }

    public CompletableFuture<ChunkBuildResult<T>> schedule(ChunkRenderBuildTask<T> task) {
        if (!this.running.get()) {
            throw new IllegalStateException("Executor is stopped");
        }

        WrappedTask<T> job = new WrappedTask<>(task);

        this.buildQueue.add(job);

        synchronized (this.jobNotifier) {
            this.jobNotifier.notify();
        }

        return job.future;
    }

    /**
     * Get the list of current chunk render caches.
     */
    public Collection<ChunkRenderCacheLocal> getChunkRenderCaches() {
        return Collections.unmodifiableList(this.chunkRenderCaches);
    }

    /**
     * Sets the current camera position of the player used for task prioritization.
     */
    public void setCameraPosition(double x, double y, double z) {
        this.cameraPosition = new Vec3(x, y, z);
    }

    /**
     * Returns the current camera position of the player used for task prioritization.
     */
    public Vec3 getCameraPosition() {
        return this.cameraPosition;
    }

    /**
     * @return True if the build queue is empty
     */
    public boolean isBuildQueueEmpty() {
        return this.buildQueue.isEmpty();
    }

    /**
     * Initializes this chunk builder for the given world. If the builder is already running (which can happen during
     * a world teleportation event), the worker threads will first be stopped and all pending tasks will be discarded
     * before being started again.
     * @param world The world instance
     * @param renderPassManager The render pass manager used for the world
     */
    public void init(ClientLevel world, BlockRenderPassManager renderPassManager) {
        if (world == null) {
            throw new NullPointerException("World is null");
        }

        this.stopWorkers();

        this.world = world;
        this.renderPassManager = renderPassManager;
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        ForgeBlockRenderer.init();

        this.startWorkers();
    }

    /**
     * Returns the "optimal" number of threads to be used for chunk build tasks. This is always at least one thread,
     * but can be up to the number of available processor threads on the system.
     */
    private static int getOptimalThreadCount() {
        return Mth.clamp(Math.max(getMaxThreadCount() / 3, getMaxThreadCount() - 6), 1, 10);
    }

    private static int getThreadCount() {
        int requested = SodiumClientMod.options().performance.chunkBuilderThreads;
        return requested == 0 ? getOptimalThreadCount() : Math.min(requested, getMaxThreadCount());
    }

    public static int getMaxThreadCount() {
        int totalCores = Runtime.getRuntime().availableProcessors();
        long memoryMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        // always allow at least one builder regardless of heap size
        int maxBuilders = Math.max(1, (int)(memoryMb / MBS_PER_CHUNK_BUILDER));
        // choose the total CPU cores or the number of builders the heap permits, whichever is smaller
        return Math.min(totalCores, maxBuilders);
    }

    /**
     * Add a completion handler to the given future task so that the result will be processed on the main thread.
     */
    public void handleCompletion(CompletableFuture<ChunkBuildResult<T>> future) {
        future.whenComplete((res, ex) -> {
            if (ex != null) {
                this.failureQueue.add(ex);
            } else if (res != null) {
                this.enqueueUpload(res);
            }
        });
    }

    /**
     * Creates a rebuild task and defers it to the work queue. When the task is completed, it will be moved onto the
     * completed uploads queued which will then be drained during the next available synchronization point with the
     * main thread.
     * @param render The render to rebuild
     */
    public void deferSort(ChunkRenderContainer<T> render) {
        handleCompletion(this.scheduleSortTaskAsync(render));
    }


    /**
     * Enqueues the build task result to the pending result queue to be later processed during the next available
     * synchronization point on the main thread.
     * @param result The build task's result
     */
    public void enqueueUpload(ChunkBuildResult<T> result) {
        this.uploadQueue.add(result);
    }

    /**
     * Schedules the rebuild task asynchronously on the worker pool, returning a future wrapping the task.
     * @param render The render to rebuild
     * @return a future representing the rebuild task, or null if the chunk section is empty
     */
    @Nullable
    public CompletableFuture<ChunkBuildResult<T>> scheduleRebuildTaskAsync(ChunkRenderContainer<T> render) {
        ChunkRenderBuildTask<T> task = this.createRebuildTask(render);

        if(task != null) {
            return this.schedule(task);
        } else {
            return null;
        }
    }

    /**
     * Schedules the rebuild task asynchronously on the worker pool, returning a future wrapping the task.
     * @param render The render to rebuild
     */
    public CompletableFuture<ChunkBuildResult<T>> scheduleSortTaskAsync(ChunkRenderContainer<T> render) {
        return this.schedule(this.createSortTask(render));
    }

    /**
     * Creates a task to rebuild the geometry of a {@link ChunkRenderContainer}.
     * @param render The render to rebuild
     */
    private ChunkRenderBuildTask<T> createRebuildTask(ChunkRenderContainer<T> render) {
        render.cancelRebuildTask();

        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), this.sectionCache);

        if (context == null) {
            return null;
        } else {
            return new ChunkRenderRebuildTask<>(render, context, render.getRenderOrigin()).withCameraPosition(this.cameraPosition);
        }
    }

    private ChunkRenderBuildTask<T> createSortTask(ChunkRenderContainer<T> render) {
        render.cancelRebuildTask();

        return new ChunkRenderTranslucencySortTask<>(render, render.getRenderOrigin(), this.cameraPosition);
    }

    public void onChunkDataChanged(int x, int y, int z) {
        this.sectionCache.invalidate(x, y, z);
    }

    private class WorkerRunnable implements Runnable {
        private final AtomicBoolean running = ChunkBuilder.this.running;

        // The re-useable build buffers used by this worker for building chunk meshes
        private final ChunkBuildBuffers bufferCache;

        // Making this thread-local provides a small boost to performance by avoiding the overhead in synchronizing
        // caches between different CPU cores
        private final ChunkRenderCacheLocal cache;

        public WorkerRunnable(ChunkBuildBuffers bufferCache, ChunkRenderCacheLocal cache) {
            this.bufferCache = bufferCache;
            this.cache = cache;
        }

        @Override
        public void run() {
            // Run until the chunk builder shuts down
            while (this.running.get()) {
                WrappedTask<T> job = this.getNextJob();

                // If the job is null or no longer valid, keep searching for a task
                if (job == null || job.isCancelled()) {
                    continue;
                }

                ChunkBuildResult<T> result;

                try {
                    // Perform the build task with this worker's local resources and obtain the result
                    result = job.task.performBuild(this.cache, this.bufferCache, job);
                } catch (Throwable e) {
                    // Propagate any exception from chunk building
                    job.future.completeExceptionally(e);
                    SodiumClientMod.logger().error("Chunk build failed", e);
                    continue;
                } finally {
                    job.task.releaseResources();
                }

                // The result can be null if the task is cancelled
                if (result != null) {
                    // Notify the future that the result is now available
                    job.future.complete(result);
                } else if (!job.isCancelled()) {
                    // If the job wasn't cancelled and no result was produced, we've hit a bug
                    job.future.completeExceptionally(new RuntimeException("No result was produced by the task"));
                }
            }
        }

        /**
         * Returns the next task which this worker can work on or blocks until one becomes available. If no tasks are
         * currently available, it will wait on {@link ChunkBuilder#jobNotifier} field until notified.
         */
        private WrappedTask<T> getNextJob() {
            WrappedTask<T> job = ChunkBuilder.this.buildQueue.poll();

            if (job == null) {
                synchronized (ChunkBuilder.this.jobNotifier) {
                    try {
                        ChunkBuilder.this.jobNotifier.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            return job;
        }
    }

    private static class WrappedTask<T extends ChunkGraphicsState> implements CancellationSource {
        private final ChunkRenderBuildTask<T> task;
        private final CompletableFuture<ChunkBuildResult<T>> future;

        private WrappedTask(ChunkRenderBuildTask<T> task) {
            this.task = task;
            this.future = new CompletableFuture<>();
        }

        @Override
        public boolean isCancelled() {
            return this.future.isCancelled();
        }
    }
}
