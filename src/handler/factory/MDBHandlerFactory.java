package handler.factory;

import java.net.DatagramPacket;

import handler.MDBHandler;
import peer.Peer;

public class MDBHandlerFactory extends HandlerFactory {

    public MDBHandlerFactory(Peer peer) {
        super(peer);
    }

    @Override
    public MDBHandler getHandler(DatagramPacket packet) {
        return new MDBHandler(this.peer, packet);
    }
}