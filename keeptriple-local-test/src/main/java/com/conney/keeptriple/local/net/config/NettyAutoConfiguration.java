package com.conney.keeptriple.local.net.config;

import com.conney.keeptriple.local.net.initializer.NettyInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;

import java.util.Collection;

@Configuration
public class NettyAutoConfiguration {

    @Bean
    public NettyApplicationListener nettyApplicationListener(ApplicationContext context) {
        return new NettyApplicationListener(context);
    }

    public static class NettyApplicationListener implements ApplicationListener {

        private static final Logger logger = LoggerFactory.getLogger(NettyApplicationListener.class);

        private ApplicationContext applicationContext;

        public NettyApplicationListener(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public void onApplicationEvent(ApplicationEvent applicationEvent) {
            if (applicationEvent instanceof ContextStoppedEvent || applicationEvent instanceof ContextClosedEvent) {
                Collection<NettyInitializer> nettyInitializers = applicationContext.getBeansOfType(NettyInitializer.class).values();

                if (nettyInitializers == null || nettyInitializers.size() < 0) {
                    logger.info("Nothing to shutdown.");
                }

                for (NettyInitializer nettyInitializer : nettyInitializers) {
                    nettyInitializer.shutdownGracefully();
                }
            }
        }
    }
}
