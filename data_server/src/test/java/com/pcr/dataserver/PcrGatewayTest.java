package com.pcr.dataserver;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

class PcrGatewayTest {
    @Test void capabilitiesAreForwardedWithoutInventingSdkReadiness() {
        var builder=RestClient.builder().baseUrl("http://bridge");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://bridge/v1/capabilities"))
            .andRespond(withSuccess("{\"sdkReady\":false}",MediaType.APPLICATION_JSON));
        assertThat(new HttpPcrAnalysisGateway(builder.build()).capabilities().path("sdkReady").asBoolean()).isFalse();
        server.verify();
    }
    @Test void saveCallsCsharpAndNeverReturnsFakeSuccess() {
        var builder=RestClient.builder().baseUrl("http://bridge");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://bridge/v1/pcr/save")).andExpect(method(HttpMethod.POST))
            .andExpect(content().json("{\"analysisId\":\"a1\",\"fileName\":\"result.pcr\"}"))
            .andRespond(withStatus(HttpStatus.NOT_IMPLEMENTED));
        assertThatThrownBy(() -> new HttpPcrAnalysisGateway(builder.build()).save(new PcrAnalysisGateway.Save("a1","result.pcr")))
            .isInstanceOfSatisfying(ApiException.class,error -> assertThat(error.status()).isEqualTo(HttpStatus.NOT_IMPLEMENTED));
        server.verify();
    }
}
