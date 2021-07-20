package filesystem;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Information regarding a file for which a peer requested backup.
 */
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 7911687701238652479L;

    private final String path;
    private final String id;
    private final int desiredRepDegree;

    // peers that are backing up each of the files' chunks
    private ConcurrentHashMap<Integer, Set<Integer>> chunks;

    public FileInfo(String path, String id, int repDegree) {
        this.path = path;
        this.id = id;
        this.desiredRepDegree = repDegree;
        this.chunks = new ConcurrentHashMap<>();
    }

    public void addChunk(int chunkNo, int peerID) {
        chunks.putIfAbsent(chunkNo, new HashSet<Integer>());

        Set<Integer> peers = chunks.get(chunkNo);

        synchronized (peers) {
            peers.add(peerID);
        }
    }

    public boolean removeChunk(int chunkNo, int peerID) {
        boolean removed = false;
        Set<Integer> peers = chunks.get(chunkNo);

        if (peers != null) {
            synchronized (peers) {
                removed = peers.remove(Integer.valueOf(peerID));
            }
        }

        return removed;
    }

    public List<Integer> getChunkPeers(int chunkNo) {
        Set<Integer> set = chunks.get(Integer.valueOf(chunkNo));
        if (set == null)
            return null;
        return new ArrayList<Integer>(set);
    }

    public List<Integer> getChunks() {
        return new ArrayList<Integer>(chunks.keySet());
    }

    @Override
    public String toString() {
        String ret = "";
        ret += "  ID : " + id + "\n";
        ret += "  Path : " + path + "\n";
        ret += "  Desired RD : " + String.valueOf(desiredRepDegree) + "\n";

        for (ConcurrentHashMap.Entry<Integer, Set<Integer>> entry : chunks.entrySet()) {
            int rd;
            synchronized (entry.getValue()) {
                rd = entry.getValue().size();
            }

            ret += "    Chunk No : " + String.valueOf(entry.getKey()) + "\n";
            ret += "      Perceived RD : " + String.valueOf(rd) + "\n";
            ret += "      Peers :";
            for (Integer peer : entry.getValue())
                ret += " " + String.valueOf(peer);
            ret += "\n";
        }

        return ret;
    }
}