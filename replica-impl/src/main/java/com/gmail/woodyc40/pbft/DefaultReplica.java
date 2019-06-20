package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

// TODO: Use timeouts
public abstract class DefaultReplica<O, R, T> implements Replica<O, R, T> {
    private static final byte[] EMPTY_DIGEST = new byte[0];

    private final int replicaId;
    private final int tolerance;
    private final long timeout;
    private final ReplicaMessageLog log;
    private final ReplicaEncoder<O, R, T> encoder;
    private final ReplicaDigester<O> digester;
    private final ReplicaTransport<T> transport;

    private volatile int viewNumber;
    private final AtomicLong seqCounter = new AtomicLong();

    public DefaultReplica(int replicaId,
                          int tolerance,
                          long timeout,
                          ReplicaMessageLog log,
                          ReplicaEncoder<O, R, T> encoder,
                          ReplicaDigester<O> digester,
                          ReplicaTransport<T> transport) {
        this.replicaId = replicaId;
        this.tolerance = tolerance;
        this.timeout = timeout;
        this.log = log;
        this.encoder = encoder;
        this.digester = digester;
        this.transport = transport;
    }

    @Override
    public int replicaId() {
        return this.replicaId;
    }

    @Override
    public int tolerance() {
        return this.tolerance;
    }

    @Override
    public long timeoutMs() {
        return this.timeout;
    }

    @Override
    public ReplicaMessageLog log() {
        return this.log;
    }

    @Override
    public void setViewNumber(int newViewNumber) {
        this.viewNumber = newViewNumber;
    }

    @Override
    public int viewNumber() {
        return this.viewNumber;
    }

    private void resendReply(String clientId, ReplicaTicket<O, R> ticket) {
        ticket.result().thenAccept(result -> {
            ReplicaReply<R> reply = new DefaultReplicaReply<>(
                    ticket.viewNumber(),
                    ticket.request().timestamp(),
                    clientId,
                    this.replicaId,
                    result);
            this.sendReply(clientId, reply);
        });
    }

    private void recvRequest(ReplicaRequest<O> request, boolean wasRequestBuffered) {
        long timestamp = request.timestamp();
        ReplicaTicket<O, R> cachedTicket = this.log.getTicketFromCache(timestamp);
        if (cachedTicket != null) {
            String clientId = request.clientId();
            this.resendReply(clientId, cachedTicket);

            return;
        }

        int primaryId = this.getPrimaryId();

        // We are not the primary replica, redirect
        if (this.replicaId != primaryId) {
            this.sendRequest(primaryId, request);
            return;
        }

        if (!wasRequestBuffered) {
            if (this.log.shouldBuffer()) {
                this.log.buffer(request);
                return;
            }
        }

        int currentViewNumber = this.viewNumber;
        long seqNumber = this.seqCounter.getAndIncrement();

        ReplicaTicket<O, R> ticket = this.log.newTicket(currentViewNumber, seqNumber);
        ticket.append(request);

        ReplicaPrePrepare<O> prePrepare = new DefaultReplicaPrePrepare<>(
                currentViewNumber,
                seqNumber,
                this.digester.digest(request),
                request);
        this.sendPrePrepare(prePrepare);

        ticket.append(prePrepare);
    }

    @Override
    public void recvRequest(ReplicaRequest<O> request) {
        this.recvRequest(request, false);
    }

    @Override
    public void sendRequest(int replicaId, ReplicaRequest<O> request) {
        T encodedPrePrepare = this.encoder.encodeRequest(request);
        this.transport.sendMessage(replicaId, encodedPrePrepare);
    }

    @Override
    public void sendPrePrepare(ReplicaPrePrepare<O> prePrepare) {
        T encodedPrePrepare = this.encoder.encodePrePrepare(prePrepare);
        this.transport.multicast(encodedPrePrepare, this.replicaId);
    }

    private boolean verifyPhaseMessage(ReplicaPhaseMessage message) {
        int currentViewNumber = this.viewNumber;
        int viewNumber = message.viewNumber();
        if (currentViewNumber != viewNumber) {
            return false;
        }

        long seqNumber = message.seqNumber();
        return this.log.isBetweenWaterMarks(seqNumber);
    }

    @Override
    public void recvPrePrepare(ReplicaPrePrepare<O> prePrepare) {
        if (!this.verifyPhaseMessage(prePrepare)) {
            return;
        }

        int currentViewNumber = this.viewNumber;
        byte[] digest = prePrepare.digest();
        ReplicaRequest<O> request = prePrepare.request();
        long seqNumber = prePrepare.seqNumber();

        ReplicaTicket<O, R> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket != null) {
            for (Object message : ticket.messages()) {
                if (!(message instanceof ReplicaPrePrepare)) {
                    continue;
                }

                ReplicaPrePrepare<O> prevPrePrepare = (ReplicaPrePrepare<O>) message;
                byte[] prevDigest = prevPrePrepare.digest();
                if (!Arrays.equals(prevDigest, digest)) {
                    return;
                }
            }
        } else {
            ticket = this.log.newTicket(currentViewNumber, seqNumber);
        }

        byte[] computedDigest = this.digester.digest(request);
        if (!Arrays.equals(digest, computedDigest)) {
            return;
        }

        ticket.append(prePrepare);

        ReplicaPrepare prepare = new DefaultReplicaPrepare(
                currentViewNumber,
                seqNumber,
                digest,
                this.replicaId);
        this.sendPrepare(prepare);

        ticket.append(prepare);

