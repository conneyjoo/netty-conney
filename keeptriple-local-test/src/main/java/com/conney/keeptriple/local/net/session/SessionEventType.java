package com.conney.keeptriple.local.net.session;

public enum SessionEventType {

    CREATED(0),
    ACTIVE(1),
    CHANGE(2),
    DESTROY(3);

    private int type;

    SessionEventType(int type) {
        this.type = type;
    }

    public boolean isCreated() {
        return this.type == CREATED.type;
    }

    public boolean isDestroy() {
        return this.type == DESTROY.type;
    }

    public boolean isActive() {
        return this.type == ACTIVE.type;
    }

    public boolean isChange() {
        return this.type == CHANGE.type;
    }

    @Override
    public String toString() {
        return "SessionEventType{" + "type=" + type + '}';
    }
}
