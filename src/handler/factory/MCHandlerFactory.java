package handler.factory;

import java.net.DatagramPacket;

import handler.MCHandler;
import peer.Peer;

public class MCHandlerFactory extends HandlerFactory {

    public MCHandlerFactory(Peer peer) {
        super(peer);
    }

    @Override
    public MCHandler getHandler(DatagramPacket packet) {
        return new MCHandler(this.peer, packet);
    }
}