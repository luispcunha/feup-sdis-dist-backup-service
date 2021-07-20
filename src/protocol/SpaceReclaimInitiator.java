package protocol;

import java.io.IOException;

import channel.MulticastChannel;
import file.ChunkKey;
import message.Message;
import message.Messages;
import peer.Peer;
import util.Log;

public class SpaceReclaimInitiator implements Runnable {

    private Peer peer;
    private ChunkKey chunkKey;

    public SpaceReclaimInitiator(Peer peer, ChunkKey chunkKey) {
        this.peer = peer;
        this.chunkKey = chunkKey;
    }

    @Override
    public void run() {
        Message message = Messages.getRemovedMessage(this.peer.getID(), this.chunkKey);
        MulticastChannel mcChannel = this.peer.getMCChannel();

        try {
            mcChannel.broadcast(message);
            Log.logSentMC(message.getHeader());
        } catch (IOException e) {
            Log.logError("Unable to send " + message.getHeader());
        }
    }
}