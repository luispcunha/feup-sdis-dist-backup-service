package filesystem;


public class ChunkReclaim implements Comparable<ChunkReclaim> {
    private int chunkNo, diff, size;
    private String fileId;

    public ChunkReclaim(String fileId, int chunkNo, int diff, int size) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.diff = diff;
        this.size = size;
    }

    @Override
    public int compareTo(ChunkReclaim chunk) {
        if (this.diff > chunk.diff)
            return -1;
        else if (this.diff < chunk.diff)
            return 1;

        if (this.size > chunk.size)
            return -1;
        else if (this.size < chunk.size)
            return 1;

        return 0;
    }

    public String getFileId() {
        return this.fileId;
    }

    public int getChunkNo() {
        return this.chunkNo;
    }

    public int getSize() {
        return this.size;
    }
}