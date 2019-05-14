package com.alibaba.csp.sentinel.dashboard.es;

/**
 * @ClassName: EsMulConditionQuery
 * @ProjectName sentinel-zookeeper-Internal
 * @author huangbing
 * @date 2019/5/1311:57
 */

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.HashMap;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 * ElasticSearch编程实例—时间范围查询
 * 
 * @author huangbing
 * @create 2019-05-13 11:57
 * @since 1.0.0
 **/
public class EsMulConditionQuery {

    private static String index = "test_index5";
    private static String type = "test_type5";

    public static void main(String[] args) throws IOException, ParseException {
        // Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "elasticsearch")
        // .put("client.transport.sniff", true)// 开启ES嗅探功能，确保集群连上多个节点
        // .build();
        // // 创建客户端
        // TransportClient transportClient = new TransportClient(settings);
        // // 添加es的节点信息，可以添加1个或多个
        // TransportAddress transportAddress = new InetSocketTransportAddress("218.193.126.32", 9300);
        // transportClient.addTransportAddresses(transportAddress);

        Settings settings =
            Settings.builder().put("cluster.name", "elasticsearch").put("client.transport.sniff", true).build();
        TransportClient transportClient = new PreBuiltTransportClient(settings)
            .addTransportAddresses(new TransportAddress(InetAddress.getByName("172.16.2.69"), 9300));

        // 连接到的节点
        // ImmutableList<DiscoveryNode> connectedNodes = transportClient.connectedNodes();
        // for (DiscoveryNode discoveryNode : connectedNodes) {
        // System.out.println(discoveryNode.getHostAddress());
        // }
        /** 进行Mapping设置，这一步设置了索引字段的存储格式，极其重要，否则后面的查询会查出0条记录 */
        XContentBuilder mapping =
            XContentFactory.jsonBuilder().startObject().startObject("properties").startObject("PolicyCode")
                .field("type", "string").field("index", "not_analyzed").endObject().startObject("ServiceId")
                .field("type", "string").field("index", "not_analyzed").endObject().startObject("CreateTime")
                .field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss").endObject().endObject().endObject();

        CreateIndexRequestBuilder prepareCreate = transportClient.admin().indices().prepareCreate(index);
        prepareCreate.setSettings(settings).addMapping(type, mapping).execute().actionGet();

        /** 向索引库中插入数据 */
        for (int i = 0; i < 10; i++) {
            HashMap<String, Object> hashMap = new HashMap<String, Object>();
            if (i % 2 == 0) {
                hashMap.put("PolicyCode", "5674504720");
                hashMap.put("ServiceId", "SE2");
                hashMap.put("CreateTime", "2016-08-21 00:00:01");
            } else {
                hashMap.put("PolicyCode", "666666666");
                hashMap.put("ServiceId", "SE3");
                hashMap.put("CreateTime", "2016-10-21 00:00:01");
            }
            IndexResponse indexResponse = transportClient.prepareIndex(index, type).setSource(hashMap).get();
        }

        /**
         * rangeQuery时间范围查询 以下三种查询方式的效果一样
         */
        // 多条件查询
        SearchResponse searchResponse = transportClient.prepareSearch(index).setTypes(type)
            .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchPhraseQuery("PolicyCode", "5674504720"))
                .must(QueryBuilders.rangeQuery("CreateTime").from("2016-8-21 00:00:01").to("2017-7-21 00:00:03")))
            .get();
        //
        // SearchResponse searchResponse = transportClient.prepareSearch(index)
        // .setTypes(type)
        // .setQuery(QueryBuilders.rangeQuery("CreateTime").from("2016-07-21 11:00:00").to("2017-07-21 11:00:00"))
        // .get();
        //
        // SearchResponse searchResponse = transportClient.prepareSearch(index)
        // .setTypes(type)
        // .setQuery(QueryBuilders.rangeQuery("CreateTime").gt("2016-07-21 11:00:00").lt("2017-07-21 11:00:00"))
        // .get();

        /**
         * rangeFilter时间范围查询 以下两种查询方式的效果一样
         */
        // SearchResponse searchResponse = transportClient.prepareSearch(index)
        // .setTypes(type)
        // .setPostFilter(FilterBuilders.rangeFilter("age").gt(2).lt(5)).get();
        // .setPostFilter(FilterBuilders.rangeFilter("age").from(2).to(5)).get();

        // 获取查询结果
        SearchHits hits = searchResponse.getHits();
        long totalHits = hits.getTotalHits();
        System.out.println("总数目=" + totalHits);
        SearchHit[] hits2 = hits.getHits();
        for (SearchHit searchHit : hits2) {
            System.out.println(searchHit.getSourceAsString());
        }
    }
}
