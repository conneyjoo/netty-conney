package com.conney.keeptriple.local.net.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager<T extends Session> {

    private int maxInactiveInterval = 0;

    protected Map<String, T> sessions = new ConcurrentHashMap<>();

    public Session get(String id) {
        return sessions.get(id);
    }

    public void processExpires() {
        T session;

        for (Iterator<T> iterable = getSessions().iterator(); iterable.hasNext(); ) {
            session = iterable.next();
            if (session.invalid()) {
                session.expire();
            }
        }
    }

    public boolean has(String id) {
        return sessions.containsKey(id);
    }

    public void add(T session) {
        sessions.put(session.getId(), session);
    }

    public void remove(Session session) {
        remove(session.getId());
    }

    public void remove(String id) {
        sessions.remove(id);
    }

    public Collection<T> getSessions() {
        return sessions.values();
    }

    public void clean() {
        sessions.clear();
    }

    public int size() {
        return sessions.size();
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }
}
