package com.gmail.woodyc40.pbft.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.gmail.woodyc40.pbft.ClientEncoder;
import com.gmail.woodyc40.pbft.ClientTransport;
import com.gmail.woodyc40.pbft.DefaultClient;
import com.gmail.woodyc40.pbft.message.DefaultClientReply;
import com.gmail.woodyc40.pbft.message.ReplicaReply;
import com.gmail.woodyc40.pbft.type.DESMessage;
import com.gmail.woodyc40.pbft.type.DESOperation;
import com.gmail.woodyc40.pbft.type.DESResult;

public class DESClient extends DefaultClient<DESOperation, DESResult, DESMessage> {
    public DESClient(String clientId,
            int tolerance,
            long timeoutClock,
            ClientEncoder<DESOperation, DESMessage> encoder,
            ClientTransport<DESMessage> transport) {
        super(clientId, tolerance, timeoutClock, encoder, transport);
    }

    private final Queue<DESMessage> messagQueue = new ConcurrentLinkedQueue<>();

    public void handleIncomingMessage(DESMessage data) {
        // System.out.println(String.format("RECV: CLIENT %s: %s", this.clientId(),
        // data));

        var type = data.getMsgType();
        switch (type) {
            case REPLY:
                var payload = (ReplicaReply<DESResult>) data.getPayload();
                int viewNumber = payload.viewNumber();
                long timestamp = payload.timestamp();
                int replicaId = payload.replicaId();
                var result = payload.result();

                var reply = new DefaultClientReply<DESResult>(
                        viewNumber,
                        timestamp,
                        this,
                        replicaId,
                        result);
                this.recvReply(reply);
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
