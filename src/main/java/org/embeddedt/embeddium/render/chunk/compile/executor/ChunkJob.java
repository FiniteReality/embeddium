package org.embeddedt.embeddium.render.chunk.compile.executor;

import org.embeddedt.embeddium.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.util.task.CancellationToken;

public interface ChunkJob extends CancellationToken {
    void execute(ChunkBuildContext context);

    boolean isStarted();
}
