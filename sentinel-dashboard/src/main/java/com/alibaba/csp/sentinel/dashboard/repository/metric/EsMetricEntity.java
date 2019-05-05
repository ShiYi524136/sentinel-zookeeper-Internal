package com.alibaba.csp.sentinel.dashboard.repository.metric;

/**
 * @ClassName: EsMetricEntity
 * @ProjectName sentinel-zookeeper-Internal
 * @author huangbing
 * @date 2019/4/311:41
 */

/**
 * EsMetricEntity
 * 
 * @author huangbing
 * @create 2019-04-03 11:41
 * @since 1.0.0
 **/

import java.io.Serializable;
import java.util.Date;

import org.elasticsearch.common.UUIDs;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
@Document(indexName = "laidian-metric", type = "sentinel")
class EsMetricEntity implements Serializable {
    @Id
    @Field
    private String id = UUIDs.randomBase64UUID();
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date gmtCreate;
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date gmtModified;
    @Field(type = FieldType.Text)
    private String app;
    /**
     * 监控信息的时间戳
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date timestamp;
    @Field(type = FieldType.Text)
    private String resource;
    @Field(index = false, type = FieldType.Long)
    private Long passQps;
    @Field(index = false, type = FieldType.Long)
    private Long successQps;
    @Field(index = false, type = FieldType.Long)
    private Long blockQps;
    /**
     * 发生异常的次数
     */
    @Field(index = false, type = FieldType.Long)
    private Long exceptionQps;

    /**
     * 所有successQps的Rt的和。
     */
    @Field(index = false, type = FieldType.Double)
    private double rt;

    /**
     * 本次聚合的总条数
     */
    @Field(index = false, type = FieldType.Integer)
    private int count;
    @Field(index = false, type = FieldType.Integer)
    private int resourceCode;
}