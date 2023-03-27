package com.gmail.woodyc40.pbft.type;

public class DESMessage {
    private final MessageType type;
    private final Object payload;

    public DESMessage(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
    
    public MessageType getMsgType() {
        return this.type;
    }

    public Object getPayload() {
        return this.payload;
    }
    
    @Override
    public String toString() {
      return "DESMessage{" +
          "type=" + type +
          ", payload=" + payload +
          '}';
    }
}
