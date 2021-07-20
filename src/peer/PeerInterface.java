package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote {
    int backup(String path, int replicationDegree) throws RemoteException;

    int restore(String path) throws RemoteException;

    int delete(String path) throws RemoteException;

    int reclaim(int space) throws RemoteException;

    String state() throws RemoteException;
}
