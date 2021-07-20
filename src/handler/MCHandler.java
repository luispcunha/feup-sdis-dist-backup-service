package handler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
import protocol.ChunkBackupInitiator;
import protocol.ChunkRestoreSynchronizer;
import protocol.SpaceReclaimSynchronizer;
import util.Log;

public class MCHandler extends Handler {

    public MCHandler(Peer peer, DatagramPacket packet) {
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
            return;
        }

        Log.logReceivedMC(message.getHeader());

        switch (message.getType()) {
            case DELETE:
                this.handleDeleteMsg(message);
                break;
            case STORED:
                this.handleStoredMsg(message);
                break;
            case GETCHUNK:
                if (message.getVersion().equals("2.0") && this.peer.getVersion().equals("2.0"))
                    this.handleGetChunkEnhMsg(message);
                else
                    this.handleGetChunkMsg(message);
                break;
            case REMOVED:
                this.handleRemovedMsg(message);
                break;
            case STARTUP:
                if (this.peer.getVersion().equals("2.0"))
                    this.handleStartupMsg(message);
                break;
            case DELETED:
                if (this.peer.getVersion().equals("2.0"))
                    this.handleDeletedMsg(message);
            default:
                break;
        }
    }

    public void handleDeleteMsg(Message msg) {
        String fileID = msg.getFileID();

        if (! this.peer.getState().isStoredFile(fileID))
            return;

        this.peer.getState().deleteStoredFile(fileID);
        this.peer.getFileSystem().deleteFileChunks(fileID);

        if (this.peer.getVersion().equals("2.0")) {
            Message response = Messages.getDeletedMessage(peer.getID(), fileID);
            try {
                peer.getMCChannel().broadcast(response);
                Log.logSentMC(response.getHeader());
            } catch (IOException e) {
                Log.logError("Unable to send " + response.getHeader());
                return;
            }
        }
    }

    public void handleStoredMsg(Message msg) {
        ChunkKey chunkKey = new ChunkKey(msg.getFileID(), msg.getChunkNumber());
        int peerID = msg.getSenderID();

        PeerState state = this.peer.getState();

        // initiator peer
        state.addFileInfo(chunkKey.getFileID(), chunkKey.getNumber(), peerID);

        // other peers
        state.addPeerBackingUpStoredChunk(chunkKey.getFileID(), chunkKey.getNumber(), peerID);

        if (this.peer.getVersion().equals("2.0")) {
            peer.getChunkBackupSync().receivedStored(chunkKey, peerID);
        }
    }

    public void handleGetChunkMsg(Message msg) {
        ChunkKey chunkKey = new ChunkKey(msg.getFileID(), msg.getChunkNumber());

        PeerState state = this.peer.getState();

        if (! state.isStoredChunk(msg.getFileID(), msg.getChunkNumber()))
            return;

        ChunkRestoreSynchronizer chunkRestoreSync = this.peer.getChunkRestoreSync();

        chunkRestoreSync.listenToChunkMsg(chunkKey);

        FileSystem fs = this.peer.getFileSystem();

        Chunk chunk;
        try {
            chunk = fs.loadChunk(chunkKey.getFileID(), chunkKey.getNumber());
        } catch (FileNotFoundException e) {
            Log.logError(e.toString());
            return;
        }

        Message responseMsg = Messages.getChunkMessage(this.peer.getID(), chunk);
        MulticastChannel mdrChannel = this.peer.getMDRChannel();

        int backoffTime = new Random().nextInt(400);
        Log.logBackoff(backoffTime, "sending CHUNK message for chunk " + chunk.getNumber());

        this.peer.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (chunkRestoreSync.hasReceivedChunkMsg(chunkKey)) {
                    chunkRestoreSync.stopListenToChunkMsg(chunkKey);
                    return;
                }

                try {
                    mdrChannel.broadcast(responseMsg);
                    Log.logSentMDR(responseMsg.getHeader());
                } catch (IOException e) {
                    Log.logError("Unable to send " + responseMsg.getHeader());
                } finally {
                    chunkRestoreSync.stopListenToChunkMsg(chunkKey);
                }
            }
        }, backoffTime);
    }

    public void handleGetChunkEnhMsg(Message msg) {
        ChunkKey chunkKey = new ChunkKey(msg.getFileID(), msg.getChunkNumber());
        PeerState state = this.peer.getState();

        if (!state.isStoredChunk(msg.getFileID(), msg.getChunkNumber()))
            return;

        ChunkRestoreSynchronizer chunkRestoreSync = this.peer.getChunkRestoreSync();
        chunkRestoreSync.listenToChunkMsg(chunkKey);


        MulticastChannel mdrChannel = this.peer.getMDRChannel();

        int backoffTime = new Random().nextInt(400);
        Log.logBackoff(backoffTime, "sending enhanced CHUNK message for chunk " + chunkKey.getNumber());

        this.peer.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (chunkRestoreSync.hasReceivedChunkMsg(chunkKey)) {
                    chunkRestoreSync.stopListenToChunkMsg(chunkKey);
                    return;
                }

                Chunk chunk;
                try {
                    chunk = peer.getFileSystem().loadChunk(chunkKey.getFileID(), chunkKey.getNumber());
                } catch (FileNotFoundException e) {
                    Log.logError(e.toString());
                    return;
                }

                ServerSocket serverSocket;
                try {
                    serverSocket = new ServerSocket(0);
                    serverSocket.setSoTimeout(1000);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    return;
                }

                Message responseMsg = Messages.getEnhancedChunkMessage(peer.getID(), chunkKey, serverSocket.getLocalPort());

                Log.log("Waiting for a connection at port " + serverSocket.getLocalPort() + " to send chunk " + chunkKey.getNumber());

                try {
                    mdrChannel.broadcast(responseMsg);
                    Log.logSentMDR(responseMsg.getHeader());
                } catch (IOException e) {
                    Log.logError("Unable to send " + responseMsg.getHeader());
                } finally {
                    chunkRestoreSync.stopListenToChunkMsg(chunkKey);
                }

                try {
                    Socket connectionSocket = serverSocket.accept();
                    OutputStream outStream = connectionSocket.getOutputStream();
                    outStream.write(chunk.getContent());
                    outStream.flush();
                    connectionSocket.close();
                } catch (SocketTimeoutException e) {
                    Log.logError("Time for accepting connections on TCP server socket has elapsed");
                } catch (IOException e) {
                    Log.logError("Failed accepting connection and sending chunk");
                } finally {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.logError("Failed closing server socket");
                    }
                }
            }
        }, backoffTime);
    }

    public void handleRemovedMsg(Message msg) {
        ChunkKey chunkKey = new ChunkKey(msg.getFileID(), msg.getChunkNumber());
        int peerID = msg.getSenderID();

        filesystem.PeerState state = this.peer.getState();
        state.removeFileInfo(msg.getFileID(), msg.getChunkNumber(), peerID);
        state.removePeerBackingUpStoredChunk(msg.getFileID(), msg.getChunkNumber(), peerID);

        int desiredRepDeg = state.getStoredFileDesiredRepDegree(msg.getFileID());
        int perceivedRepDeg = state.getStoredChunkPerceivedRepDegree(msg.getFileID(), msg.getChunkNumber());

        if (perceivedRepDeg >= desiredRepDeg)
            return;

        Log.log("Perceived RD (" + perceivedRepDeg + ") is lower than the desired (" + desiredRepDeg + ")");

        SpaceReclaimSynchronizer spaceReclaimSync = this.peer.getSpaceReclaimSync();
        spaceReclaimSync.listenToPutChunkMsg(chunkKey);

        int backoffTime = new Random().nextInt(400);
        Log.logBackoff(backoffTime, " sending PUTCHUNK message for chunk " + chunkKey.getNumber());

        this.peer.scheduleTask(new Runnable() {
            @Override
            public void run() {
                    if (spaceReclaimSync.hasReceivedPutChunkMsg(chunkKey)) {
                        spaceReclaimSync.stopListenToPutChunkMsg(chunkKey);
                        return;
                    }

                    Chunk chunk;
                    try {
                        chunk = peer.getFileSystem().loadChunk(chunkKey.getFileID(), chunkKey.getNumber());
                    } catch (FileNotFoundException e) {
                        Log.logError("Failed loading chunk from file system");
                        spaceReclaimSync.stopListenToPutChunkMsg(chunkKey);
                        return;
                    }

                    peer.submitWorker(new ChunkBackupInitiator(peer, chunk, desiredRepDeg, 5, 1000));

                    spaceReclaimSync.stopListenToPutChunkMsg(chunkKey);
            }
        }, backoffTime);
    }

    public void handleStartupMsg(Message msg) {
        Set<String> files = peer.getState().getUndeletedFilesFrom(msg.getSenderID());
        if (files == null)
            return;

        for (String fileID : files) {
            Message response = Messages.getEnhancedDeleteMessage(peer.getID(), fileID);
            try {
                peer.getMCChannel().broadcast(response);
                Log.logSentMC(response.getHeader());
            } catch (IOException e) {
                Log.logError("Unable to send " + response.getHeader());
            }
        }
    }

    public void handleDeletedMsg(Message msg) {
        int peerID = msg.getSenderID();
        String fileID = msg.getFileID();

        this.peer.getState().peerDeletedFile(peerID, fileID);
    }
}