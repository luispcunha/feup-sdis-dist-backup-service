package filesystem;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Information regarding a chunk being backed up by a peer.
 */
public class ChunkInfo implements Serializable {

    private static final long serialVersionUID = 7493772498312054194L;

    private final int size; // size of the chunk in bytes
    private Set<Integer> peers; // other peers that are currently backing up this chunk

    public ChunkInfo(int size) {
        this.size = size;
        this.peers = new HashSet<Integer>();
    }

    public int getSize() {
        return this.size;
    }

    public synchronized void addPeer(int peerID) {
        peers.add(peerID);
    }

    public synchronized boolean removePeer(int peerID) {
        return peers.remove(peerID);
    }

    public synchronized int getPerceivedRepDegree() {
        return this.peers.size() + 1;
    }

    @Override
    public String toString() {
        String ret = "";

        ret += "      Perceived RD : " + this.getPerceivedRepDegree() + "\n";
        ret += "      Size : " + size + " bytes\n";
        ret += "      Peers :";

        synchronized (this) {
            for (Integer peer : peers)
                ret += " " + peer;
        }

        ret += "\n";

        return ret;
    }
}