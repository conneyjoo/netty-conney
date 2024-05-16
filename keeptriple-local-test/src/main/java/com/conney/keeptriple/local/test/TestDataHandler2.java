package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.annotation.ProtoHandler;
import com.conney.keeptriple.local.net.handler.AbstractHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ProtoHandler
public class TestDataHandler2 extends AbstractHandler<TestData> {

    private static final Logger logger = LoggerFactory.getLogger(TestDataHandler2.class);

    private AtomicInteger i = new AtomicInteger();

    private List<Integer> nums = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        //Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> logger.info("i = {}", i), 1, 30, TimeUnit.SECONDS);
        for (int i = 1; i <= 0; i++) {
            nums.add(i);
        }
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TestData testData) {
        i.incrementAndGet();
        logger.info("server data {}", i.get());
        Collections.shuffle(nums);
    }
}
