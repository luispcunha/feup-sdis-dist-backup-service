package protocol;

import java.io.IOException;

import channel.MulticastChannel;
import message.Message;
import peer.Peer;
import util.Log;

public class DeleteInitiator implements Runnable {

    private Peer peer;
    private Message message;
    private int numTries;
    private long rate;

    public DeleteInitiator(Peer peer, Message message, int numTries, long rate) {
        this.peer = peer;
        this.message = message;
        this.numTries = numTries;
        this.rate = rate;
    }

    @Override
    public void run() {
        MulticastChannel mcChannel = this.peer.getMCChannel();

        // if using the enhanced version, resend the DELETE only if necessary
        if (peer.getVersion().equals("2.0")) {
            if (! peer.getState().getUndeletedFiles().contains(message.getFileID()))
                return;
        }

        try {
            mcChannel.broadcast(message);
            Log.logSentMC(message.getHeader());
        } catch (IOException e) {
            Log.logError("Unable to send " + message.getHeader());
        }

        this.numTries--;
        if (this.numTries == 0)
            return;

        this.peer.scheduleTask(this, rate);
    }
}