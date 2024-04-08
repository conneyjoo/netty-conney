package com.conney.keeptriple.local.net.handler;

import com.conney.keeptriple.local.bean.Preset;
import com.conney.keeptriple.local.net.annotation.NonBlocking;
import com.conney.keeptriple.local.net.initializer.NettyInitializer;
import com.conney.keeptriple.local.net.listener.HandlerListener;
import com.conney.keeptriple.local.net.proto.Proto;
import com.conney.keeptriple.local.util.ReflectUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Dispatcher implements Preset {

    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private Map<Byte, Handler> handlers = new ConcurrentHashMap<>();

    private Collection<HandlerListener> listeners;

    private NettyInitializer nettyInitializer;

    private ThreadPoolExecutor handleExecutor;

    private LinkedBlockingQueue<Runnable> handleQueue = new LinkedBlockingQueue<>();

    public Dispatcher(NettyInitializer nettyInitializer) {
        this.nettyInitializer = nettyInitializer;
        this.handleExecutor = createHandleExecutor();
    }

    public void afterPropertiesSet() {
        listeners = nettyInitializer.getSpringContent().getBeansOfType(HandlerListener.class).values();
        listeners.stream().filter((e) -> nettyInitializer.isProtoType(getProto(e))).collect(Collectors.toList());

        Map<String, Handler> beans = nettyInitializer.getSpringContent().getBeansOfType(Handler.class);
        Set<Map.Entry<String, Handler>> entries = beans.entrySet();

        for (Map.Entry<String, Handler> entry : entries) {
            Handler handler = entry.getValue();
            handler.afterPropertiesSet();
            register(handler);
        }
    }

    public void register(Handler handler) {
        Proto proto = getProto(handler);

        if (nettyInitializer.isProtoType(proto)) {
            handlers.put(proto.getCmd(), handler);
            bindListener(handler, proto.getCmd());
            handler.setBlocking(!handler.getClass().isAnnotationPresent(NonBlocking.class));
            handler.setSessionManager(nettyInitializer.getSessionManager());
            handler.setNettyInitializer(nettyInitializer);
            handler.setHandleExecutor(handleExecutor);
        }
    }

    public Handler dispatch(Byte cmd) {
        return handlers.get(cmd);
    }

    public void bindListener(Handler handler, Byte cmd) {
        if (listeners != null) {
            for (HandlerListener listener : listeners) {
                if (cmd.equals(getCmd(listener))) {
                    handler.addListener(listener);
                }
            }
        }
    }

    public Byte getCmd(Object obj) {
        Proto proto = getProto(obj);
        return proto != null ? proto.getCmd() : null;
    }

    public Proto getProto(Object obj) {
        try {
            return (Proto) ReflectUtils.getGenericParameterType(obj.getClass()).newInstance();
        } catch (InstantiationException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    public Set<Map.Entry<String, Proto>> getProtocols() {
        return nettyInitializer.getSpringContent().getBeansOfType(Proto.class).entrySet();
    }

    private ThreadPoolExecutor createHandleExecutor() {
        BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("handle-thread-%d").priority(Thread.MAX_PRIORITY).build();
        return new ThreadPoolExecutor(64, 64, 0, TimeUnit.MILLISECONDS, handleQueue, factory);
    }

    public ThreadPoolExecutor getHandleExecutor() {
        return handleExecutor;
    }

    public LinkedBlockingQueue<Runnable> getHandleQueue() {
        return handleQueue;
    }
}