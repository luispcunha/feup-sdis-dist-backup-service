package protocol;

import java.io.IOException;

import channel.MulticastChannel;
import file.ChunkKey;
import message.Message;
import message.Messages;
import peer.Peer;
import util.Log;

public class ChunkRestoreInitiator implements Runnable {

    private Peer peer;
    private ChunkKey chunkKey;

    public ChunkRestoreInitiator(Peer peer, ChunkKey chunkKey) {
        this.peer = peer;
        this.chunkKey = chunkKey;
    }

    @Override
    public void run() {
        Message message;

        if (this.peer.getVersion().equals("2.0"))
            message = Messages.getEnhancedGetChunkMessage(this.peer.getID(), this.chunkKey);
        else
            message = Messages.getGetChunkMessage(this.peer.getID(), this.chunkKey);

        MulticastChannel mcChannel = this.peer.getMCChannel();

        try {
            mcChannel.broadcast(message);
            Log.logSentMC(message.getHeader());
        } catch (IOException e) {
            Log.logError("Unable to send " + message.getHeader());
            e.printStackTrace();
        }
    }
}