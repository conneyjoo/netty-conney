package com.conney.keeptriple.local.net.session;

public class SessionEvent {

    public SessionEventType type;

    public Object data;

    public Session session;

    public SessionEvent(Session session) {
        this.session = session;
    }

    @Override
    public String toString() {
        return "SessionEvent{" +
                "type=" + type +
                ", data=" + data +
                ", session=" + session +
                '}';
    }
}