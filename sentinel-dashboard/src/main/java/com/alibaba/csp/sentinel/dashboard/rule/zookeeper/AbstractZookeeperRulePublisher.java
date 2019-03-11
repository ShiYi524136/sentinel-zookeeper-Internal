package com.alibaba.csp.sentinel.dashboard.rule.zookeeper;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.fastjson.JSON;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 * @Description Zookeeper规则发布抽象类
 * @author: cc
 * @Date: 2019-03-04
 */
public abstract class AbstractZookeeperRulePublisher<T extends RuleEntity> implements DynamicRulePublisher<T> {

    @Autowired
    private CuratorFramework curatorFramework;

    public static final String PATH = "/sentinel/rules/%s/%s";

    @Override
    public void publish(String app, List<T> rules) throws Exception {
        String reallyPath = String.format(getZookeeperPath(), app, getRuleName());
        if (rules == null) {
            return;
        }
        Stat stat = getClient().checkExists().forPath(reallyPath);
        String rule = ruleEntityEncoder().convert(rules);
        if (stat == null) {
            getClient().create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(reallyPath, rule.getBytes());
            return;
        }
        getClient().setData().forPath(reallyPath, rule.getBytes());
    }

    private CuratorFramework getClient() {
        return this.curatorFramework;
    }

    private String getZookeeperPath() {
        return PATH;
    }

    private Converter<List<T>, String> ruleEntityEncoder() {
        return JSON::toJSONString;
    }

    private Class<T> getEntityRealClassName() {
        ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
        return (Class<T>) pt.getActualTypeArguments()[0];
    }

    private String getRuleName() {
        String entityName = getEntityRealClassName().getSimpleName();
        return entityName.substring(0, entityName.indexOf(RuleEntity.class.getSimpleName())).toLowerCase();
    }
}
