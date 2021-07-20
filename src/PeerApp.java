import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import channel.MulticastChannel;
import peer.Peer;
import peer.PeerInterface;
import util.Log;

public class PeerApp {
    public static void main(String[] args)  {
        if (args.length != 9) {
            System.out.println(
                    "Usage: java Peer <version> <peer_id> <rmi_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>");
            System.exit(-1);
        }

        String version = args[0];
        int peerID = Integer.parseInt(args[1]);
        String remoteObjName = args[2];
        String mcAddr = args[3];
        int mcPort = Integer.parseInt(args[4]);
        String mdbAddr = args[5];
        int mdbPort = Integer.parseInt(args[6]);
        String mdrAddr = args[7];
        int mdrPort = Integer.parseInt(args[8]);

        Log.setPeerID(peerID);

        if (! (version.equals("1.0") || version.equals("2.0"))) {
            Log.log("Invalid version. Available versions are 1.0 and 2.0");
            return;
        }

        MulticastChannel mc;
        try {
            mc = new MulticastChannel(mcAddr, mcPort);
        } catch (IOException e) {
            Log.logError("Unable to setup MC channel");
            System.exit(-1);
            return;
        }

        MulticastChannel mdb;
        try {
            mdb = new MulticastChannel(mdbAddr, mdbPort);
        } catch (IOException e) {
            Log.logError("Unable to setup MDB channel");
            System.exit(-1);
            return;
        }

        MulticastChannel mdr;
        try {
            mdr = new MulticastChannel(mdrAddr, mdrPort);
        } catch (IOException e) {
            Log.logError("Unable to setup MDR channel");
            System.exit(-1);
            return;
        }

        Peer peer = new Peer(mc, mdb, mdr, version, peerID);

        PeerInterface peerStub;
        Registry registry;

        try {
            peerStub = (PeerInterface) UnicastRemoteObject.exportObject(peer, 0);
            registry = LocateRegistry.getRegistry();
            registry.rebind(remoteObjName, peerStub);
        } catch (RemoteException e) {
            Log.logError("Unable to setup RMI");
            System.exit(-1);
        }
        Log.log("Version " + version);
        Log.log("Listening on " + mcAddr + ":" + mcPort + " (MC)");
        System.out.println("                       " + mdbAddr + ":" + mdbPort + " (MDB)");
        System.out.println("                       " + mdrAddr + ":" + mdrPort + " (MDR)");
        Log.log("RMI access point is " + remoteObjName);

        peer.sendStartupMessage();
    }
}