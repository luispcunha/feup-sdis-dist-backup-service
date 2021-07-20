package handler;

import java.net.DatagramPacket;

import peer.Peer;

public abstract class Handler implements Runnable {
    protected Peer peer;
    protected DatagramPacket packet;

    public Handler(Peer peer, DatagramPacket packet) {
        this.peer = peer;
        this.packet = packet;
    }
}