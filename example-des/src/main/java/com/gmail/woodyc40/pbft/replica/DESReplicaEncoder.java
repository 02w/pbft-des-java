package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.ReplicaEncoder;
import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.type.DESOperation;
import com.gmail.woodyc40.pbft.type.DESResult;
import com.gmail.woodyc40.pbft.type.MessageType;
import com.gmail.woodyc40.pbft.type.DESMessage;

public class DESReplicaEncoder implements ReplicaEncoder<DESOperation, DESResult, DESMessage> {

    @Override
    public DESMessage encodeRequest(ReplicaRequest<DESOperation> request) {
        return new DESMessage(MessageType.REQUEST, request);
    }

    @Override
    public DESMessage encodePrePrepare(ReplicaPrePrepare<DESOperation> prePrepare) {
        return new DESMessage(MessageType.PRE_PREPARE, prePrepare);
    }

    @Override
    public DESMessage encodePrepare(ReplicaPrepare prepare) {
        return new DESMessage(MessageType.PREPARE, prepare);
    }

    @Override
    public DESMessage encodeCommit(ReplicaCommit commit) {
        return new DESMessage(MessageType.COMMIT, commit);
    }

    @Override
    public DESMessage encodeReply(ReplicaReply<DESResult> reply) {
        return new DESMessage(MessageType.REPLY, reply);
    }

    @Override
    public DESMessage encodeCheckpoint(ReplicaCheckpoint checkpoint) {
        return new DESMessage(MessageType.CHECKPOINT, checkpoint);
    }

    @Override
    public DESMessage encodeViewChange(ReplicaViewChange viewChange) {
        return new DESMessage(MessageType.VIEW_CHANGE, viewChange);
    }

    @Override
    public DESMessage encodeNewView(ReplicaNewView newView) {
        return new DESMessage(MessageType.NEW_VIEW, newView);
    }
}
