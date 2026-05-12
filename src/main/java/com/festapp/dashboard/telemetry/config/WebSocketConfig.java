package com.festapp.dashboard.telemetry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic"); // 프론트엔드가 구독할 경로
    config.setApplicationDestinationPrefixes("/app"); // 프론트엔드가 메시지를 보낼 경로
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-stomp") // 최초 웹소켓 연결 주소
        .setAllowedOriginPatterns(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://43.201.141.9:*",
            "https://*.vercel.app",
            "https://*.netlify.app"
        ) // SockJS credentials 요청은 wildcard Origin("*")을 사용할 수 없습니다.
        .withSockJS();
  }
}
