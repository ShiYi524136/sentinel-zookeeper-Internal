package com.alibaba.csp.sentinel.dashboard.es;

/**
 * @ClassName: EsClientTest
 * @ProjectName sentinel-zookeeper-Internal
 * @author huangbing
 * @date 2019/5/1015:55
 */

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 *
 * @author huangbing
 * @create 2019-05-10 15:55
 * @since 1.0.0
 **/
public class EsClientTest {

    private final static int port = 9300;

    @SuppressWarnings("unused")
    public static void main(String[] args) throws IOException {
        TransportClient client = client = new PreBuiltTransportClient(Settings.EMPTY)
            .addTransportAddresses(new TransportAddress(InetAddress.getByName("172.16.2.69"), port));
        IndexResponse response =
            client.prepareIndex("msg", "tweet", "1").setSource(XContentFactory.jsonBuilder().startObject()
                .field("userName", "张三1").field("sendDate", new Date()).field("msg", "msg1").endObject()).get();
        IndexResponse response2 =
            client.prepareIndex("msg", "tweet", "2").setSource(XContentFactory.jsonBuilder().startObject()
                .field("userName", "张三2").field("sendDate", new Date()).field("msg", "msg2").endObject()).get();
        IndexResponse response3 =
            client.prepareIndex("msg", "tweet", "3").setSource(XContentFactory.jsonBuilder().startObject()
                .field("userName", "张三3").field("sendDate", new Date()).field("msg", "msg3").endObject()).get();
        /*  String jsonStr = "{\"name\":\"tom zhang\",\"age\":19}";
        IndexResponse responseJson = client.prepareIndex("msg", "tweet", "1").setSource(jsonStr).get();*/
        // DeleteResponse dResponse = client.prepareDelete("msg", "tweet", "1").execute().actionGet();
        // System.err.println(dResponse.getResult().getLowercase());
        GetResponse getResponse = client.prepareGet("msg", "tweet", "1").get();
        System.err.println(getResponse.getSourceAsString());
        client.close();
    }

}
