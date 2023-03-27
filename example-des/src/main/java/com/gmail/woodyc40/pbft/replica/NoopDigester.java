package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.ReplicaDigester;
import com.gmail.woodyc40.pbft.message.ReplicaRequest;
import com.gmail.woodyc40.pbft.type.DESOperation;

public class NoopDigester implements ReplicaDigester<DESOperation> {
    private static final byte[] EMPTY_DIGEST = new byte[0];

    @Override
    public byte[] digest(ReplicaRequest<DESOperation> request) {
        return EMPTY_DIGEST;
    }
}
