package com.gmail.woodyc40.pbft.type;

public enum MessageType {
    REQUEST,
    PRE_PREPARE,
    PREPARE,
    COMMIT,
    CHECKPOINT,
    VIEW_CHANGE,
    NEW_VIEW,
    REPLY
}
