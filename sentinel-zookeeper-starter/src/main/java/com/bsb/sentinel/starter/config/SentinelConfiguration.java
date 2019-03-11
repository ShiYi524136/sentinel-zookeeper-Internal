package com.bsb.sentinel.starter.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterParamFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
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
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.csp.sentinel.util.HostNameUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.bsb.sentinel.starter.entity.ClusterGroupEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Sentinel启动配置类
 *
 * @author: cc
 * @Date: 2019-03-06
 */
@Configuration
@EnableConfigurationProperties(SentinelProperties.class)
@Slf4j
public class SentinelConfiguration implements InitializingBean {

    @Autowired
    private SentinelProperties sentinelProperties;

    @Autowired
    private Environment env;


    /**
     * sentinel注解切面
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    @Override
    public void afterPropertiesSet() {
        // 设置主页(Sentinel-dashboard服务的地址和端口号)
        if (StringUtil.isNotBlank(sentinelProperties.getApplication().getDashboard())) {
            SentinelConfig.setConfig(TransportConfig.CONSOLE_SERVER, sentinelProperties.getApplication().getDashboard());
        }

        // 设置端口(单机多实例请在配置文件设置不同的端口号)
        if (StringUtil.isNotBlank(sentinelProperties.getApplication().getPort())) {
            SentinelConfig.setConfig(TransportConfig.SERVER_PORT, sentinelProperties.getApplication().getPort());
        }
    }

    /**
     * zookeeper的datasource注册
     */
    @Configuration
    @ConditionalOnProperty(name = "sentinel.zookeeper.enable", havingValue = "true")
    public class ZookeeperDataSourceConfiguration implements InitializingBean {

        public static final String FLOW_RULE_PATH = "/sentinel/rules/%s/flow";
        public static final String AUTHORITY_RULE_PATH = "/sentinel/rules/%s/authority";
        public static final String DEGRADE_RULE_PATH = "/sentinel/rules/%s/degrade";
        public static final String PARAM_FLOW_RULE_PATH = "/sentinel/rules/com.bsb.atp.AtpApplication/paramflow";
        public static final String SYSTEM_RULE_PATH = "/sentinel/rules/%s/system";
        public static final String configDataId =  "/sentinel/rules/%s/cluster-client-config";
        public static final String clusterMapDataId = "/sentinel/rules/%s/cluster-map";
        /*public static final String FLOW_POSTFIX = "/sentinel/rules/%s/flow-rules";
        public static final String PARAM_FLOW_POSTFIX = "/sentinel/rules/%s/param-rules";*/

        @Override
        public void afterPropertiesSet() {
            log.info("当前系统名称为{}",AppNameUtil.getAppName());
            ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(FLOW_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                    }));
            FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

            ReadableDataSource<String, List<AuthorityRule>> authorityRleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(AUTHORITY_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<AuthorityRule>>() {
                    }));
            AuthorityRuleManager.register2Property(authorityRleDataSource.getProperty());

            ReadableDataSource<String, List<DegradeRule>> degradeRleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(DEGRADE_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<DegradeRule>>() {
                    }));
            DegradeRuleManager.register2Property(degradeRleDataSource.getProperty());

            ReadableDataSource<String, List<ParamFlowRule>> paramFlowRleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(PARAM_FLOW_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {
                    }));
            ParamFlowRuleManager.register2Property(paramFlowRleDataSource.getProperty());

            ReadableDataSource<String, List<SystemRule>> systemRuleDataSource = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(SYSTEM_RULE_PATH, AppNameUtil.getAppName()),
                    source -> JSON.parseObject(source, new TypeReference<List<SystemRule>>() {
                    }));
            SystemRuleManager.register2Property(systemRuleDataSource.getProperty());

            ReadableDataSource<String, ClusterClientConfig> clientConfigDs = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(configDataId, AppNameUtil.getAppName()), source -> JSON.parseObject(source, new TypeReference<ClusterClientConfig>() {}));
            ClusterClientConfigManager.registerClientConfigProperty(clientConfigDs.getProperty());

            ReadableDataSource<String, ClusterClientAssignConfig> clientAssignDs = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(clusterMapDataId, AppNameUtil.getAppName()), source -> {
                List<ClusterGroupEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterGroupEntity>>() {});
                return Optional.ofNullable(groupList)
                        .flatMap(this::extractClientAssignment)
                        .orElse(null);
            });
            ClusterClientConfigManager.registerServerAssignProperty(clientAssignDs.getProperty());



            ClusterFlowRuleManager.setPropertySupplier(namespace -> {
                ReadableDataSource<String, List<FlowRule>> ds = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                        String.format(FLOW_RULE_PATH, AppNameUtil.getAppName()), source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {}));
                return ds.getProperty();
            });

            // Register cluster parameter flow rule property supplier which creates data source by namespace.
            ClusterParamFlowRuleManager.setPropertySupplier(namespace -> {
                ReadableDataSource<String, List<ParamFlowRule>> ds = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                        String.format(PARAM_FLOW_RULE_PATH, AppNameUtil.getAppName()), source -> JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {}));
                return ds.getProperty();
            });


            ReadableDataSource<String, ServerTransportConfig> serverTransportDs = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(clusterMapDataId, AppNameUtil.getAppName()), source -> {
                List<ClusterGroupEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterGroupEntity>>() {});
                return Optional.ofNullable(groupList)
                        .flatMap(this::extractServerTransportConfig)
                        .orElse(null);
            });
            ClusterServerConfigManager.registerServerTransportProperty(serverTransportDs.getProperty());


            ReadableDataSource<String, Integer> clusterModeDs = new ZookeeperDataSource<>(sentinelProperties.getZookeeper().getAddress(),
                    String.format(clusterMapDataId, AppNameUtil.getAppName()), source -> {
                List<ClusterGroupEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterGroupEntity>>() {});
                return Optional.ofNullable(groupList)
                        .map(this::extractMode)
                        .orElse(ClusterStateManager.CLUSTER_NOT_STARTED);
            });
            ClusterStateManager.registerProperty(clusterModeDs.getProperty());
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
        private static final String SEPARATOR = "@";
    }
}
