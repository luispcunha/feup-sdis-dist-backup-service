package handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import file.Chunk;
import file.ChunkKey;
import filesystem.FileSystem;
import message.InvalidMessageException;
import message.Message;
import message.Messages;
import peer.Peer;
import protocol.ChunkRestoreSynchronizer;
import util.Log;

public class MDRHandler extends Handler {

    public MDRHandler(Peer peer, DatagramPacket packet) {
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

        Log.logReceivedMDR(message.getHeader());

        switch (message.getType()) {
            case CHUNK:
                // if this isn't the peer restoring the file, keep track of CHUNK msgs received
                if (! peer.getChunkRestoreSync().isRestoringFile(message.getFileID())) {
                    peer.getChunkRestoreSync().chunkMsgReceived(new ChunkKey(message.getFileID(), message.getChunkNumber()));
                    return;
                }

                if (this.peer.getVersion().equals("2.0") && message.getVersion().equals("2.0"))
                    this.handleChunkEnhMsg(message);
                else
                    this.handleChunkMsg(message);
                break;
            default:
                break;
        }
    }

    public void handleChunkMsg(Message msg) {
        Chunk chunk = new Chunk(msg.getFileID(), msg.getChunkNumber(), msg.getBody());

        ChunkRestoreSynchronizer chunkRestoreSync = this.peer.getChunkRestoreSync();

        FileSystem fs = this.peer.getFileSystem();

        chunkRestoreSync.chunkReceived(chunk);

        if (chunkRestoreSync.isRestoringFile(chunk.getFileID())) {
            List<Chunk> chunks = chunkRestoreSync.allChunksReceived(chunk.getFileID());

            if (chunks == null)
                return;

            Log.log("Received all chunks of file " + chunk.getFileID());
            fs.restoreFile(chunks);
        }
    }

    public void handleChunkEnhMsg(Message msg) {

        ByteBuffer wrapped = ByteBuffer.wrap(msg.getBody());
        int port = wrapped.getInt();
        InetAddress address = this.packet.getAddress();


        Chunk chunk;
        Socket socket;
        try {
            Log.log("Connecting to server socket");
            socket = new Socket(address, port);
            InputStream inStream = socket.getInputStream();
            byte[] buf = new byte[64000];
            buf = inStream.readAllBytes();
            Log.log("Read chunk's contents from socket (" + buf.length + " bytes)");
            chunk = new Chunk(msg.getFileID(), msg.getChunkNumber(), buf);
            socket.close();
        } catch (IOException e) {
            Log.logError("failed to read contents from socket");
            return;
        }

        ChunkRestoreSynchronizer chunkRestoreSync = this.peer.getChunkRestoreSync();
        FileSystem fs = this.peer.getFileSystem();

        chunkRestoreSync.chunkReceived(chunk);

        if (chunkRestoreSync.isRestoringFile(chunk.getFileID())) {
            List<Chunk> chunks = chunkRestoreSync.allChunksReceived(chunk.getFileID());

            if (chunks == null)
                return;

            Log.log("Received all chunks of file " + chunk.getFileID());
            fs.restoreFile(chunks);
        }
    }
}