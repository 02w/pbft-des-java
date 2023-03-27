package com.gmail.woodyc40.pbft.client;

import com.gmail.woodyc40.pbft.ClientTransport;
import com.gmail.woodyc40.pbft.replica.DESReplica;
import com.gmail.woodyc40.pbft.type.DESMessage;

import java.util.Map;
import java.util.stream.IntStream;

public class DESClientTransport implements ClientTransport<DESMessage> {
    private final Map<Integer, DESReplica> replicas;
    // private final int replicasCount;

    public DESClientTransport(Map<Integer, DESReplica> replicas) {
        this.replicas = replicas;
        // this.replicasCount = replicas.size();
    }

    @Override
    public IntStream knownReplicaIds() {
        return IntStream.range(0, this.replicas.size());
    }

    @Override
    public int countKnownReplicas() {
        return this.replicas.size();
    }

    @Override
    public void sendRequest(int replicaId, DESMessage request) {
        System.out.println(String.format("SEND: CLIENT -> %d: %s", replicaId, request));
        this.replicas.get(replicaId).recvMessage(request);
    }

    @Override
    public void multicastRequest(DESMessage request) {
        for (int i = 0; i < this.replicas.size(); i++) {
            this.sendRequest(i, request);
        }
    }
}
