package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.ReplicaTransport;
import com.gmail.woodyc40.pbft.client.DESClient;
import com.gmail.woodyc40.pbft.type.DESMessage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class DESReplicaTransport implements ReplicaTransport<DESMessage> {
    private final Map<Integer, DESReplica> replicas;
    private final Map<String, DESClient> clients;

    public DESReplicaTransport(Map<Integer, DESReplica> replicas) {
        this.replicas = replicas;
        this.clients = new ConcurrentHashMap<>();
    }

    @Override
    public int countKnownReplicas() {
        return this.replicas.size();
    }

    @Override
    public IntStream knownReplicaIds() {
        return IntStream.range(0, this.replicas.size());
    }

    @Override
    public void sendMessage(int replicaId, DESMessage data) {
        System.out.println(String.format("SEND: REPLICA -> %d: %s", replicaId, data));
        this.replicas.get(replicaId).recvMessage(data);
    }

    @Override
    public void multicast(DESMessage data, int... ignoredReplicas) {
        Set<Integer> ignored = new HashSet<>(ignoredReplicas.length);
        for (int id : ignoredReplicas) {
            ignored.add(id);
        }

        for (int i = 0; i < this.replicas.size(); i++) {
            if (!ignored.contains(i)) {
                this.sendMessage(i, data);
            }
        }
    }

    public void addClient(DESClient client) {
        this.clients.put(client.clientId(), client);
    }

    @Override
    public void sendReply(String clientId, DESMessage reply) {
        System.out.println(String.format("SEND: REPLY -> %s: %s", clientId, reply));
        this.clients.get(clientId).recvMessage(reply);
    }
}
