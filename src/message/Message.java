package message;

public class Message {

    public enum Type {
        PUTCHUNK, STORED, GETCHUNK, CHUNK, DELETE, REMOVED, STARTUP, DELETED, UNKNOWN;
    }

    private String version;
    private Type type;
    private int senderID;
    private String fileID;
    private int chunkNo;
    private int repDegree;
    private byte[] body;

    public Message(String version, Type type, int senderID, String fileID, int chunkNo, int replicationDegree, byte[] body) {
        this.version = version;
        this.type = type;
        this.senderID = senderID;
        this.fileID = fileID;
        this.chunkNo = chunkNo;
        this.repDegree = replicationDegree;
        this.body = body;
    }

    public String getHeader() {
        String header = version + " " + type.toString() + " "
                + String.valueOf(senderID) + " "
                + ((fileID != null) ? (fileID + " ") : "")
                + ((chunkNo != -1) ? (String.valueOf(chunkNo) + " ") : "")
                + ((repDegree != -1) ? (String.valueOf(repDegree) + " ") : "");

        return header;
    }

    public byte[] toBytes() {
        String header = version + " " +
            type.toString() + " " +
            String.valueOf(senderID) + " " +
            fileID + " " +
            ((chunkNo != -1) ? (String.valueOf(chunkNo) + " ") : "") +
            ((repDegree != -1) ? (String.valueOf(repDegree) + " ") : "") +
            "\r\n\r\n";

        byte[] headerBytes = header.getBytes();

        if (this.body == null)
            return headerBytes;

        byte[] messageBytes = new byte[headerBytes.length + this.body.length];

        System.arraycopy(headerBytes, 0, messageBytes, 0, headerBytes.length);
        System.arraycopy(this.body, 0, messageBytes, headerBytes.length, this.body.length);

        return messageBytes;
    }

    public String getVersion() {
        return this.version;
    }

    public Type getType() {
        return this.type;
    }

    public int getSenderID() {
        return this.senderID;
    }

    public String getFileID() {
        return this.fileID;
    }

    public int getChunkNumber() {
        return this.chunkNo;
    }

    public int getRepDegree() {
        return this.repDegree;
    }

    public byte[] getBody() {
        return this.body;
    }
}
