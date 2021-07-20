package message;

import java.nio.ByteBuffer;
import java.util.Arrays;

import file.Chunk;
import file.ChunkKey;
import message.Message.Type;

public class Messages {
    public static Message parseMessage(byte[] msg, int msgLength) throws InvalidMessageException {
        String version = null;
        Type type = null;
        int senderID = -1;
        String fileID = null;
        int chunkNo = -1;
        int repDegree = -1;
        byte[] body = null;

        int lastCRLF = -1;
        for (int i = 0; i < msgLength; i++) {
            if (msg[i] == 0xd && msg[i + 1] == 0xa) {
                if (msg[i + 2] == 0xd && msg[i + 3] == 0xa) {
                    lastCRLF = i + 2;
                    break;
                }
            }
        }

        if (lastCRLF == -1) {
            throw new InvalidMessageException("Last CRLF doesn't exist.");
        }

        String header = new String(msg, 0, lastCRLF - 2);
        String[] lines = header.split("\r\n");

        if (lines.length > 1) {
            type = Type.UNKNOWN;
            new Message(version, type, senderID, fileID, chunkNo, repDegree, body);
        }

        String[] headerFields = lines[0].split("\\s+");

        version = headerFields[0];

        if (! (headerFields[1].equals("PUTCHUNK")
                || headerFields[1].equals("CHUNK")
                || headerFields[1].equals("GETCHUNK")
                || headerFields[1].equals("STORED")
                || headerFields[1].equals("DELETE")
                || headerFields[1].equals("REMOVED")
                || headerFields[1].equals("DELETED")
                || headerFields[1].equals("STARTUP"))) {
            type = Type.UNKNOWN;
            new Message(version, type, senderID, fileID, chunkNo, repDegree, body);
        } else {
            type = Type.valueOf(headerFields[1]);
        }


        senderID = Integer.parseInt(headerFields[2]);

        if (type != Type.STARTUP) {
            fileID = headerFields[3];
        }

        if (type != Type.DELETE && type != Type.DELETED && type != Type.STARTUP) {
            chunkNo = Integer.parseInt(headerFields[4]);
        }

        if (type == Type.PUTCHUNK) {
            repDegree = Integer.parseInt(headerFields[5]);
        }

        if (type == Type.PUTCHUNK || type == Type.CHUNK) {
            body = Arrays.copyOfRange(msg, lastCRLF + 2, msgLength);
        }

        return new Message(version, type, senderID, fileID, chunkNo, repDegree, body);
    }

    public static Message getPutChunkMessage(int senderID, Chunk chunk, int repDegree) {
        return new Message("1.0", Type.PUTCHUNK, senderID, chunk.getFileID(), chunk.getNumber(), repDegree, chunk.getContent());
    }

    public static Message getEnhancedPutChunkMessage(int senderID, Chunk chunk, int repDegree) {
        return new Message("2.0", Type.PUTCHUNK, senderID, chunk.getFileID(), chunk.getNumber(), repDegree,
                chunk.getContent());
    }

    public static Message getStoredMessage(int senderID, ChunkKey chunk) {
        return new Message("1.0", Type.STORED, senderID, chunk.getFileID(), chunk.getNumber(), -1, null);
    }

    public static Message getDeleteMessage(int senderID, String fileID) {
        return new Message("1.0", Type.DELETE, senderID, fileID, -1, -1, null);
    }

    public static Message getEnhancedDeleteMessage(int senderID, String fileID) {
        return new Message("2.0", Type.DELETE, senderID, fileID, -1, -1, null);
    }

    public static Message getChunkMessage(int senderID, Chunk chunk) {
        return new Message("1.0", Type.CHUNK, senderID, chunk.getFileID(), chunk.getNumber(), -1, chunk.getContent());
    }

    public static Message getEnhancedChunkMessage(int senderID, ChunkKey chunkKey, int socketPort) {
        return new Message("2.0", Type.CHUNK, senderID, chunkKey.getFileID(), chunkKey.getNumber(), -1, ByteBuffer.allocate(4).putInt(socketPort).array());
    }

    public static Message getGetChunkMessage(int senderID, ChunkKey chunkKey) {
        return new Message("1.0", Type.GETCHUNK, senderID, chunkKey.getFileID(), chunkKey.getNumber(), -1, null);
    }

    public static Message getEnhancedGetChunkMessage(int senderID, ChunkKey chunkKey) {
        return new Message("2.0", Type.GETCHUNK, senderID, chunkKey.getFileID(), chunkKey.getNumber(), -1, null);
    }

    public static Message getRemovedMessage(int senderID, ChunkKey chunkKey) {
        return new Message("1.0", Type.REMOVED, senderID, chunkKey.getFileID(), chunkKey.getNumber(), -1, null);
    }

    public static Message getStartupMessage(int senderID) {
        return new Message("2.0", Type.STARTUP, senderID, null, -1, -1, null);
    }

    public static Message getDeletedMessage(int senderID, String fileID) {
        return new Message("2.0", Type.DELETED, senderID, fileID, -1, -1, null);
    }
}