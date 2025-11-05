package org.forif_backend.infrastructure.external.oauth;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.user.GoogleOAuthClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GoogleOAuthClientImpl implements GoogleOAuthClient {

    private final WebClient webClient;

    public GoogleOAuthClientImpl() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .responseTimeout(Duration.ofMillis(5000))
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(3000, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(3000, TimeUnit.MILLISECONDS)));

        this.webClient = WebClient.builder()
            .baseUrl("https://www.googleapis.com/oauth2/v3/userinfo")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Override
    public String getEmailFromToken(String token) {
        try {
            GoogleUserInfo response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("access_token", token)
                    .build())
                .retrieve()
                .bodyToMono(GoogleUserInfo.class)
                .block();

            log.info("Google user email retrieved successfully: {}", response.email());
            return response.email();
        } catch (WebClientResponseException e) {
            log.error("Error while retrieving user email: {}", e.getResponseBodyAsString(), e);
            throw new ForifException(ErrorCode.BAD_REQUEST, "유효하지 않은 Google 토큰입니다.");
        } catch (Exception e) {
            log.error("Unexpected error while retrieving user email", e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR, "사용자 이메일을 가져오는 중 오류가 발생했습니다.");
        }
    }

    private record GoogleUserInfo(String email) {}
}
