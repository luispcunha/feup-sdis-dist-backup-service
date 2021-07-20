package handler.factory;

import java.net.DatagramPacket;

import handler.Handler;
import peer.Peer;

public abstract class HandlerFactory {
    protected Peer peer;

    public HandlerFactory(Peer peer) {
        this.peer = peer;
    }

    public abstract Handler getHandler(DatagramPacket packet);

	public Peer getPeer() {
		return this.peer;
	}
}