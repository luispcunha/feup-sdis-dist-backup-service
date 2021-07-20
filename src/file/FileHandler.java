package file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileHandler {
    public static final long MAX_NUM_CHUNKS = 1000000;

    private File file;
    private String id;
    private List<Chunk> chunks = new ArrayList<Chunk>();

    public FileHandler(String path) throws IOException, FileSizeException, NoSuchAlgorithmException {
        this.file = new File(path);

        if (file.length() > Chunk.MAX_SIZE * MAX_NUM_CHUNKS) {
            throw new FileSizeException(path);
        }

        this.id = FileIDGenerator.generateID(file);

        this.generateChunks();
    }

    public String getAbsolutePath() {
        return this.file.getAbsolutePath();
    }

    public void generateChunks() throws IOException {
        boolean requiresChunk0Len = (file.length() % Chunk.MAX_SIZE) == 0;

        FileInputStream input = new FileInputStream(file);

        int chunkNo = 0, chunkLen = 0;
        byte[] chunkBuf = new byte[Chunk.MAX_SIZE];

        while ((chunkLen = input.read(chunkBuf)) != -1) {
            chunks.add(new Chunk(this.id, chunkNo, Arrays.copyOfRange(chunkBuf, 0, chunkLen)));
            chunkNo++;
        }

        // if last chunk has 64k, add chunk of size 0
        if (requiresChunk0Len) {
            chunks.add(new Chunk(this.id, chunkNo, new byte[0]));
        }

        input.close();
    }


    public List<Chunk> getChunks() {
        return this.chunks;
    }

    public String getID() {
        return this.id;
    }
}
