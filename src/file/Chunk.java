package file;

public class Chunk implements Comparable<Chunk> {
    public static final int MAX_SIZE = 64000;
    private ChunkKey key;
    private byte[] content;

    public Chunk(String fileId, int number, byte[] content) {
        this.key = new ChunkKey(fileId, number);
        this.content = content;
    }

    public String getFileID() {
        return this.key.getFileID();
    }

    public int getNumber() {
        return this.key.getNumber();
    }

    public ChunkKey getKey() {
        return this.key;
    }

    public byte[] getContent() {
        return this.content;
    }

    public int getSize() {
        return this.content.length;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true; // are the references equal
        if (o == null)
            return false; // is the other object null
        if (getClass() != o.getClass())
            return false; // both objects the same class

        Chunk chunk = (Chunk) o; // cast the other object

        return this.key.equals(chunk.key); // actual comparison
    }

    @Override
    public int compareTo(Chunk other) {
        if (this.getNumber() < other.getNumber())
            return -1;
        if (this.getNumber() == other.getNumber())
            return 0;

        return 1;
    }


}