package protocol;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import file.ChunkKey;

public class ChunkBackupSynchronizer {
    // peers that have confirmed storing a chunk
    private ConcurrentHashMap<ChunkKey, Set<Integer>> receivedStored;

    public ChunkBackupSynchronizer() {
        this.receivedStored = new ConcurrentHashMap<>();
    }

    /**
     * Start recording stored messages for a given chunk
     */
    public void listenToStored(ChunkKey chunkKey) {
        this.receivedStored.put(chunkKey, new HashSet<Integer>());
    }

    /**
     * Stop recording stored messages for a given chunk
     */
    public void stopListenToStored(ChunkKey chunkKey) {
        this.receivedStored.remove(chunkKey);
    }

    /**
     * Check if is recording stored messages for a given chunk
     */
    public boolean isListeningToStored(ChunkKey chunkKey) {
        return this.receivedStored.containsKey(chunkKey);
    }

    /**
     * Record that stored message was received for a given chunk from a given peer
     */
    public void receivedStored(ChunkKey chunkKey, int peerID) {
        Set<Integer> peers = this.receivedStored.get(chunkKey);

        if (peers == null)
            return;

        synchronized (peers) {
            peers.add(peerID);
        }
    }

    /**
     * Number of stored messages (from different peers) that have been received. Corresponds to the replication degree
     */
    public int getNumStored(ChunkKey chunkKey) {
        Set<Integer> peers = this.receivedStored.get(chunkKey);

        if (peers == null) {
            return 0;
        }

        int numStored = 0;
        synchronized (peers) {
            numStored = peers.size();
            return numStored;
        }
    }

    /**
     * Get peers that confirmed that are backing up a given chunk
     */
    public Set<Integer> getReplicationPeers(ChunkKey chunkKey) {
        return this.receivedStored.get(chunkKey);
    }
}