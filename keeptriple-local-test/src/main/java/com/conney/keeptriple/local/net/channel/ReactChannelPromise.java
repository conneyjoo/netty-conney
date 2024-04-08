package com.conney.keeptriple.local.net.channel;

import com.conney.keeptriple.local.net.proto.Proto;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ReactChannelPromise<T extends Proto> extends SerialChannelPromise<T> {

	private static final Logger logger = LoggerFactory.getLogger(ReactChannelPromise.class);

	private Callback callback;

	public ReactChannelPromise(Channel channel, EventExecutor executor) {
		super(channel, executor);
	}

	public ReactChannelPromise(Channel channel, EventExecutor executor, Callback callback) {
		super(channel, executor);
		this.callback = callback;
	}

	@Override
	public T writeAndFlush(T t) {
		InterlocutionPipeline pipeline = null;

		try {
			Channel channel = channel();
			Attribute<InterlocutionPipeline<T>> attribute = InterlocutionPipeline.getInterlocutionPipeline(channel);

			if ((pipeline = attribute.get()) == null) {
				pipeline = new InterlocutionPipeline();
				InterlocutionPipeline old = attribute.setIfAbsent(pipeline);
				pipeline = old != null ? old : pipeline;
			}

			pipeline.ask(t, this);
			channel.writeAndFlush(t, this);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);

			if (Objects.nonNull(pipeline)) {
				pipeline.remove(t, this);
			}
		}

		return getValue();
	}

	public void terminate(T t) {
		Attribute<InterlocutionPipeline<T>> attribute = InterlocutionPipeline.getInterlocutionPipeline(channel());
		InterlocutionPipeline<T> pipeline = attribute.get();
		pipeline.remove(t, this);
	}

	@Override
	public boolean wakeUp(Proto t) {
		if (Objects.nonNull(callback)) {
			callback.run(t);
		}
		return true;
	}

	public void setCallback(Callback<T> callback) {
		this.callback = callback;
	}

	public interface Callback<T extends Proto> {

		void run(T t);
	}
}
