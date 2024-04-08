package com.conney.keeptriple.local.net.session;

import io.netty.util.DefaultAttributeMap;

import java.util.LinkedList;
import java.util.List;

public class Session extends DefaultAttributeMap {

    protected String id;

    private volatile long creationTime;

    private volatile long accessTime;

    private volatile long maxInactiveInterval;

    protected volatile boolean invalid;

    private List<SessionListener> listeners;

    protected SessionManager manager;

    public Session(String id, SessionManager manager) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
        this.accessTime = creationTime;
        this.maxInactiveInterval = -1;
        this.invalid = false;
        this.listeners = new LinkedList<>();
        this.manager = manager;
    }

    protected long getIdleTimeInternal() {
        long timeNow = System.currentTimeMillis();
        return timeNow - accessTime;
    }

    protected void tellNew() {
        SessionEvent event = new SessionEvent(this);
        event.type = SessionEventType.CREATED;
        fireSessionListener(event);
    }

    public Session active() {
        return active(null);
    }

    public Session active(Object data) {
        accessTime = System.currentTimeMillis();

        SessionEvent event = new SessionEvent(this);
        event.type = SessionEventType.ACTIVE;
        event.data = data;
        fireSessionListener(event);
        return this;
    }

    public void change() {
        change(null);
    }

    public void change(Object data) {
        SessionEvent event = new SessionEvent(this);
        event.type = SessionEventType.CHANGE;
        event.data = data;
        fireSessionListener(event);
    }

    /**
     * 判断session是否无效,当idle超过maxInactiveInterval时,则认定session为无效的(invalid=true)并执行expire方法;
     * 1.如果invalid=true则表示该session已经被销毁掉无需进入超时判断
     * 2.timeIdle时间为当前系统时间 - 最后一次活跃时间(也就是最后一次调用active方法)
     *
     * @return invalid(true.无效的, false.有效的)
     */
    public boolean invalid() {
        if (!invalid && this.maxInactiveInterval > 0) {
            int timeIdle = (int) (this.getIdleTimeInternal() / 1000L);
            if (timeIdle >= this.maxInactiveInterval) {
                this.expire(true);
            }
        }

        return invalid;
    }

    public void expire() {
        expire(true);
    }

    private void expire(boolean expiring) {
        if (invalid) {
            return;
        }

        invalid = expiring;

        recycle();
    }

    public void recycle() {
        if (manager != null && manager.has(id)) {
            manager.remove(id);

            SessionEvent event = new SessionEvent(this);
            event.type = SessionEventType.DESTROY;
            fireSessionListener(event);

            listeners.clear();
            creationTime = 0L;
            accessTime = 0L;
            listeners = null;
            maxInactiveInterval = 1L;
            invalid = true;
            manager = null;
        }
    }

    public Session addListener(SessionListener sessionListener) {
        if (sessionListener != null) {
            listeners.add(sessionListener);
        }
        return this;
    }

    public void removeListener(SessionListener sessionListener) {
        listeners.remove(sessionListener);
    }

    protected void fireSessionListener(SessionEvent event) {
        if (listeners != null) {
            for (SessionListener listener : listeners) {
                if (event.type == SessionEventType.CREATED) {
                    listener.created(event);
                } else if (event.type == SessionEventType.ACTIVE) {
                    listener.active(event);
                } else if (event.type == SessionEventType.CHANGE) {
                    listener.change(event);
                } else if (event.type == SessionEventType.DESTROY) {
                    listener.destroy(event);
                }
            }

            event.session = null;
        }
    }

    public String getId() {
        return id;
    }

    public SessionManager manager() {
        return manager;
    }

    public long getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(long maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    public long getAccessTime() {
        return accessTime;
    }
}