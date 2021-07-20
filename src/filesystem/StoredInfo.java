package filesystem;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information regarding the chunks of a file which are being backed up by a peer.
 */
public class StoredInfo implements Serializable {

    private static final long serialVersionUID = 6564061166107827209L;

    private final int repDegree;
    private ConcurrentHashMap<Integer, ChunkInfo> chunks;

    public StoredInfo(int repDegree) {
        chunks = new ConcurrentHashMap<>();
        this.repDegree = repDegree;
    }

    public int getRepDegree() {
        return this.repDegree;
    }

    public ConcurrentHashMap<Integer, ChunkInfo> getChunks() {
        return chunks;
    }

    public int getChunkPerceivedRepDegree(int chunkNo) {
        ChunkInfo chunkInfo = chunks.get(chunkNo);

        if (chunkInfo == null)
            return -1;
        else
            return chunkInfo.getPerceivedRepDegree();
    }

    public boolean hasChunks() {
        return ! chunks.isEmpty();
    }

    public boolean isStored(int chunkNo) {
        return chunks.containsKey(chunkNo);
    }

    public boolean addChunk(int chunkNo, int size) {
        return chunks.putIfAbsent(chunkNo, new ChunkInfo(size)) == null;
    }

    public void removeChunk(int chunkNo) {
        chunks.remove(chunkNo);
    }

    public boolean addPeerBackingUpChunk(int chunkNo, int peerId) {
        ChunkInfo chunkInfo = chunks.get(chunkNo);

        if (chunkInfo == null)
            return false;

        chunkInfo.addPeer(peerId);

        return true;
    }

    public boolean removePeerBackingUpChunk(int chunkNo, int peerId) {
        ChunkInfo chunkInfo = chunks.get(chunkNo);

        if (chunkInfo == null)
            return false;

        chunkInfo.addPeer(peerId);

        return chunkInfo.removePeer(peerId);
    }

    @Override
    public String toString() {
        String ret = "  Desired RD : " + String.valueOf(repDegree) + "\n";

        for (ConcurrentHashMap.Entry<Integer, ChunkInfo> entry : chunks.entrySet()) {
            ret += "    Chunk No : " + String.valueOf(entry.getKey()) + "\n";
            ret += entry.getValue().toString();
        }
        return ret;
    }
}
