package protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import file.Chunk;
import file.ChunkKey;

public class ChunkRestoreSynchronizer {

    // recorded CHUNK messages by the peer to know if it's necessary to send a CHUNK message after the random wait
    private ConcurrentHashMap<ChunkKey, Boolean> receivedChunkMsgs;

    // received CHUNKs
    private ConcurrentHashMap<String, Set<Chunk>> receivedChunks;

    public ChunkRestoreSynchronizer() {
        this.receivedChunkMsgs = new ConcurrentHashMap<>();
        this.receivedChunks = new ConcurrentHashMap<>();
    }

    public void listenToChunkMsg(ChunkKey chunkKey) {
        receivedChunkMsgs.put(chunkKey, false);
    }

    public void stopListenToChunkMsg(ChunkKey chunkKey) {
        receivedChunkMsgs.remove(chunkKey);
    }

    public boolean hasReceivedChunkMsg(ChunkKey chunkKey) {
        return receivedChunkMsgs.get(chunkKey);
    }

    public void chunkMsgReceived(ChunkKey chunkKey) {
        // if peer is listening for chunk msgs in order to avoid sending the same
        // message
        receivedChunkMsgs.replace(chunkKey, true);
    }

    public void chunkReceived(Chunk chunk) {
        // if this peer is the one restoring the file related to this chunk
        Set<Chunk> chunks = receivedChunks.get(chunk.getFileID());
        if (chunks == null)
            return;

        synchronized (chunks) {
            chunks.add(chunk);
        }
    }

    public void restoreFile(String fileID) {
        receivedChunks.putIfAbsent(fileID, new HashSet<Chunk>());
    }

    public boolean isRestoringFile(String fileID) {
        return receivedChunks.containsKey(fileID);
    }

    public void finishedRestoreFile(String fileID) {
        receivedChunks.remove(fileID);
    }

    /**
     * Check if all chunks of a file have been received
     */
    public List<Chunk> allChunksReceived(String fileID) {
        Set<Chunk> chunks = receivedChunks.get(fileID);

        if (chunks == null)
            return null;

        synchronized (chunks) {
            List<Integer> chunkNumbers = new ArrayList<>();
            for (Chunk c : chunks) {
                chunkNumbers.add(c.getNumber());
                if (c.getNumber() == chunks.size() - 1)
                    if (c.getSize() == Chunk.MAX_SIZE)
                        return null;
            }

            Collections.sort(chunkNumbers);

            for (int i = 0; i < chunkNumbers.size(); i++)
                if (chunkNumbers.get(i) != i)
                    return null;

            return new ArrayList<>(receivedChunks.remove(fileID));
        }
    }

    public List<Chunk> getChunks(String fileID) {
        Set<Chunk> chunks = receivedChunks.get(fileID);
        if (chunks == null)
            return null;

        return new ArrayList<>(chunks);
    }
}