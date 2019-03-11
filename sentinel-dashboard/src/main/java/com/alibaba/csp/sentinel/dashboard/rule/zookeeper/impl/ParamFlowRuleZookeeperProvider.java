package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.AbstractZookeeperRuleProvider;
import org.springframework.stereotype.Service;

/**
 * @Description Zookeeper规则查询类
 * @author: cc
 * @Date: 2019-03-04
 */
@Service("paramFlowRuleZookeeperProvider")
public class ParamFlowRuleZookeeperProvider extends AbstractZookeeperRuleProvider<ParamFlowRuleEntity> {

}
