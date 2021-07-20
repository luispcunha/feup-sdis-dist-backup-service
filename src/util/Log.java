package util;

public class Log {
    static int peerID;

    public static void setPeerID(int id) {
        peerID = id;
    }

    public static void log(String msg) {
        System.out.println("Peer " + peerID + " :: " + msg);
    }

    public static void logReceivedMC(String msg) {
        log("MC :: Received :: " + msg);
    }

    public static void logReceivedMDB(String msg) {
        log("MDB :: Received :: " + msg);
    }

    public static void logReceivedMDR(String msg) {
        log("MDR :: Received :: " + msg);
    }

    public static void logSentMC(String msg) {
        log("MC :: Sent :: " + msg);
    }

    public static void logSentMDB(String msg) {
        log("MDB :: Sent :: " + msg);
    }

    public static void logSentMDR(String msg) {
        log("MDR :: Sent :: " + msg);
    }

    public static void logError(String msg) {
        log("ERROR :: " + msg);
    }

    public static void logBackoff(int time, String before) {
        log("Backing off for " + String.format("%03d", time) + "ms before " + before);
    }

    public static void logRepDegree(int desired, int perceived, int chunkNo) {
        log("Desired RD " + desired + " :: Perceived RD " + perceived + " :: Chunk " + chunkNo);
    }

    public static void logWaiting(long time, int chunkNo) {
        log("Waiting for " + time + "ms (chunk " + chunkNo + ")");
    }
}