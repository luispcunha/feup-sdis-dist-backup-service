package protocol;

import java.util.concurrent.ConcurrentHashMap;

import file.ChunkKey;

public class SpaceReclaimSynchronizer {
    /**
     * Chunks for which a PUTCHUNK message has already been sent
     */
    private ConcurrentHashMap<ChunkKey, Boolean> receivedPutChunkMsgs;

    public SpaceReclaimSynchronizer() {
        this.receivedPutChunkMsgs = new ConcurrentHashMap<>();
    }

    public void listenToPutChunkMsg(ChunkKey chunkKey) {
        receivedPutChunkMsgs.put(chunkKey, false);
    }

    public void stopListenToPutChunkMsg(ChunkKey chunkKey) {
        receivedPutChunkMsgs.remove(chunkKey);
    }

    public boolean hasReceivedPutChunkMsg(ChunkKey chunkKey) {
        return receivedPutChunkMsgs.get(chunkKey);
    }

    public void putChunkReceived(ChunkKey chunkKey) {
        receivedPutChunkMsgs.replace(chunkKey, true);
    }
}