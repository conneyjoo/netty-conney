package com.conney.keeptriple.local.net.handler;

import com.conney.keeptriple.local.net.channel.InterlocutionPipeline;
import com.conney.keeptriple.local.net.proto.Proto;
import com.conney.keeptriple.local.net.session.ClientSession;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.unix.Errors;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ChannelHandler.Sharable
public class NettyHandler extends SimpleChannelInboundHandler<Proto> {

	public static final AttributeKey<ClientSession> KEY_SESSION = AttributeKey.valueOf("SESSION");

	private static final Logger logger = LoggerFactory.getLogger(NettyHandler.class);

	private Dispatcher dispatcher;

	private SocketEventHandler socketEventHandler;

	public NettyHandler(Dispatcher dispatcher, SocketEventHandler socketEventHandler) {
		this.dispatcher = dispatcher;
		this.socketEventHandler = socketEventHandler;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Channel-Active: socket({})", ctx.channel());
		}

		socketEventHandler.accept(ctx);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Proto proto) throws Exception {
		try {
			if (proto.serializable()) {
				InterlocutionPipeline<Proto> pipeline = InterlocutionPipeline.getInterlocutionPipeline(ctx.channel()).get();

				if (pipeline != null) {
					pipeline.answers(proto, ctx.channel());
				}
			}

			Handler<Proto> handler = dispatcher.dispatch(proto.getCmd());

			if (handler != null) {
				handler.process(ctx, proto);
			} else {
				logger.trace("Unkown command({}) cause cannot find handle", proto.getCmd());
			}
		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
			throw t;
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleState state = ((IdleStateEvent) evt).state();

			if (state == IdleState.READER_IDLE) {
				if (logger.isInfoEnabled()) {
					ClientSession session = ctx.channel().attr(KEY_SESSION).get();

					if (logger.isInfoEnabled()) {
						if (session != null) {
							logger.info("IdleStateEvent triggered: No message received from {}, {} for a long time.", session.getId(), ctx.channel().remoteAddress());
						} else {
							logger.info("IdleStateEvent triggered: No message received from {} communication server for a long time.", ctx.channel().remoteAddress());
						}
					}
				}

				close(ctx);
			} else if (state == IdleState.WRITER_IDLE) {
				socketEventHandler.heartbeat(ctx);
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		//logger.info("channelWritabilityChanged: {}", ctx.channel().isWritable());
//		if (ctx.channel().isWritable()) {
//			ctx.flush();
//			System.out.println(PooledByteBufAllocator.DEFAULT.dumpStats());
//		}
		super.channelWritabilityChanged(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (logger.isInfoEnabled()) {
			ClientSession session = ctx.channel().attr(KEY_SESSION).get();

			if (session != null) {
				logger.info("Channel-Inactive: client({}), socket({})", session.getId(), ctx.channel().remoteAddress());
			} else {
				logger.info("Channel-Inactive: socket({})", ctx.channel().remoteAddress());
			}
		}

		close(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof Errors.NativeIoException) {
			Errors.NativeIoException nativeIoException = (Errors.NativeIoException) cause;

			if (nativeIoException.expectedErr() == Errors.ERRNO_ECONNRESET_NEGATIVE && logger.isInfoEnabled()) {
				logger.info("{}: {}", nativeIoException.getMessage(), ctx.channel().remoteAddress());
			} else {
				logger.error("Exception: {} - socket: {}", nativeIoException.getMessage(), ctx.toString(), nativeIoException);
			}
		} else {
			logger.error("Exception: {} - socket: {}", cause.getMessage(), ctx.toString(), cause);
		}

		if (cause instanceof IOException) {
			close(ctx);
		}
	}

	public void close(ChannelHandlerContext ctx) {
		ClientSession session = ctx.channel().attr(KEY_SESSION).getAndSet(null);

		socketEventHandler.close(ctx);

		if (session != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Close client({}) connection({})", session.getId(), ctx.channel().remoteAddress());
			}

			session.close(ctx);
		} else {
			if (logger.isInfoEnabled()) {
				logger.info("Close client connection({})", ctx.channel().remoteAddress());
			}

			ctx.close();
		}
	}
}