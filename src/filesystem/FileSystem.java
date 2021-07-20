package filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;

import file.Chunk;
import file.ChunkKey;
import peer.Peer;
import util.Log;

public class FileSystem {
    private String fileSystemPrefix;
    private final String CHUNKS_PATH_PREFIX = "chunks/";
    private final String RECOVERED_PATH_PREFIX = "recovered/";
    private final String PERSISTENT_STATE_PATH = ".state";

    public FileSystem(Peer peer) {
        this.fileSystemPrefix = "peer_" + peer.getID() + "/";

        File chunksDir = new File(this.fileSystemPrefix + CHUNKS_PATH_PREFIX);
        chunksDir.mkdirs();

        File recoveredDir = new File(this.fileSystemPrefix + RECOVERED_PATH_PREFIX);
        recoveredDir.mkdirs();
    }

    public int storeChunk(Chunk chunk) throws IOException {
        String dirPath = fileSystemPrefix + CHUNKS_PATH_PREFIX + chunk.getFileID() + "/";

        File dir = new File(dirPath);
        dir.mkdirs();

        String path = dirPath + chunk.getNumber();

        FileOutputStream fos = new FileOutputStream(path);

        fos.write(chunk.getContent());
        fos.close();

        return 0;
    }

    public Chunk loadChunk(String fileID, int chunkNumber) throws FileNotFoundException {
        String path = fileSystemPrefix + CHUNKS_PATH_PREFIX + fileID + "/" + chunkNumber;

        File file = new File(path);

        if (! file.isFile())
            return null;

        FileInputStream fis = new FileInputStream(file);
        byte[] buf = new byte[(int) file.length()];

        try {
            fis.read(buf);
            fis.close();
        } catch (IOException e) {
            Log.logError("Failed loading chunk from file system");
            return null;
        }

        return new Chunk(fileID, chunkNumber, buf);
    }

    public int deleteChunk(ChunkKey chunkKey) {
        String path = fileSystemPrefix + CHUNKS_PATH_PREFIX + chunkKey.getFileID() + "/" + chunkKey.getNumber();

        File file = new File(path);
        File dir = file.getParentFile();
        file.delete();

        if (dir.isDirectory() && dir.list().length == 0)
            dir.delete();

        return 0;
    }

    public int deleteFileChunks(String fileID) {
        String path = fileSystemPrefix + CHUNKS_PATH_PREFIX + fileID + "/";
        File dir = new File(path);

        File[] chunks = dir.listFiles();

        if (chunks != null) {
            for (File chunk : chunks) {
                chunk.delete();
            }
        }

        dir.delete();

        return 0;
    }

    public int restoreFile(List<Chunk> chunks) {
        if (chunks.size() <= 0)
            return -1;

        Collections.sort(chunks);

        String path = fileSystemPrefix + RECOVERED_PATH_PREFIX + "/" + chunks.get(0).getFileID();

        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream fos = new FileOutputStream(path, true);

            for (Chunk chunk : chunks) {
                fos.write(chunk.getContent());
            }

            fos.close();
        } catch (IOException e) {
            Log.logError("Failed restoring file from chunks");
        }

        Log.log("Restored file " + chunks.get(0).getFileID());

        return 0;
    }

    public void storeState(PeerState state) {
        if (! state.modified())
            return;

        try {
            FileOutputStream fileOut = new FileOutputStream(fileSystemPrefix + PERSISTENT_STATE_PATH);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(state);
            objectOut.close();
            fileOut.close();
            state.savedToDisk();
        } catch (IOException e) {
            Log.logError("Failed saving state");
        }
    }

    public PeerState loadState() {
        File file = new File(fileSystemPrefix + PERSISTENT_STATE_PATH);

        if (! file.exists())
            return null;

        try {
            FileInputStream fileIn = new FileInputStream(fileSystemPrefix + PERSISTENT_STATE_PATH);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            PeerState state = (PeerState) objectIn.readObject();
            objectIn.close();
            fileIn.close();
            return state;
        } catch (IOException | ClassNotFoundException e) {
            Log.logError("Failed loading state");
            return null;
        }
    }
}