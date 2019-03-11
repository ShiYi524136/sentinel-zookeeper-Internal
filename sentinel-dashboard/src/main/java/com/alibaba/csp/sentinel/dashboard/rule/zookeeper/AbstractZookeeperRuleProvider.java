package com.alibaba.csp.sentinel.dashboard.rule.zookeeper;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;

/**
 * @Description Zookeeper规则查询抽象类
 * @author: cc
 * @Date: 2019-03-04
 */
public abstract class AbstractZookeeperRuleProvider<T extends RuleEntity> implements DynamicRuleProvider<T> {

    @Autowired
    private CuratorFramework curatorFramework;

    public static final String PATH = "/sentinel/rules/%s/%s";


    @Override
    public List<T> getRules(String appName) throws Exception {
        byte[] bytes = getClient().getData().forPath(String.format(getZookeeperPath(), appName, getRuleName()));
        if (null == bytes || bytes.length == 0) {
            return Collections.emptyList();
        }

        String rules = new String(bytes);
        if (StringUtil.isEmpty(rules)) {
            return Collections.emptyList();
        }
        List list = getRuleEntityDecoders().convert(rules);
        return list;
    }

    private String getZookeeperPath() {
        return PATH;
    }

    private Class<T> getEntityRealClassName() {
        ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
        return (Class<T>) pt.getActualTypeArguments()[0];
    }

    private CuratorFramework getClient() {
        return this.curatorFramework;
    }

    private Converter<String, List<T>> getRuleEntityDecoders() {
        return s -> JSON.parseArray(s, getEntityRealClassName());
    }

    private String getRuleName() {
        String entityName = getEntityRealClassName().getSimpleName();
        return entityName.substring(0, entityName.indexOf(RuleEntity.class.getSimpleName())).toLowerCase();
    }

}
