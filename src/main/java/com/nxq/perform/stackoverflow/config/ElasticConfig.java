package com.nxq.perform.stackoverflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import java.time.Duration;

@Configuration
public class ElasticConfig extends ElasticsearchConfiguration {

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo("172.31.22.44:9200") // IP Elastic của bạn
                .withConnectTimeout(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.ofSeconds(3))
                // Tăng connection pool lên cao để chịu tải
                .withClientConfigurer(
                        ElasticsearchClients.ElasticsearchHttpClientConfigurationCallback.from(
                                httpAsyncClientBuilder -> httpAsyncClientBuilder
                                        .setMaxConnTotal(1000) // Cho phép 1000 kết nối đồng thời
                                        .setMaxConnPerRoute(200)
                        )
                )
                .build();
    }
}