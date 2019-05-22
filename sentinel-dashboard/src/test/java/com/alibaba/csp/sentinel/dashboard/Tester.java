package com.alibaba.csp.sentinel.dashboard;



import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.csp.sentinel.dashboard.DashboardApplication;

/**
 * 单元测试继承该类即可
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DashboardApplication.class)
//@Transactional
//@Rollback
public abstract class Tester {}



