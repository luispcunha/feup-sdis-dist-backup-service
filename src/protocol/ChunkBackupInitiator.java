package protocol;

import java.io.IOException;

import channel.MulticastChannel;
import file.Chunk;
import filesystem.PeerState;
import peer.Peer;
import util.Log;
import message.Message;
import message.Messages;

/**
 * Initiates the chunk backup subprotocol
 */
public class ChunkBackupInitiator implements Runnable {

    private Peer peer;
    private Chunk chunk;
    private int repDegree;
    private long time;
    private int numTries;

    /**
     * @param peer          peer for which the protocol is being executed
     * @param chunk         chunk to backup
     * @param repDegree     desired replication degree
     * @param numTries      max number of times to try executing the protocol
     * @param time          time to wait before next execution
     */
    public ChunkBackupInitiator(Peer peer, Chunk chunk, int repDegree, int numTries, long time) {
        this.peer = peer;
        this.chunk = chunk;
        this.repDegree = repDegree;
        this.time = time;
        this.numTries = numTries;
    }

    @Override
    public void run() {
        PeerState state = this.peer.getState();
        int currentRepDegree;

        // get current replication degree depending on whether the peer is the owner of the file or is
        // executing the procol after receiving a removed message
        if (state.isBackupFile(this.chunk.getFileID()))
            currentRepDegree = state.getBackupChunkPerceivedRepDegree(chunk.getFileID(), chunk.getNumber());
        else
            currentRepDegree = state.getStoredChunkPerceivedRepDegree(chunk.getFileID(), chunk.getNumber());

        Log.logRepDegree(this.repDegree, currentRepDegree, chunk.getNumber());

        // check if it's necessary to retry the protocol
        if (currentRepDegree >= this.repDegree) {
            Log.log("Backed up chunk " + chunk.getNumber() + " of file " + chunk.getFileID() + " with RD " + currentRepDegree);
            return;
        }

        if (this.numTries == 0) {
            if (state.isBackupFile(this.chunk.getFileID())) {
                if (currentRepDegree == 0) {
                    Log.log("Unable to backup chunk " + chunk.getNumber() + " of file " + chunk.getFileID() + ". RD = " + currentRepDegree);
                    state.deleteBackupFile(this.chunk.getFileID());
                } else {
                    Log.log("Backed up chunk " + chunk.getNumber() + " of file " + chunk.getFileID() + " with RD "
                            + currentRepDegree);
                }
            }
            return;
        }

        MulticastChannel mdbChannel = this.peer.getMDBChannel();

        Message message;
        if (this.peer.getVersion().equals("2.0"))
            message = Messages.getEnhancedPutChunkMessage(this.peer.getID(), this.chunk, this.repDegree);
        else
            message = Messages.getPutChunkMessage(this.peer.getID(), this.chunk, this.repDegree);

        try {
            mdbChannel.broadcast(message);
            Log.logSentMDB(message.getHeader());
        } catch (IOException e) {
            Log.logError("Unable to send " + message.getHeader());
        }

        long waitTime = this.time;

        this.numTries--;
        this.time *= 2;


        Log.logWaiting(waitTime, this.chunk.getNumber());

        this.peer.scheduleTask(this, waitTime);
    }
}