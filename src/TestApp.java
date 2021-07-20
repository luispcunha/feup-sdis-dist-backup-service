import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

import peer.PeerInterface;

public class TestApp {
    public static void logUsage() {
        System.out.println("Usage: java TestApp <rmi_ap> <operation> <opnd_1> <opnd_2>");
        System.out.println("                              BACKUP <path> <rep_degree>");
        System.out.println("                              RESTORE <path>");
        System.out.println("                              DELETE <path>");
        System.out.println("                              RECLAIM <max_space_kb>");
        System.out.println("                              STATE");
    }
    public static void main(String[] args) {
        if (args.length < 2) {
            logUsage();
        }

        String accessPoint = args[0];
        String protocol = args[1];
        String[] operands = Arrays.copyOfRange(args, 2, args.length);

        try {
            Registry registry = LocateRegistry.getRegistry();
            PeerInterface peerStub = (PeerInterface) registry.lookup(accessPoint);

            String path;
            int maxSpace, repDegree, result;

            switch (protocol) {
                case "BACKUP":
                    if (operands.length != 2) {
                        System.out.println("Usage: java TestApp <peer_ap> BACKUP <path> <rep_degree>");
                        System.exit(-1);
                    }
                    path = operands[0];
                    repDegree = Integer.parseInt(operands[1]);
                    result = peerStub.backup(path, repDegree);
                    System.out.println("TestApp :: BACKUP " + path + " " + repDegree + " :: " + result);
                    break;
                case "RESTORE":
                    if (operands.length != 1) {
                        System.out.println("Usage: java TestApp <peer_ap> RESTORE <path>");
                        System.exit(-1);
                    }
                    path = operands[0];
                    result = peerStub.restore(path);
                    System.out.println("TestApp :: RESTORE " + path + " :: " + result);
                    break;
                case "DELETE":
                    if (operands.length != 1) {
                        System.out.println("Usage: java TestApp <peer_ap> DELETE <path>");
                        System.exit(-1);
                    }
                    path = operands[0];
                    result = peerStub.delete(path);
                    System.out.println("TestApp :: DELETE " + path + " :: " + result);
                    break;
                case "RECLAIM":
                    if (operands.length != 1) {
                        System.out.println("Usage: java TestApp <peer_ap> RECLAIM <max_space_kb>");
                        System.exit(-1);
                    }
                    maxSpace = Integer.parseInt(operands[0]);
                    result = peerStub.reclaim(maxSpace);
                    System.out.println("TestApp :: RECLAIM " + maxSpace + " :: " + result);
                    break;
                case "STATE":
                    if (operands.length != 0) {
                        System.out.println("Usage: java TestApp <peer_ap> STATE");
                        System.exit(-1);
                    }
                    String state = peerStub.state();
                    System.out.println("TestApp :: STATE\n\n" + state);
                    break;
                default:
                    logUsage();
            }
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}