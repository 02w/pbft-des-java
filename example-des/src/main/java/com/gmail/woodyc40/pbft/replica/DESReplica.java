package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.*;
import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.type.DESOperation;
import com.gmail.woodyc40.pbft.type.DESResult;
import com.gmail.woodyc40.pbft.type.DESMessage;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class DESReplica extends DefaultReplica<DESOperation, DESResult, DESMessage> {
    private final boolean faulty;
    private final Queue<DESMessage> messagQueue = new ConcurrentLinkedQueue<>();

    public DESReplica(int replicaId,
            int tolerance,
            long timeout,
            ReplicaMessageLog log,
            ReplicaEncoder<DESOperation, DESResult, DESMessage> encoder,
            ReplicaDigester<DESOperation> digester,
            ReplicaTransport<DESMessage> transport,
            boolean faulty) {
        super(replicaId, tolerance, timeout, log, encoder, digester, transport);
        this.faulty = faulty;
    }

    @Override
    public DESResult compute(DESOperation operation) {
        return new DESResult(
                this.faulty ? ThreadLocalRandom.current().nextInt() : operation.first() + operation.second());
    }

    public void handleIncomingMessage(DESMessage data) {
        // System.out.println(String.format("RECV: REPLICA %d: %s", this.replicaId(),
        // data));
        var type = data.getMsgType();
        var payload = data.getPayload();
        switch (type) {
            case REQUEST:
                var clientReq = (DefaultClientRequest<DESOperation>) payload;
                var request = new DefaultReplicaRequest<>(clientReq.operation(), clientReq.timestamp(),
                        clientReq.client().clientId());
                this.recvRequest(request);
                break;
            case PRE_PREPARE:
                this.recvPrePrepare((ReplicaPrePrepare<DESOperation>) payload);
                break;
            case PREPARE:
                this.recvPrepare((ReplicaPrepare) payload);
                break;
            case COMMIT:
                this.recvCommit((ReplicaCommit) payload);
                break;
            case CHECKPOINT:
                this.recvCheckpoint((ReplicaCheckpoint) payload);
                break;
            case VIEW_CHANGE:
                this.recvViewChange((ReplicaViewChange) payload);
                break;
            case NEW_VIEW:
                this.recvNewView((ReplicaNewView) payload);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized type: " + type);
        }
    }

    public void recvMessage(DESMessage m) {
        this.messagQueue.add(m);
    }

    public void step() {
        var m = this.messagQueue.poll();
        if (m != null) {
            this.handleIncomingMessage(m);
        }
    }
}
