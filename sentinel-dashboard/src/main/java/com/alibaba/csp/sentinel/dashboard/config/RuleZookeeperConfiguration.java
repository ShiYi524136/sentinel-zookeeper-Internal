package com.alibaba.csp.sentinel.dashboard.config;

import com.alibaba.csp.sentinel.util.StringUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description zookeeper数据源配置类
 * @author: cc
 * @Date: 2019-03-04
 */
@Configuration
@EnableConfigurationProperties(DashboardProperties.class)
@ConditionalOnProperty(name = "sentinel.zookeeper.enable", havingValue = "true")
public class RuleZookeeperConfiguration implements DisposableBean, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(RuleZookeeperConfiguration.class);

    @Autowired
    private DashboardProperties dashboardProperties;


    //CuratorFramework创建一个实例就可以了，是线程安全的
    @Bean
    public CuratorFramework getClient() {
        if (StringUtil.isBlank(dashboardProperties.getZookeeper().getAddress())) {
            throw new IllegalArgumentException(String.format("Bad argument: serverAddr=[%s]", dashboardProperties.getZookeeper().getAddress()));
        }
        logger.info("创建CuratorFramework实例");
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(dashboardProperties.getZookeeper().getAddress()).
                retryPolicy(new ExponentialBackoffRetry(dashboardProperties.getZookeeper().getSleepTimeMs(), dashboardProperties.getZookeeper().getMaxRetries())).
                build();
        client.start();
        logger.info("启动CuratorFramework实例完成");
        return client;
    }

    @Override
    public void destroy() {
        logger.info("销毁CuratorFramework实例");
        CuratorFramework client = getClient();
        if (null != client) {
            client.close();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}