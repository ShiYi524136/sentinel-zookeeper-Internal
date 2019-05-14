/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.discovery;

import static org.junit.Assert.fail;

import java.util.ConcurrentModificationException;
import java.util.Set;

import org.junit.Test;

public class AppInfoTest {

    @Test
    public void testConcurrentGetMachines() throws Exception {
        AppInfo appInfo = new AppInfo("testApp");
        appInfo.addMachine(genMachineInfo("hostName1", "10.18.129.91"));
        appInfo.addMachine(genMachineInfo("hostName2", "10.18.129.92"));
        Set<MachineInfo> machines = appInfo.getMachines();
        new Thread(() -> {
            try {
                for (MachineInfo m : machines) {
                    System.out.println(m);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
                fail();
            }

        }).start();
        Thread.sleep(100);
        try {
            appInfo.addMachine(genMachineInfo("hostName3", "10.18.129.93"));
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
            fail();
        }
        Thread.sleep(1000);
    }

    private MachineInfo genMachineInfo(String hostName, String ip) {
        MachineInfo machine = new MachineInfo();
        machine.setApp("testApp");
        machine.setHostname(hostName);
        machine.setIp(ip);
        machine.setPort(8719);
        machine.setVersion(String.valueOf(System.currentTimeMillis()));
        return machine;
    }

    @Test
    public void test() throws Exception {

        // FormatDateTimeFormatter formatter = new FormatDateTimeFormatter(dateFormatterPattern, parser, Locale.ROOT);
        // FormatDateTimeFormatter dateTimeFormatter = new DateMathParser(formatter);
        // DateTimeFormatter parser = dateTimeFormatter.parser();
        //
        // MutableDateTime date = new MutableDateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        // int end = parser.parseInto(date, "Fri May 10 10:51:30 CST 2019", 0);
    }
}