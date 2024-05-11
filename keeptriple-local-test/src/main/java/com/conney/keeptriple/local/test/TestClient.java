/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.NettyClient;
import com.conney.keeptriple.local.net.NettyContext;
import com.conney.keeptriple.local.net.codec.ProtoDecoder;
import com.conney.keeptriple.local.net.codec.ProtoEncoder;
import com.conney.keeptriple.local.util.ThreadUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.kqueue.DuplexKQueueSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
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
import java.util.concurrent.TimeUnit;

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
                //.channel(DuplexKQueueSocketChannel.class)
                .channel(KQueueSocketChannel.class)
                //.option(ChannelOption.MAX_MESSAGES_PER_READ, 16)
                //.option(ChannelOption.SO_RCVBUF, 1024 * 8)
                .handler(this)
                .connect(ip, port).sync();
    }

    @Override
    public void accept(ChannelHandlerContext ctx) {
        logger.info("accepted");

//        TestData testData = new TestData();
//        testData.setData(TestData.KB16);
//        ByteBuf buf = testData.encode();
//        ByteBuf data = buf.retain().duplicate();
//        ctx.channel().writeAndFlush(data).addListener((f) -> {
//            logger.info("write " + f.isSuccess());
//        });

        new Thread(() -> {
            ThreadUtils.sleepSilent(5000);
            int size = 0;
            TestData testData = new TestData();
            testData.setData(TestData.KB1);
            ByteBuf buf = testData.encode();
            for (int i = 0; i < size; i++) {
                ByteBuf data = buf.retain().duplicate();
                ctx.channel().writeAndFlush(data).addListener((f) -> {
                    //logger.info("write " + f.isSuccess());
                });
                //ThreadUtils.sleepSilent(1000);
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
        logger.info("heartbeat");
        ctx.writeAndFlush(HEARTBEAT);
    }

    @Override
    public String getName() {
        return "client";
    }

    @Override
    public IdleStateHandler getIdleStateHandler() {
        //return null;
        return new IdleStateHandler(0, 10, 0, TimeUnit.SECONDS);
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
