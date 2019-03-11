package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.AbstractZookeeperRuleProvider;
import org.springframework.stereotype.Service;

/**
 * @version Revision: 0.0.1
 * @author: weihuang.peng
 * @Date: 2018-12-22
 */
@Service("degradeRuleZookeeperProvider")
public class DegradeRuleZookeeperProvider extends AbstractZookeeperRuleProvider<DegradeRuleEntity> {

}
