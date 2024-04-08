package com.conney.keeptriple.local.net.session;

import com.conney.keeptriple.local.net.handler.NettyHandler;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientSessionManager extends SessionManager<ClientSession> {

    private int maxContextSize;

    public ClientSession create(String id, ChannelHandlerContext ctx) {
        return create(id, ctx, null, null);
    }

    public ClientSession create(String id, ChannelHandlerContext ctx, SessionListener listener) {
        return create(id, ctx, listener, null);
    }

    public ClientSession create(String id, ChannelHandlerContext ctx, SessionListener listener, Callback callback) {
        final AtomicBoolean isNew = new AtomicBoolean();

        ClientSession session = sessions.compute(id, (key, value) -> {
            if (value == null) {
                value = createSession(id);
                value.addListener(listener);
                isNew.set(true);
            } else {
                isNew.set(false);
            }

            return value;
        });

        ctx.channel().attr(NettyHandler.KEY_SESSION).setIfAbsent(session);
        session.addContext(ctx);

        if (callback != null) {
            callback.call(isNew.get(), id, session);
        }

        if (isNew.get()) {
            session.tellNew();
        } else {
            session.change();
        }

        return session;
    }

    public ClientSession createSession(String id) {
        return createSession(id, null);
    }

    public ClientSession createSession(String id, ChannelHandlerContext ctx) {
        ClientSession clientSession = ctx != null ? new ClientSession(id, ctx, this) : new ClientSession(id, this);
        clientSession.setMaxInactiveInterval(getMaxInactiveInterval());

        if (maxContextSize > 0) {
            clientSession.setMaxContextSize(maxContextSize);
        }

        return clientSession;
    }

    public int getMaxContextSize() {
        return maxContextSize;
    }

    public void setMaxContextSize(int maxContextSize) {
        this.maxContextSize = maxContextSize;
    }

    @Override
    public ClientSession get(String clientId) {
        return (ClientSession) super.get(clientId);
    }

    public int getActiveCount() {
        return getSessions().stream().mapToInt(session -> session.getContexts().size()).sum();
    }

    public String toSessionString() {
        Collection<ClientSession> sessions = getSessions();
        StringBuilder strings = new StringBuilder();
        Iterator<ClientSession> iterator = sessions.iterator();

        if (iterator.hasNext()) {
            strings.append(StringUtils.join(iterator.next().getContexts().stream().map(e -> e.channel()).iterator(), ","));

            while (iterator.hasNext()) {
                strings.append(", ").append(StringUtils.join(iterator.next().getContexts().stream().map(e -> e.channel()).iterator(), ","));
            }
        }

        return strings.toString();
    }

    public interface Callback {
        void call(boolean isNew, String id, ClientSession session);
    }
}
