package handler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Random;
import java.util.Set;

import channel.MulticastChannel;
import file.Chunk;
import file.ChunkKey;
import filesystem.FileSystem;
import filesystem.PeerState;
import message.InvalidMessageException;
import message.Message;
import message.Messages;
import peer.Peer;
import util.Log;

public class MDBHandler extends Handler {

    public MDBHandler(Peer peer, DatagramPacket packet) {
        super(peer, packet);
    }

    @Override
    public void run() {
        Message message;
        try {
            message = Messages.parseMessage(packet.getData(), packet.getLength());
        } catch (InvalidMessageException e) {
            Log.logError(e.toString());
            return;
        }

        if (peer.getID() == message.getSenderID()) {
            // reply with STORED to a PUTCHUNK sent by itself if the PUTCHUNK refers to a file whose backup was requested by another peer, so that other peers know that this peer is backing up the chunk (and not requesting the backup)
            if (message.getType() == Message.Type.PUTCHUNK
                && peer.getState().isStoredChunk(message.getFileID(), message.getChunkNumber())) {
                Message response = Messages.getStoredMessage(peer.getID(), new ChunkKey(message.getFileID(), message.getChunkNumber()));
                try {
                    peer.getMCChannel().broadcast(response);
                    Log.logSentMC(response.getHeader());
                } catch (IOException e) {
                    Log.logError("Unable to send " + response.getHeader());
                }
            }
            return;
        }

        Log.logReceivedMDB(message.getHeader());

        switch (message.getType()) {
            case PUTCHUNK:
                if (message.getVersion().equals("2.0") && this.peer.getVersion().equals("2.0"))
                    this.handlePutchunkEnhMsg(message);
                else
                    this.handlePutchunkMsg(message);
                break;
            default:
                break;
        }
    }

    public void handlePutchunkMsg(Message msg) {
        Chunk chunk = new Chunk(msg.getFileID(), msg.getChunkNumber(), msg.getBody());
        FileSystem fs = this.peer.getFileSystem();
        PeerState state = this.peer.getState();

        state.removeUndeletedFile(msg.getFileID()); // in case

        this.peer.getSpaceReclaimSync().putChunkReceived(chunk.getKey());

        if (state.getAvailableSpace() < chunk.getSize())
            return;

        if (state.isBackupFile(chunk.getFileID()))
            return;

        if (! state.isStoredChunk(chunk.getFileID(), chunk.getNumber())) {
            try {
                fs.storeChunk(chunk);
                state.addStoredChunkInfo(chunk.getFileID(), msg.getRepDegree(), chunk.getNumber(), msg.getSenderID(), chunk.getSize());
            } catch (IOException e) {
                Log.logError("Failed storing chunk");
                return;
            }
        }

        int backoffTime = new Random().nextInt(400);
        Log.logBackoff(backoffTime, "before sending STORED message for chunk " + chunk.getNumber());

        MulticastChannel mcChannel = this.peer.getMCChannel();
        Peer peer = this.peer;

        this.peer.scheduleTask(new Runnable() {
            @Override
            public void run() {
                Message message = Messages.getStoredMessage(peer.getID(), chunk.getKey());
                try {
                    mcChannel.broadcast(message);
                    Log.logSentMC(message.getHeader());
                } catch (IOException e) {
                    Log.logError("Unable to send " + message.getHeader());
                }
            }
        }, backoffTime);
    }

    public void handlePutchunkEnhMsg(Message msg) {
        Chunk chunk = new Chunk(msg.getFileID(), msg.getChunkNumber(), msg.getBody());
        FileSystem fs = this.peer.getFileSystem();
        PeerState state = this.peer.getState();

        state.removeUndeletedFile(msg.getFileID());

        this.peer.getSpaceReclaimSync().putChunkReceived(chunk.getKey());

        int desiredRepDeg = msg.getRepDegree();

        if (state.getAvailableSpace() < chunk.getSize())
            return;

        if (state.isBackupFile(chunk.getFileID()))
            return;



        peer.getChunkBackupSync().listenToStored(chunk.getKey());

        int backoffTime = new Random().nextInt(400);
        Log.logBackoff(backoffTime, "before storing chunk " + chunk.getNumber());

        MulticastChannel mcChannel = this.peer.getMCChannel();
        Peer peer = this.peer;

        this.peer.scheduleTask(new Runnable() {
            @Override
            public void run() {

                int perceivedRepDeg = peer.getChunkBackupSync().getNumStored(chunk.getKey());

                Log.logRepDegree(desiredRepDeg, perceivedRepDeg, chunk.getNumber());
                Set<Integer> replicationPeers = peer.getChunkBackupSync().getReplicationPeers(chunk.getKey());
                peer.getChunkBackupSync().stopListenToStored(chunk.getKey());

                // if desired replication degree has already been achieved, don't store the chunk
                if (perceivedRepDeg >= desiredRepDeg) {
                    return;
                }

                if (! state.isStoredChunk(chunk.getFileID(), chunk.getNumber())) {
                    try {
                        fs.storeChunk(chunk);
                        state.addStoredChunkInfo(chunk.getFileID(), msg.getRepDegree(), chunk.getNumber(),
                                msg.getSenderID(), chunk.getSize());
                        if (replicationPeers != null) {
                            for (Integer peerID : replicationPeers) {
                                state.addPeerBackingUpStoredChunk(chunk.getFileID(), chunk.getNumber(), peerID);
                            }
                        }
                    } catch (IOException e) {
                        Log.logError("Failed to store chunk " + chunk.getNumber() + " for file " + chunk.getFileID());
                        return;
                    }
                }

                Message message = Messages.getStoredMessage(peer.getID(), chunk.getKey());
                try {
                    mcChannel.broadcast(message);
                    Log.logSentMC(message.getHeader());
                } catch (IOException e) {
                    Log.logError("Unable to send " + message.getHeader());
                }
            }
        }, backoffTime);
    }
}