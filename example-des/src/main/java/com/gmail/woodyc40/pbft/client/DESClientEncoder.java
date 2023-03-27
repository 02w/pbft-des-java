package com.gmail.woodyc40.pbft.client;

import com.gmail.woodyc40.pbft.ClientEncoder;
import com.gmail.woodyc40.pbft.message.ClientRequest;
import com.gmail.woodyc40.pbft.type.DESMessage;
import com.gmail.woodyc40.pbft.type.DESOperation;
import com.gmail.woodyc40.pbft.type.MessageType;

public class DESClientEncoder implements ClientEncoder<DESOperation, DESMessage> {
    @Override
    public DESMessage encodeRequest(ClientRequest<DESOperation> request) {
        return new DESMessage(MessageType.REQUEST, request);
    }
}
