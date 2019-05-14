package com.alibaba.csp.sentinel.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import com.alibaba.csp.sentinel.init.InitExecutor;

/**
 * Sentinel dashboard application. 应用启动类，可以直接启动.<br/>
 * 默认端口号8080 控制台访问地址：<url>http://localhost:8080/#/dashboard/home</url> <br/>
 * 
 * @author Carpenter Lee
 */
@SpringBootApplication
@EnableElasticsearchRepositories // 开启使用elasticsearch存储
public class DashboardApplication {

    public static void main(String[] args) {
        triggerSentinelInit();
        SpringApplication.run(DashboardApplication.class, args);
    }

    private static void triggerSentinelInit() {
        new Thread(() -> InitExecutor.doInit()).start();
    }
}
