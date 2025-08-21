package ec.edu.ups.Backend.config;

import ec.edu.ups.Backend.websocket.ShellWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.io.IOException;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        try {
            registry.addHandler(shellWebSocketHandler(), "/ws/shell")
                    .setAllowedOriginPatterns("*"); // en lugar de setAllowedOrigins("*")
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public ShellWebSocketHandler shellWebSocketHandler() throws IOException {
        return new ShellWebSocketHandler();
    }
}