        this.tryAdvanceState(ticket, prePrepare);
    }

    @Override
    public void sendPrepare(ReplicaPrepare prepare) {
        T encodedPrepare = this.encoder.encodePrepare(prepare);
        this.transport.multicast(encodedPrepare, this.replicaId);
    }

    @Override
    public void recvPrepare(ReplicaPrepare prepare) {
        ReplicaTicket<O, R> ticket = this.recvPhaseMessage(prepare);

        if (ticket != null) {
            this.tryAdvanceState(ticket, prepare);
        }
    }

    @Override
    public void sendCommit(ReplicaCommit commit) {
        T encodedCommit = this.encoder.encodeCommit(commit);
        this.transport.multicast(encodedCommit, this.replicaId);
    }

    @Override
    public void recvCommit(ReplicaCommit commit) {
        ReplicaTicket<O, R> ticket = this.recvPhaseMessage(commit);

        if (ticket != null) {
            this.tryAdvanceState(ticket, commit);
        }
    }

    private @Nullable ReplicaTicket<O, R> recvPhaseMessage(ReplicaPhaseMessage message) {
        if (!this.verifyPhaseMessage(message)) {
            return null;
        }

        int currentViewNumber = message.viewNumber();
        long seqNumber = message.seqNumber();

        ReplicaTicket<O, R> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket == null) {
            ticket = this.log.newTicket(currentViewNumber, seqNumber);
        }

        ticket.append(message);
        return ticket;
    }

    private void tryAdvanceState(ReplicaTicket<O, R> ticket, ReplicaPhaseMessage message) {
        int currentViewNumber = message.viewNumber();
        long seqNumber = message.seqNumber();
        byte[] digest = message.digest();

        ReplicaTicketPhase phase = ticket.phase();
        if (phase == ReplicaTicketPhase.PRE_PREPARE) {
            if (ticket.isPrepared(this.tolerance) && ticket.casPhase(phase, ReplicaTicketPhase.PREPARE)) {
                ReplicaCommit commit = new DefaultReplicaCommit(
                        currentViewNumber,
                        seqNumber,
                        digest,
                        this.replicaId);
                this.sendCommit(commit);

                ticket.append(commit);
            }
        }

        phase = ticket.phase();
        if (phase == ReplicaTicketPhase.PREPARE) {
            if (ticket.isCommittedLocal(this.tolerance) && ticket.casPhase(phase, ReplicaTicketPhase.COMMIT)) {
                ReplicaRequest<O> request = ticket.request();
                R result = this.compute(request.operation());

                String clientId = request.clientId();
                ReplicaReply<R> reply = new DefaultReplicaReply<>(
                        currentViewNumber,
                        request.timestamp(),
                        clientId,
                        this.replicaId,
                        result);

                this.log.completeTicket(currentViewNumber, seqNumber);
                this.sendReply(clientId, reply);

                if (seqNumber % this.log.checkpointInterval() == 0) {
                    ReplicaCheckpoint checkpoint = new DefaultReplicaCheckpoint(
                            seqNumber,
                            this.digestState(),
                            this.replicaId);
                    this.sendCheckpoint(checkpoint);
                    this.log.appendCheckpoint(checkpoint, this.tolerance);
                }
            }
        }
    }

    private void handleNextBufferedRequest() {
        ReplicaRequest<O> bufferedRequest = this.log.popBuffer();
        if (bufferedRequest != null) {
            this.recvRequest(bufferedRequest, true);
        }
    }

    @Override
    public void sendReply(String clientId, ReplicaReply<R> reply) {
        T encodedReply = this.encoder.encodeReply(reply);
        this.transport.sendReply(clientId, encodedReply);

        this.handleNextBufferedRequest();
    }

    @Override
    public void recvCheckpoint(ReplicaCheckpoint checkpoint) {
        this.log.appendCheckpoint(checkpoint, this.tolerance);
    }

    @Override
    public void sendCheckpoint(ReplicaCheckpoint checkpoint) {
        T encodedCheckpoint = this.encoder.encodeCheckpoint(checkpoint);
        this.transport.multicast(encodedCheckpoint, this.replicaId);
    }

    @Override
    public void recvViewChange(ReplicaViewChange viewChange) {
        int newViewNumber = viewChange.newViewNumber();
        int newPrimaryId = getPrimaryId(newViewNumber, this.transport.countKnownReplicas());

        // We're not the new primary
        if (newPrimaryId != this.replicaId) {
            return;
        }

        this.log.appendViewChange(viewChange);

        if (this.log.produceNewView(newViewNumber, this.tolerance)) {


            new DefaultReplicaNewView(
                    newViewNumber,
                    )

            this.viewNumber = newViewNumber;
        }
    }

    @Override
    public void sendViewChange(ReplicaViewChange viewChange) {
        T encodedViewChange = this.encoder.encodeViewChange(viewChange);
        this.transport.multicast(encodedViewChange, this.replicaId);
    }

    @Override
    public void recvNewView(ReplicaNewView newView) {
    }

    @Override
    public void sendNewView(ReplicaNewView newView) {
        T encodedNewView = this.encoder.encodeNewView(newView);
        this.transport.multicast(encodedNewView, this.replicaId);
    }

    @Override
    public byte[] digestState() {
        return EMPTY_DIGEST;
    }

    @Override
    public ReplicaEncoder<O, R, T> encoder() {
        return this.encoder;
    }

    @Override
    public ReplicaDigester<O> digester() {
        return this.digester;
    }

    @Override
    public ReplicaTransport<T> transport() {
        return this.transport;
    }

    private static int getPrimaryId(int viewNumber, int knownReplicas) {
        return viewNumber % knownReplicas;
    }

    private int getPrimaryId() {
        return getPrimaryId(this.viewNumber, this.transport.countKnownReplicas());
    }
}
