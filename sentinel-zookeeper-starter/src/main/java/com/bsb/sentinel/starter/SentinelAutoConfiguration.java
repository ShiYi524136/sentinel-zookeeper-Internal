package com.bsb.sentinel.starter;

import com.bsb.sentinel.starter.config.SentinelConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Sentinel自动配置类
 * @author: cc
 * @Date: 2019-03-06
 */
@Configuration
@Import(SentinelConfiguration.class)
public class SentinelAutoConfiguration {

}
