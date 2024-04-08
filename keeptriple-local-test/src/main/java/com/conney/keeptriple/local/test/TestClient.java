package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.NettyClient;
import com.conney.keeptriple.local.net.NettyContext;
import com.conney.keeptriple.local.net.codec.ProtoDecoder;
import com.conney.keeptriple.local.net.codec.ProtoEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import java.io.UnsupportedEncodingException;

@Import({Heartbeat.class, TestData.class, TestDataHandler.class})
public class TestClient extends NettyClient implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestClient.class);

    public static final Heartbeat HEARTBEAT = new Heartbeat();

    private EventLoopGroup bossGroup;

    private Bootstrap bs;

    public TestClient(ApplicationContext springContent) {
        super(springContent);
    }

    public void connect(String ip, int port) throws InterruptedException, UnsupportedEncodingException {
        bossGroup = NettyContext.get().getEventLoopGroup(1, getName());
        bs = new Bootstrap();

        bs.group(bossGroup)
                .channel(NettyContext.get().getChannelClass())
                //.option(ChannelOption.MAX_MESSAGES_PER_READ, 16)
                //.option(ChannelOption.SO_RCVBUF, 1024 * 8)
                .handler(this)
                .connect(ip, port).sync();
    }

    @Override
    public void accept(ChannelHandlerContext ctx) {
        System.out.println("accepted");

        new Thread(() -> {
            int size = 1;
            TestData testData = new TestData();
            testData.setData(TestData.KB16);
            ByteBuf buf = testData.encode();
            for (int i = 0; i < size; i++) {
                ctx.writeAndFlush(buf.retain().duplicate());
            }
            logger.info("test completed " + size);
        }).start();
    }

    @Override
    public void close(ChannelHandlerContext ctx) {
        super.close(ctx);
        System.exit(-1);
    }

    @Override
    public void heartbeat(ChannelHandlerContext ctx) {
//        logger.info("heartbeat");
//        ctx.writeAndFlush(HEARTBEAT);
    }

    @Override
    public String getName() {
        return "client";
    }

    @Override
    public IdleStateHandler getIdleStateHandler() {
        return null;
        //return new IdleStateHandler(0, 10, 0, TimeUnit.SECONDS);
    }

    @Override
    public ByteToMessageDecoder getLengthFieldBasedFrameDecoder() {
        return new GameLengthFieldDecoder();
    }

    @Override
    public ProtoEncoder getEncoder() {
        return new GameProtoEncoder();
    }

    @Override
    public ProtoDecoder getDecoder() {
        return new GameProtoDecoder();
    }

    @Override
    public void run(String... args) throws Exception {
        connect("127.0.0.1", 2200);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(TestClient.class).web(WebApplicationType.NONE).run(args);
    }
}
