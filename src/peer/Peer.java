package peer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import channel.MulticastChannel;
import file.Chunk;
import file.FileHandler;
import file.FileIDGenerator;
import file.FileSizeException;
import file.ChunkKey;
import filesystem.FileSystem;
import filesystem.PeerState;
import handler.factory.MCHandlerFactory;
import handler.factory.MDBHandlerFactory;
import handler.factory.MDRHandlerFactory;
import message.Message;
import message.Messages;
import protocol.ChunkBackupInitiator;
import protocol.ChunkBackupSynchronizer;
import protocol.ChunkRestoreInitiator;
import protocol.ChunkRestoreSynchronizer;
import protocol.DeleteInitiator;
import protocol.SpaceReclaimInitiator;
import protocol.SpaceReclaimSynchronizer;
import util.Log;

public class Peer implements PeerInterface {
    private int id;
    private String version;

    private MulticastChannel mdb;
    private MulticastChannel mdr;
    private MulticastChannel mc;

    private FileSystem fileSystem;
    private PeerState state;

    private ExecutorService workers;
    private ScheduledExecutorService scheduler;

    // objects used to synchronize between threads working on the same protocol
    private ChunkBackupSynchronizer chunkBackupSync;
    private ChunkRestoreSynchronizer chunkRestoreSync;
    private SpaceReclaimSynchronizer spaceReclaimSync;

    // true if it's first time this peer is launched
    private boolean firstTime;

    private final int SCHEDULER_POOL_SIZE = 500;
    private final long SAVE_STATE_INTERVAL_MS = 2000;

    public Peer(MulticastChannel mc, MulticastChannel mdb, MulticastChannel mdr, String version, int id) {
        this.id = id;
        this.version = version;

        this.mc = mc;
        this.mdb = mdb;
        this.mdr = mdr;

        this.mc.setHandlerFactory(new MCHandlerFactory(this));
        this.mdb.setHandlerFactory(new MDBHandlerFactory(this));
        this.mdr.setHandlerFactory(new MDRHandlerFactory(this));

        this.chunkBackupSync = new ChunkBackupSynchronizer();
        this.chunkRestoreSync = new ChunkRestoreSynchronizer();
        this.spaceReclaimSync = new SpaceReclaimSynchronizer();

        this.workers = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(SCHEDULER_POOL_SIZE);

        this.fileSystem = new FileSystem(this);
        this.firstTime = ! this.loadState();

        // start listener threads
        new Thread(this.mc).start();
        new Thread(this.mdb).start();
        new Thread(this.mdr).start();

        // schedule task that saves peer state to disk
        this.writeStateToDisk();
    }

    public boolean loadState() {
        PeerState state = this.fileSystem.loadState();

        if (state == null) {
            this.state = new PeerState(this.version);
            return false;
        } else {
            this.state = state;
            state.savedToDisk();
            state.setVersion(this.version);
            return true;
        }
    }

    public void sendStartupMessage() {
        if (this.firstTime || !this.version.equals("2.0"))
            return;

        Message msg = Messages.getStartupMessage(this.id);
        try {
            Log.logSentMC(msg.getHeader());
            this.mc.broadcast(msg);
        } catch (IOException e) {
            Log.logError("Unable to send " + msg.getHeader());
        }

        Set<String> undeletedFiles = state.getUndeletedFiles();
        for (String file : undeletedFiles) {
            msg = Messages.getEnhancedDeleteMessage(id, file);
            try {
                mc.broadcast(msg);
                Log.logSentMC(msg.getHeader());
            } catch (IOException e) {
                Log.logError("Unable to send " + msg.getHeader());
            }
        }
    }

    public void writeStateToDisk() {
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                fileSystem.storeState(state);
            }
        }, 0, SAVE_STATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void submitWorker(Runnable task) {
        this.workers.submit(task);
    }

    public void scheduleTask(Runnable task, long delay) {
        this.scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    public int getID() {
        return this.id;
    }

    public String getVersion() {
        return this.version;
    }

    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

    public PeerState getState() {
        return this.state;
    }

    public MulticastChannel getMDBChannel() {
        return this.mdb;
    }

    public MulticastChannel getMDRChannel() {
        return this.mdr;
    }

    public MulticastChannel getMCChannel() {
        return this.mc;
    }

    public ChunkBackupSynchronizer getChunkBackupSync() {
        return this.chunkBackupSync;
    }

    public ChunkRestoreSynchronizer getChunkRestoreSync() {
        return this.chunkRestoreSync;
    }

    public SpaceReclaimSynchronizer getSpaceReclaimSync() {
        return this.spaceReclaimSync;
    }

    @Override
    public int backup(String path, int replicationDegree) throws RemoteException {

        try {
            FileHandler file = new FileHandler(path);

            if (! this.state.insertFileInfo(file.getAbsolutePath(), file.getID(), replicationDegree)) {
                Log.logError("File " + file.getID() + " already backed up");
                return -1;
            }


            List<Chunk> chunks = file.getChunks();


            for (Chunk chunk : chunks) {
                this.workers.submit(new ChunkBackupInitiator(this, chunk, replicationDegree, 5, 1000));
            }
        } catch (IOException e) {
            Log.logError("Failed opening file");
            return -1;
        } catch (FileSizeException e) {
            Log.logError(e.toString());
            return -1;
        } catch (NoSuchAlgorithmException e) {
            Log.logError("Failed to generate file ID");
            return -1;
        }

        return 0;
    }

    @Override
    public int restore(String path) throws RemoteException {

        File file = new File(path);
        String fileID;
        try {
            fileID = FileIDGenerator.generateID(file);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            Log.logError("Failed generating file ID");
            return -1;
        }

        if (!state.isBackupFile(fileID)) {
            Log.logError("This peer didn't request backup of file " + fileID);
            return -1;
        }

        long numChunks = file.length() / Chunk.MAX_SIZE + 1;

        this.chunkRestoreSync.restoreFile(fileID);

        for (int i = 0; i < numChunks; i++) {
            this.workers.submit(new ChunkRestoreInitiator(this, new ChunkKey(fileID, i)));
        }

        return 0;
    }

    @Override
    public int delete(String path) throws RemoteException {
        File file = new File(path);
        String fileID;
        try {
            fileID = FileIDGenerator.generateID(file);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            Log.logError("Failed generating file ID");
            return -1;
        }

        if (! state.isBackupFile(fileID)) {
            Log.logError("This peer didn't request backup of file " + fileID);
            return -1;
        }

        Message msg;
        if (this.version.equals("2.0"))
            msg = Messages.getEnhancedDeleteMessage(this.id, fileID);
        else
            msg = Messages.getDeleteMessage(this.id, fileID);


        this.state.deleteBackupFile(fileID);

        this.workers.submit(new DeleteInitiator(this, msg, 3, 1000));

        return 0;
    }

    @Override
    public int reclaim(int space) throws RemoteException {
        List<ChunkKey> chunkKeys = this.state.reclaim(space);

        for (ChunkKey chunkKey : chunkKeys) {
            this.workers.submit(new SpaceReclaimInitiator(this, chunkKey));
        }

        for (ChunkKey chunkKey : chunkKeys) {
            this.fileSystem.deleteChunk(chunkKey);
        }

        return 0;
    }

    @Override
    public String state() throws RemoteException {
        return this.state.toString();
    }
}