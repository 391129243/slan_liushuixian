package com.pcr.dataserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PcrBridgeConfig {
    @Bean PcrAnalysisGateway pcrGateway(@Value("${dataserver.csharp-base-url:http://127.0.0.1:5099}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); factory.setReadTimeout(30000);
        return new HttpPcrAnalysisGateway(RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build());
    }
}
