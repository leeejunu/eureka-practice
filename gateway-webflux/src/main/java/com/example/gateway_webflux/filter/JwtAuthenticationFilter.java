package com.example.gateway_webflux.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory {

    Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            String token = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            log.info("jwt token: {}", token);

            // 토큰이 없을 경우 401 Unauthorized
            if (token == null || token.isEmpty()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            // SecretKey로 토큰 검증 및 Payload 가져오기 sha256
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            String subject = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            log.info("payload: {}", subject);

            // Payload를 X-User-Id 헤더에 담아서 Request 전달
            return chain.filter(
                    exchange.mutate()
                            .request(
                                    exchange.getRequest()
                                            .mutate()
                                            .header("X-User-Id", subject)
                                            .build()
                            ).build()
            );
        };
    }
}
