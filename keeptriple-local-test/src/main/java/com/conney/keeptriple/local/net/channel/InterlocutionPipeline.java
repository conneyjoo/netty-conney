package com.conney.keeptriple.local.net.channel;

import com.conney.keeptriple.local.net.proto.Proto;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class InterlocutionPipeline<T extends Proto> {

    private static final Logger logger = LoggerFactory.getLogger(InterlocutionPipeline.class);

    private static final InterlocutionPipeline INSTANCE = new InterlocutionPipeline();

    public final AttributeKey<InterlocutionPipeline<T>> SERIAL_CHANNEL_PIPELINE_KEY = AttributeKey.valueOf("serialChannelPipelineKey");

    private Map<String, ChannelPromiseTable> map = new ConcurrentHashMap<>();

    public void answers(T t, Channel channel) {
        ChannelPromiseTable cpt = get(t, channel);

        if (cpt != null && !cpt.isEmpty()) {
            final SerialChannelPromise<T> promise = cpt.remove(t.serial());
            if (Objects.nonNull(promise)) {
                promise.wakeUp(t);
            }
        }
    }

    /**
     * 按照请求类型和对端信息缓存每个请求
     *
     * @param t
     * @param channel
     * @return
     */
    private String toKey(T t, Channel channel) {
        InetSocketAddress sa = (InetSocketAddress) channel.remoteAddress();
        return String.format("%s:%s:%d", t.getClass().getSimpleName(), sa.getAddress().getHostAddress(), sa.getPort());
    }

    private ChannelPromiseTable get(T t, Channel channel) {
        return map.get(toKey(t, channel));
    }

    public void ask(T t, SerialChannelPromise<T> promise) {
        ChannelPromiseTable cpt;
        String key = toKey(t, promise.channel());

        if ((cpt = map.get(key)) == null) {
            cpt = new ChannelPromiseTable();
            ChannelPromiseTable old = map.putIfAbsent(key, cpt);
            cpt = old != null ? old : cpt;
        }

        cpt.put(t, promise);
    }

    public boolean remove(T t, SerialChannelPromise<T> promise) {
        ChannelPromiseTable cpt = map.get(toKey(t, promise.channel()));
        return cpt != null && Objects.nonNull(cpt.remove(t.serial()));
    }

    public static <T extends Proto> Attribute<InterlocutionPipeline<T>> getInterlocutionPipeline(Channel channel) {
        return channel.attr(INSTANCE.SERIAL_CHANNEL_PIPELINE_KEY);
    }

    class ChannelPromiseTable extends ConcurrentSkipListMap<Long, SerialChannelPromise<T>> {

        AtomicLong serialGenerator = new AtomicLong();

        public SerialChannelPromise put(T t, SerialChannelPromise value) {
            return super.put(t.serial(nextSerial()), value);
        }

        private long nextSerial() {
            return serialGenerator.incrementAndGet() & Long.MAX_VALUE;
        }
    }
}
