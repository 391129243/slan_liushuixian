package com.pcr.dataserver;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.client.*;

public class HttpPcrAnalysisGateway implements PcrAnalysisGateway {
    private final RestClient client;
    public HttpPcrAnalysisGateway(RestClient client) { this.client = client; }
    @Override public JsonNode capabilities() { return call("/v1/capabilities", null); }
    @Override public JsonNode read(SourceFile source) { return call("/v1/pcr/read", source); }
    @Override public JsonNode reanalyze(Reanalyze request) { return call("/v1/pcr/reanalyze", request); }
    @Override public JsonNode save(Save request) { return call("/v1/pcr/save", request); }
    @Override public byte[] download(String outputId) {
        if (outputId == null || !outputId.matches("[a-f0-9]{32}"))
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Invalid saved output ID");
        try {
            byte[] bytes = client.get().uri("/v1/pcr/files/{id}", outputId).retrieve().body(byte[].class);
            if (bytes == null || bytes.length == 0 || bytes.length > 32 * 1024 * 1024)
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Invalid saved output size");
            return bytes;
        } catch (RestClientException error) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not download saved PCR output");
        }
    }
    private JsonNode call(String path, Object body) {
        try {
            var request = body == null ? client.get().uri(path) : client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body);
            JsonNode response = request.retrieve().body(JsonNode.class);
            if (response == null) throw new ApiException(HttpStatus.BAD_GATEWAY, "Empty response from C# SDK bridge");
            return response;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 501)
                throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "C# offline SDK adapter is disabled");
            int status=error.getStatusCode().value();
            if (java.util.Set.of(400,404,409,422,429,503,504).contains(status))
                throw new ApiException(HttpStatus.valueOf(status), "C# offline SDK operation failed; check bridge logs and session state");
            throw new ApiException(HttpStatus.BAD_GATEWAY, "C# SDK bridge rejected the operation");
        } catch (ResourceAccessException error) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "C# SDK bridge is not reachable");
        }
    }
}
