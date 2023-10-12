package ru.practicum.stats.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.dto.EndpointHitDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class StatsClient {
    private final RestTemplate rest;

    public StatsClient(RestTemplate rest) {
        this.rest = rest;
    }

    public ResponseEntity<Object> saveHit(EndpointHitDto endpointHitDto) {
        return makeAndSendRequest(HttpMethod.POST, "/hit", null, endpointHitDto);
    }

    public ResponseEntity<Object> getStats(LocalDateTime start, LocalDateTime end, Boolean unique) {
        String encodedStart = URLEncoder.encode(start.toString(), StandardCharsets.UTF_8);
        String encodedEnd = URLEncoder.encode(end.toString(), StandardCharsets.UTF_8);
        Map<String, Object> parameters = Map.of(
                "start", encodedStart,
                "end", encodedEnd,
                "unique", unique
        );
        return makeAndSendRequest(HttpMethod.GET, "/stats?start={start}&end={end}&unique={unique}",
                parameters, null);
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private static ResponseEntity<Object> prepareResponse(ResponseEntity<Object> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());

        if (response.hasBody()) {
            return responseBuilder.body(response.getBody());
        }

        return responseBuilder.build();
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(HttpMethod method, String path, @Nullable Map<String,
            Object> parameters, @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        ResponseEntity<Object> responseEntity;
        try {
            if (parameters != null) {
                responseEntity = rest.exchange(path, method, requestEntity, Object.class, parameters);
            } else {
                responseEntity = rest.exchange(path, method, requestEntity, Object.class);
            }
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
        }
        return prepareResponse(responseEntity);
    }
}
