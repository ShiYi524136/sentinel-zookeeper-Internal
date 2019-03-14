package com.cc.sentinel.starter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * sentinel配置类（该配置需要配置在引用此包的配置文件中）
 * for example
 * <p>
 * yml格式
 * sentinel:
 *  application:
 *  enable: true
 *  name: sentinel-consumer
 *  port: 8719
 * dashboard: localhost:8181
 *  zookeeper:
 *  enable: true
 *  address: localhost:2181
 *  autoVoteEnable: true
 * @author: cc
 * @Date: 2019-03-07
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sentinel")
public class SentinelProperties {

    /**
     * 应用配置
     */
    private ApplicationProperties application;

    /**
     * zk配置
     */
    private ZookeeperProperties zookeeper;

    /**
     * 应用配置
     */
    @Getter
    @Setter
    public static class ApplicationProperties {

        /**
         * 是否开启sentinel功能
         */
        private boolean enable;

        /**
         * 客户端的 port，用于上报相关信息（默认为 8719）, 同台机器上由多台时，需要指定不同的端口
         */
        private String port;

        /**
         * 控制台的地址 IP + 端口
         */
        private String dashboard;

        /**
         * 应用名称，会在控制台中显示
         */
        private String name;
    }


    /**
     * zookeeper配置文件
     */
    @Getter
    @Setter
    public static class ZookeeperProperties {

        /**
         * 服务器地址
         */
        private String address;

        /**
         * 睡眠时间
         */
        private int sleepTimeMs = 100;

        /**
         * 最大重试
         */
        private int maxRetries = 3;

        /**
         * 是否开启zk作为数据源
         */
        private boolean enable;

        /**
         * 是否开启集群模式的自动选举
         */
        private boolean autoVoteEnable;
    }
}
