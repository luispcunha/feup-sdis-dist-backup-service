package channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import handler.Handler;
import handler.factory.HandlerFactory;
import message.Message;
import peer.Peer;
import util.Log;

public class MulticastChannel implements Runnable {
    private MulticastSocket socket;
    private InetAddress address;
    private int port;
    private boolean close;
    private HandlerFactory handlerFactory;

    private final int MAX_BUF_LEN = 65507;
    private Peer peer;

    public MulticastChannel(String addrName, int port) throws IOException {
        this.address = InetAddress.getByName(addrName);
        this.port = port;

        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.address);
        this.socket.setTimeToLive(1);

        this.handlerFactory = null;
        this.peer = null;

        this.close = false;
    }

    public void setHandlerFactory(HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
        this.peer = handlerFactory.getPeer();
    }

    public void broadcast(Message msg) throws IOException {
        byte[] buf = msg.toBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, this.address, this.port);
        socket.send(packet);
    }

    @Override
    public void run() {
        while (! this.close) {
            byte[] buf = new byte[this.MAX_BUF_LEN];
            DatagramPacket packet = new DatagramPacket(buf, MAX_BUF_LEN);

            try {
                this.socket.receive(packet);
            } catch (IOException e) {
                Log.logError("Failed receiving packet from socket");
                continue;
            }

            if (this.handlerFactory == null)
                continue;

            Handler handler = this.handlerFactory.getHandler(packet);
            this.peer.submitWorker(handler);
        }
    }

    public void close() {
        this.close = true;
        this.socket.close();
    }
}