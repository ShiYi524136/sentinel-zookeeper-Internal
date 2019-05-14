package com.alibaba.csp.sentinel.dashboard.dateTest;

/**
 * @ClassName: LocalDateTimeTest
 * @ProjectName sentinel-zookeeper-Internal
 * @author huangbing
 * @date 2019/5/1316:14
 */

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author huangbing
 * @create 2019-05-13 16:14
 * @since 1.0.0
 **/
public class LocalDateTimeTest {

    public static void main(String[] args) {

        // String nowDateStr = LocalDate.now().toString();
        // System.out.println(nowDateStr);// 2018-03-27
        //
        // LocalDate nowDate = LocalDate.parse("2018-03-25");
        // System.out.println(nowDate.toString());// 2018-03-25
        //
        // String nowTimeStr = LocalTime.now().toString();
        // System.out.println(nowTimeStr);// 13:45:07.105
        //
        // LocalTime nowTime = LocalTime.parse("12:10:13");
        // System.out.println(nowTime.toString());// 12:10:13
        //
        // System.out.println(LocalDateTime.now().toString());// 2018-03-27T13:55:34.047
        // System.out.println(LocalDateTime.now().toLocalDate().toString());// 2018-03-27
        // System.out.println(LocalDateTime.now().toLocalTime().toString());// 13:55:34.047
        //
        // System.out.println(LocalDateTime.MAX.toString());// +999999999-12-31T23:59:59.999999999
        // System.out.println(LocalDateTime.MIN.toString());// -999999999-01-01T00:00

        new LocalDateTimeTest().date2LocalDateTime(new Date());
        // new LocalDateTimeTest().localDateTime2Date(LocalDateTime.now());
    }

    /**
     * Date转换为LocalDateTime
     *
     * @param date
     */
    public void date2LocalDateTime(Date date) {
        Instant instant = date.toInstant();// An instantaneous point on the time-line.(时间线上的一个瞬时点。)
        ZoneId zoneId = ZoneId.systemDefault();// A time-zone ID, such as {@code Europe/Paris}.(时区)
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();

        System.out.println(localDateTime.toString());// 2018-03-27T14:07:32.668
        System.out.println(localDateTime.toLocalDate() + " " + localDateTime.toLocalTime());// 2018-03-27 14:48:57.453

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");// This class is
        // immutable and
        // thread-safe.@since
        // 1.8
        String format = dateTimeFormatter.format(localDateTime);
        System.out.println(format);// 2018-03-27 14:52:57

        Date start = Date.from(instant);
        System.out.println("第一次 ： " + start);

        LocalDateTime localDateTime3 = LocalDateTime.now();
        String format1 = localDateTime3.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("format1： " + format1);
        Date date2 = Date.from(localDateTime3.atZone(ZoneId.systemDefault()).toInstant());
        // LocalDateTime dateTime = LocalDateTime.ofInstant(date2.toInstant(), ZoneId.systemDefault());

        System.out.println("第二次 ： " + date2);
    }

    /**
     * LocalDateTime转换为Date
     *
     * @param localDateTime
     */
    public void localDateTime2Date(LocalDateTime localDateTime) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = localDateTime.atZone(zoneId);// Combines this date-time with a time-zone to create a
        // ZonedDateTime.
        Date date = Date.from(zdt.toInstant());
        System.out.println(date.toString());// Tue Mar 27 14:17:17 CST 2018
    }

}
