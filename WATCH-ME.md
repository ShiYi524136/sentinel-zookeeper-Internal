# 目录
- 介绍
- 问题
- 控制台改造
- 客户端改造
- 自动选举
# 介绍
[sentinel-github介绍](https://github.com/alibaba/Sentinel/wiki/介绍)
# 问题
- sentinel开源版本原始模式，如果不做任何修改，Dashboard的推送规则方式是通过API将规则推送至客户端并直接更新到内存中，这就将导致重启应用规则就失效需要重新添加规则
- 集群流控原始版本如果不做任何修改，需要到控制台手动分配TokenServer和TokenClient，无法自动选举和宕机自动重新选举
- 如何低侵入性方便简洁的使用sentinel
# 控制台改造
原始模式

![](_v_images/20190315092316655_20206.png =749x)

push模式  
![](_v_images/20190315092426427_16740.png =838x)
  
改造如下
1.首先找到页面规则相关的controller，该controller中有相关的规则查询、推送更新的方法，将对应的方法改为zk实现
![](_v_images/20190318144640153_14650.png =842x)
  
2.新建AbstractZookeeperRuleProvider、AbstractZookeeperRulePublisher分别实现DynamicRuleProvider、DynamicRulePublisher来实现动态规则的获取与推送  
AbstractZookeeperRuleProvider的获取规则方法
```
public List<T> getRules(String app, String ip, int port, String appName) throws Exception {
        byte[] bytes = getClient().getData().forPath(String.format(getZookeeperPath(), appName, getRuleName()));
        if (null == bytes || bytes.length == 0) {
            return Collections.emptyList();
        }
        String rules = new String(bytes);
        if (StringUtil.isEmpty(rules)) {
            return Collections.emptyList();
        }
        //List list = getRuleEntityDecoders().convert(rules);
        List list = getRuleEntityDecoders(app, ip, port, rules);
        return list;
    }
```
  
AbstractZookeeperRulePublisher的推送方法
```
@Override
    public void publish(String app, List<T> rules) throws Exception {
        String reallyPath = String.format(getZookeeperPath(), app, getRuleName());
        if (rules == null) {
            return;
        }
        Stat stat = getClient().checkExists().forPath(reallyPath);
        //String rule = ruleEntityEncoder().convert(rules);
        String rule = ruleEntityEncoder(rules);
        if (stat == null) {
            getClient().create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(reallyPath, rule.getBytes());
            return;
        }
        getClient().setData().forPath(reallyPath, rule.getBytes());
    }
```
  
子类Publisher与Provider实现了具体的RuleEntity与Rule的相互转换，举例如下
```
@Service("flowRuleZookeeperProvider")
public class FlowRuleZookeeperProvider extends AbstractZookeeperRuleProvider<FlowRuleEntity> {
    @Override
    protected List<FlowRuleEntity> getRuleEntityDecoders(String app, String ip, int port, String rules) {
        List<FlowRule> ruleZ = RuleUtils.parseFlowRule(rules);
        if (rules != null) {
            return ruleZ.stream().map(rule -> FlowRuleEntity.fromFlowRule(app, ip, port, rule))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }
}
@Service("flowRuleZookeeperPublisher")
public class FlowRuleZookeeperPublisher extends AbstractZookeeperRulePublisher<FlowRuleEntity> {
    @Override
    protected String ruleEntityEncoder(List<FlowRuleEntity> rules) {
        String data = JSON.toJSONString(rules.stream().map(FlowRuleEntity::toFlowRule).collect(Collectors.toList()));
        return data;
    }
}
```

3.修改了ParamFlowRuleEntity的一个注解@JsonIgnore为@JSONField(serialize = false)，前面注解是jackson的，后面是fastjson的，因为用的是fastjson所以修改为后面注解方式，不然会导致序列化错误
  
4.之前规则id获取采用的是AtomicLong,服务重启后值就会消失，故修改此地方获取全局不重复id，使用zk分布式id修改InMemoryRuleRepositoryAdapter如下
```
public long nextId(T entity) {
        String entityName = entity.getClass().getSimpleName();
        DistributedAtomicLong distAtomicLong = new DistributedAtomicLong(curatorFramework, "/" + entityName, new ExponentialBackoffRetry(SLEEP_TIME, RETRY_TIMES));
        long num;
        try {
            num = distAtomicLong.increment().postValue();
        } catch (Exception e) {
            return nextId();
        }
        return initNum + num;
    }
```
  
5.pom增加引用sentinel-datasource-zookeeper
```
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-datasource-zookeeper</artifactId>
            <version>${project.version}</version>
            <exclusions>
                <exclusion>
                    <artifactId>slf4j-log4j12</artifactId>
                    <groupId>org.slf4j</groupId>
                </exclusion>
            </exclusions>
        </dependency>
```
  
# 客户端改造
问题1：原始客户端接入模式需要早启动参数中增加配置，不是很方便。使用zk数据源后，客户端需要增加对应的规则监听与注册，希望能有一个统一的依赖jar完成这个事情  
![](_v_images/20190318155700263_18281.png =665x)
  
问题2：使用集群流控功能时候，需要手动指定TokenServer和TokenClient，这样每次应用重启都需要配置一次此配置
上述问题解决方案  
增加一个模块sentinel-zookeeper-starter，模块目录如下  
![](_v_images/20190318161451447_8774.png)
  
通过SPI方式发现服务，spring.factories文件内容如下，spring-boot启动时候会加载META-INF下面的的配置文件
```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.cc.sentinel.starter.SentinelAutoConfiguration
```
SentinelConfiguration类完成了程序启动的初始化内容，该类完成了相关bean的创建，zk数据源的注册与监听，集群服务的TokenServer自动选举，宕机自动failover，注意：默认端口是
```
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
``` 


然后，pom中增加，deploy即可发布此jar到maven仓库，所有需要使用sentinel的客户端引用此jar，然后在配置文件增加如下配置即可  
pom文件修改
```
<distributionManagement>
        <snapshotRepository>
            <id>snapshots</id>
            <url>http://172.16.1.110:8081/nexus/content/repositories/snapshots</url>
        </snapshotRepository>
        <repository>
            <id>releases</id>
            <name>Releases</name>
            <url>http://172.16.1.110:8081/nexus/content/repositories/releases</url>
        </repository>
    </distributionManagement>
```
配置文件修改（yml实例）sentinel.application.enable代表是否开启sentinel，sentinel.zookeeper.autoVoteEnable代表是否开启自动选举
```
sentinel:
  application:
    enable: true
    name: sentinel-atp
    port: 8819
    dashboard: localhost:8080
  zookeeper:
    enable: true
    address: localhost:2181
    autoVoteEnable: true
```