package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.NettyServer;
import com.conney.keeptriple.local.net.codec.ProtoDecoder;
import com.conney.keeptriple.local.net.codec.ProtoEncoder;
import com.conney.keeptriple.local.util.ThreadUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

@Import({Heartbeat.class, TestData.class, TestDataHandler2.class, HeartbeatHandler.class})
public class TestServer extends NettyServer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestServer.class);

    public TestServer(ApplicationContext springContent) {
        super(springContent, 1, 1);
    }

    @Override
    public void initChannel(ServerBootstrap b) {
    }

    @Override
    public String getName() {
        return "server";
    }

    @Override
    public void accept(ChannelHandlerContext ctx) {
        new Thread(() -> {
            ThreadUtils.sleepSilent(5000);
            int size = 0;
            TestData testData = new TestData();
            testData.setData(TestData.KB1);
            ByteBuf buf = testData.encode();
            for (int i = 0; i < size; i++) {
                ctx.writeAndFlush(buf.retain().duplicate());
                //ThreadUtils.sleepSilent(1000);
            }
            logger.info("test completed " + size);
        }).start();
    }

    @Override
    public IdleStateHandler getIdleStateHandler() {
        return new IdleStateHandler(2000, 0, 0, TimeUnit.SECONDS);
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
        start(2200);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(TestServer.class).web(WebApplicationType.NONE).run(args);
    }
}
