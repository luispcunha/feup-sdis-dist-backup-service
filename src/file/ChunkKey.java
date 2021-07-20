package file;

import java.util.Objects;

public class ChunkKey {

    private final String fileId;
    private final int chunkNo;

    public ChunkKey(String fileId, int chunkNo) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
    }

    public String getFileID() {
        return this.fileId;
    }

    public int getNumber() {
        return this.chunkNo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, chunkNo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true; // are the references equal
        if (o == null)
            return false; // is the other object null
        if (getClass() != o.getClass())
            return false; // both objects the same class

        ChunkKey cKey = (ChunkKey) o; // cast the other object

        return fileId.equals(cKey.getFileID()) && chunkNo == cKey.getNumber(); // actual comparison
    }
}