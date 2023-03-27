package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.client.DESClient;
import com.gmail.woodyc40.pbft.client.DESClientEncoder;
import com.gmail.woodyc40.pbft.client.DESClientTransport;
import com.gmail.woodyc40.pbft.replica.DESReplica;
import com.gmail.woodyc40.pbft.replica.DESReplicaEncoder;
import com.gmail.woodyc40.pbft.replica.DESReplicaTransport;
import com.gmail.woodyc40.pbft.replica.NoopDigester;
import com.gmail.woodyc40.pbft.type.DESOperation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DESMain {
    private static final int TOLERANCE = 1;
    private static final long TIMEOUT_MS = 5;
    private static final int REPLICA_COUNT = 3 * TOLERANCE + 1;
    private static final int MAX_STEP = 100;

    public static void main(String[] args) throws InterruptedException {
        final Map<Integer, DESReplica> replicas = new ConcurrentHashMap<>();
        setupReplicas(replicas);

        var client = setupClient("1", replicas);
        for (var r : replicas.values()) {
            ((DESReplicaTransport) r.transport()).addClient(client);
        }

        var ticket = client.sendRequest(new DESOperation(1, 1));
        client.checkTimeout(ticket);
        for (int i = 0; i < MAX_STEP; i++) {
            for (var r : replicas.values()) {
                r.step();
            }
            client.step();
            SimClock.getInstance().advance(1);
        }
        ticket.result().thenAccept(result -> {
            synchronized (System.out) {
                System.out.println("==========================");
                System.out.println("==========================");
                System.out.println("1 + 1  = " + result.result());
                System.out.println("==========================");
                System.out.println("==========================");
            }
        }).exceptionally(t -> {
            throw new RuntimeException(t);
        });
    }

    private static void setupReplicas(Map<Integer, DESReplica> replicas) {
        var replicaEncoder = new DESReplicaEncoder();
        var digester = new NoopDigester();
        var replicaTransport = new DESReplicaTransport(replicas);

        for (int i = 0; i < REPLICA_COUNT; i++) {
            var log = new DefaultReplicaMessageLog(100, 100, 200);
            var replica = new DESReplica(
                    i,
                    TOLERANCE,
                    TIMEOUT_MS,
                    log,
                    replicaEncoder,
                    digester,
                    replicaTransport,
                    i == 0);
            replicas.put(i, replica);
        }

    }

    private static DESClient setupClient(String clientId, Map<Integer, DESReplica> replicas) {
        var clientEncoder = new DESClientEncoder();
        var clientTransport = new DESClientTransport(replicas);

        var client = new DESClient(
                clientId,
                TOLERANCE,
                TIMEOUT_MS,
                clientEncoder,
                clientTransport);

        return client;
    }
}
