package com.cc.sentinel.starter.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterParamFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.command.CommandHandler;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.entity.ClusterClientStateEntity;
import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.zookeeper.ZookeeperDataSource;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.csp.sentinel.transport.command.SimpleHttpCommandCenter;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.csp.sentinel.util.HostNameUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cc.sentinel.starter.entity.ClusterGroupEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Sentinel启动配置类，该类完成了相关bean的创建，zk数据源的注册与监听，集群服务的TokenServer自动选举，宕机自动failover
 *
 * @author: cc
 * @Date: 2019-03-06
 */
@Configuration
@EnableConfigurationProperties(SentinelProperties.class)
@Slf4j
@ConditionalOnProperty(name = "sentinel.application.enable", havingValue = "true")
public class SentinelConfiguration implements InitializingBean, DisposableBean {

    @Autowired
    private SentinelProperties sentinelProperties;

    @Autowired
    private CuratorFramework curatorFramework;

    /**
     * CuratorFramework重试策略
     */
    private static final RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);


    /**
     * sentinel注解切面
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    /**
     * zk客户端实例
     *
     * @return
     */
    @Bean
    public CuratorFramework getClient() {
        CuratorFramework client = CuratorFrameworkFactory.newClient(sentinelProperties.getZookeeper().getAddress(), retryPolicy);
        client.start();
        log.info("创建CuratorFramework实例并启动");
        return client;
    }

    /**
     * 初始化逻辑，加载各个规则的数据源配置，然后开启集群选举
     */
    @Override
    public void afterPropertiesSet() {
        // 设置主页(Sentinel-dashboard服务的地址和端口号)
        if (StringUtil.isNotBlank(sentinelProperties.getApplication().getDashboard())) {
            SentinelConfig.setConfig(TransportConfig.CONSOLE_SERVER, sentinelProperties.getApplication().getDashboard());
        }

        // 设置端口(单机多实例请在配置文件或者启动参数中设置不同的端口号)
        if (StringUtil.isNotBlank(sentinelProperties.getApplication().getPort())) {
            SentinelConfig.setConfig(TransportConfig.SERVER_PORT, sentinelProperties.getApplication().getPort());
        }
    }

    /**
     * 主动关闭CuratorFramework实例，以便其他服务实例尽早开始选举
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        if (curatorFramework != null) {
            curatorFramework.close();
        }
        log.info("销毁CuratorFramework实例，删除临时节点");
    }

    /**
     * zookeeper的datasource注册
     */
    @Configuration
    @ConditionalOnProperty(name = "sentinel.zookeeper.enable", havingValue = "true")
    public class ZookeeperDataSourceConfiguration implements InitializingBean {

        /**
         * 流控规则节点路径
         */
        private final String FLOW_RULE_PATH = "/sentinel/rules/%s/flow";
        /**
         * 授权规则节点路径
         */
        private final String AUTHORITY_RULE_PATH = "/sentinel/rules/%s/authority";
        /**
         * 降级规则节点路径
         */
        private final String DEGRADE_RULE_PATH = "/sentinel/rules/%s/degrade";
        /**
         * 热点规则节点路径
         */
        private final String PARAM_FLOW_RULE_PATH = "/sentinel/rules/%s/paramflow";
        /**
         * 系统规则节点路径
         */
        private final String SYSTEM_RULE_PATH = "/sentinel/rules/%s/system";
        /**
         * 集群客户端信息路径
         */
        private final String CONFIG_DATA_ID = "/sentinel/rules/%s/cluster-client-config";
        /**
         * 集群信息路径
         */
        private final String CLUSTER_MAP_DATA_ID = "/sentinel/rules/%s/cluster-map";
        /**
         * ip@port分隔符
         */
        private final String SEPARATOR = "@";
        /**
         * SimpleHttpCommandCenter设置client、server模式api
         */
        private final String MODIFY_CLUSTER_MODE_PATH = "setClusterMode";
        /**
         * SimpleHttpCommandCenter修改client、server模式api
         */
        private final String MODIFY_CONFIG_PATH = "cluster/client/modifyConfig";
        /**
         * TokenServer自动选举使用的zk节点路径(临时节点)
         */
        private final String TOKEN_SERVER_VOTE_PATH = String.format("/sentinel/vote/%s", AppNameUtil.getAppName());
        /**
         * TokenServer自动选举存储当前TokenServer的IP的路径(临时节点)
         */
        private final String TOKEN_SERVER_IP_PATH = String.format("/sentinel/tokenServerIp/%s", AppNameUtil.getAppName());
        /**
         * TokenServer默认端口号
         */
        private final int DEFAULT_PORT = 18730;
        /**
         * 单位：秒
         */
        private final int AWAIT_TIME = 5;

        @Override
        public void afterPropertiesSet() {
            log.info("当前系统名称为{}", AppNameUtil.getAppName());
            //流控规则注册
            ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(FLOW_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                    }));
            FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

            //授权规则注册
            ReadableDataSource<String, List<AuthorityRule>> authorityRleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(AUTHORITY_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<AuthorityRule>>() {
                    }));
            AuthorityRuleManager.register2Property(authorityRleDataSource.getProperty());

            //降级规则注册
            ReadableDataSource<String, List<DegradeRule>> degradeRleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(DEGRADE_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<DegradeRule>>() {
                    }));
            DegradeRuleManager.register2Property(degradeRleDataSource.getProperty());

            //热点参数规则注册
            ReadableDataSource<String, List<ParamFlowRule>> paramFlowRleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(PARAM_FLOW_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {
                    }));
            ParamFlowRuleManager.register2Property(paramFlowRleDataSource.getProperty());

            //系统参数规则注册
            ReadableDataSource<String, List<SystemRule>> systemRuleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(SYSTEM_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<SystemRule>>() {
                    }));
            SystemRuleManager.register2Property(systemRuleDataSource.getProperty());

            //集群客户端配置注册
            ReadableDataSource<String, ClusterClientConfig> clientConfigDs = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(CONFIG_DATA_ID, AppNameUtil.getAppName()), source -> JSON.parseObject(source, new TypeReference<ClusterClientConfig>() {
            }));
            ClusterClientConfigManager.registerClientConfigProperty(clientConfigDs.getProperty());

            //集群服务端配置注册
            ReadableDataSource<String, ClusterClientAssignConfig> clientAssignDs = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(CLUSTER_MAP_DATA_ID, AppNameUtil.getAppName()), source -> {
                List<ClusterGroupEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterGroupEntity>>() {
                });
                return Optional.ofNullable(groupList)
                        .flatMap(this::extractClientAssignment)
                        .orElse(null);
            });
            ClusterClientConfigManager.registerServerAssignProperty(clientAssignDs.getProperty());

            //集群流控规则注册
            ClusterFlowRuleManager.setPropertySupplier(namespace -> {
                ReadableDataSource<String, List<FlowRule>> ds = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                        String.format(FLOW_RULE_PATH, AppNameUtil.getAppName()), source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                }));
                return ds.getProperty();
            });

            //集群热点规则注册
            ClusterParamFlowRuleManager.setPropertySupplier(namespace -> {
                ReadableDataSource<String, List<ParamFlowRule>> ds = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                        String.format(PARAM_FLOW_RULE_PATH, AppNameUtil.getAppName()), source -> JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {
                }));
                return ds.getProperty();
            });

            //初始化集群服务端配置
            ReadableDataSource<String, ServerTransportConfig> serverTransportDs = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(CLUSTER_MAP_DATA_ID, AppNameUtil.getAppName()), source -> {
                List<ClusterGroupEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterGroupEntity>>() {
                });
                return Optional.ofNullable(groupList)
                        .flatMap(this::extractServerTransportConfig)
                        .orElse(null);
            });
            ClusterServerConfigManager.registerServerTransportProperty(serverTransportDs.getProperty());

            //初始化集群服务端状态配置
            ReadableDataSource<String, Integer> clusterModeDs = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(CLUSTER_MAP_DATA_ID, AppNameUtil.getAppName()), source -> {
                List<ClusterGroupEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterGroupEntity>>() {
                });
                return Optional.ofNullable(groupList)
                        .map(this::extractMode)
                        .orElse(ClusterStateManager.CLUSTER_NOT_STARTED);
            });
            ClusterStateManager.registerProperty(clusterModeDs.getProperty());

            //开始集群选举注册(由属性配置决定是否开启)
            try {
                Boolean autoVoteEnable = sentinelProperties.getZookeeper().isAutoVoteEnable();
                if (autoVoteEnable != null && autoVoteEnable) {
                    curatorToVote();
                }
            } catch (Exception e) {
                log.error("集群选举注册失败，异常信息", e);
            }
        }

        /**
         * 通过zk框架curator来自动选举集群流控的TokenServer和TokenClient
         */
        private void curatorToVote() throws Exception {
            String ip = InetAddress.getLocalHost().getHostAddress(); //获取本机i
            LeaderLatch leaderLatch = new LeaderLatch(curatorFramework, TOKEN_SERVER_VOTE_PATH, ip);
            leaderLatch.addListener(new LeaderLatchListener() {
                @Override
                public void notLeader() {
                    log.info("ip为{}由leader变换为follower", ip);
                    changeClusterMode(ip, ClusterStateManager.CLUSTER_CLIENT);
                }

                @Override
                public void isLeader() {
                    log.info("ip为{}选举为leader", ip);
                    changeClusterMode(ip, ClusterStateManager.CLUSTER_SERVER);
                }
            });
            leaderLatch.start();
            leaderLatch.await(AWAIT_TIME, TimeUnit.SECONDS);
            if (!leaderLatch.hasLeadership()) {
                changeClusterMode(ip, ClusterStateManager.CLUSTER_CLIENT);
            }
        }

        /**
         * 通过调用SimpleHttpCommandCenter调用api变换TokenServer和TokenClient
         *
         * @param ip
         * @param mode
         */
        private void changeClusterMode(String ip, int mode) {
            CommandRequest request = new CommandRequest();
            request.addParam("mode", String.valueOf(mode));
            if (mode == 0) {
                Stat stat = null;
                try {
                    stat = curatorFramework.checkExists().forPath(TOKEN_SERVER_IP_PATH);
                    if (stat != null) {
                        //调用api指定当前服务器为TokenClient
                        CommandHandler<?> commandHandler = SimpleHttpCommandCenter.getHandler(MODIFY_CLUSTER_MODE_PATH);
                        commandHandler.handle(request);

                        //调用api指定当前TokenClient的TokenServer信息
                        ClusterClientStateEntity entity = new ClusterClientStateEntity();
                        //获取当前TokenServer的ip地址
                        byte[] bytes = curatorFramework.getData().forPath(TOKEN_SERVER_IP_PATH);
                        String serverIp = new String(bytes);
                        entity.setServerHost(serverIp);
                        entity.setServerPort(DEFAULT_PORT);
                        entity.setRequestTimeout(60);
                        request.addParam("data", JSON.toJSONString(entity));
                        CommandHandler<?> commandHandler2 = SimpleHttpCommandCenter.getHandler(MODIFY_CONFIG_PATH);
                        commandHandler2.handle(request);
                    }
                } catch (Exception e) {
                    log.error(ip + "指定为TokenClient模式异常，异常信息", e);
                }
            } else if (mode == 1) {
                Stat stat = null;
                try {
                    stat = curatorFramework.checkExists().forPath(TOKEN_SERVER_IP_PATH);
                    if (stat == null) {
                        curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(TOKEN_SERVER_IP_PATH, ip.getBytes());
                    }
                    CommandHandler<?> commandHandler = SimpleHttpCommandCenter.getHandler(MODIFY_CLUSTER_MODE_PATH);
                    commandHandler.handle(request);
                } catch (Exception e) {
                    log.error(ip + "指定为TokenServer模式异常，异常信息", e);
                }
            }
        }

        private int extractMode(List<ClusterGroupEntity> groupList) {
            // If any server group machineId matches current, then it's token server.
            if (groupList.stream().anyMatch(this::machineEqual)) {
                return ClusterStateManager.CLUSTER_SERVER;
            }
            // If current machine belongs to any of the token server group, then it's token client.
            // Otherwise it's unassigned, should be set to NOT_STARTED.
            boolean canBeClient = groupList.stream()
                    .flatMap(e -> e.getClientSet().stream())
                    .filter(Objects::nonNull)
                    .anyMatch(e -> e.equals(getCurrentMachineId()));
            return canBeClient ? ClusterStateManager.CLUSTER_CLIENT : ClusterStateManager.CLUSTER_NOT_STARTED;
        }

        private Optional<ServerTransportConfig> extractServerTransportConfig(List<ClusterGroupEntity> groupList) {
            return groupList.stream()
                    .filter(this::machineEqual)
                    .findAny()
                    .map(e -> new ServerTransportConfig().setPort(e.getPort()).setIdleSeconds(600));
        }

        private Optional<ClusterClientAssignConfig> extractClientAssignment(List<ClusterGroupEntity> groupList) {
            if (groupList.stream().anyMatch(this::machineEqual)) {
                return Optional.empty();
            }
            // Build client assign config from the client set of target server group.
            for (ClusterGroupEntity group : groupList) {
                if (group.getClientSet().contains(getCurrentMachineId())) {
                    String ip = group.getIp();
                    Integer port = group.getPort();
                    return Optional.of(new ClusterClientAssignConfig(ip, port));
                }
            }
            return Optional.empty();
        }

        private boolean machineEqual(/*@Valid*/ ClusterGroupEntity group) {
            return getCurrentMachineId().equals(group.getMachineId());
        }

        private String getCurrentMachineId() {
            // Note: this may not work well for container-based env.
            return HostNameUtil.getIp() + SEPARATOR + TransportConfig.getRuntimePort();
        }
    }
}