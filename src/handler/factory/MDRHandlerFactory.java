package handler.factory;

import java.net.DatagramPacket;

import handler.MDRHandler;
import peer.Peer;

public class MDRHandlerFactory extends HandlerFactory {

    public MDRHandlerFactory(Peer peer) {
        super(peer);
    }

    @Override
    public MDRHandler getHandler(DatagramPacket packet) {
        return new MDRHandler(this.peer, packet);
    }
}