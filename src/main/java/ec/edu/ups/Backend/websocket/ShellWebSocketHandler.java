package ec.edu.ups.Backend.websocket;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShellWebSocketHandler extends TextWebSocketHandler {
    private static final ConcurrentHashMap<String, ShellSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        ShellSession shellSession = new ShellSession(session);
        sessions.put(session.getId(), shellSession);
        shellSession.startReading();
        System.out.println("Nueva sesión WebSocket establecida: " + session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        ShellSession shellSession = sessions.get(session.getId());
        if (shellSession != null) {
            shellSession.sendCommand(message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ShellSession shellSession = sessions.remove(session.getId());
        if (shellSession != null) {
            shellSession.close();
        }
        System.out.println("Sesión WebSocket cerrada: " + session.getId());
    }

    private static class ShellSession {
        private final WebSocketSession session;
        private final Process process;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final Thread outputThread;

        public ShellSession(WebSocketSession session) throws IOException {
            this.session = session;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                this.process = new ProcessBuilder("cmd.exe").redirectErrorStream(true).start();
            } else {
                this.process = new ProcessBuilder("/bin/bash").redirectErrorStream(true).start();
            }
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            this.outputThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(line));
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error en la lectura del proceso: " + e.getMessage());
                }
            });
        }

        public void startReading() {
            outputThread.start();
        }

        public void sendCommand(String command) throws IOException {
            writer.write(command + "\n");
            writer.flush();
        }

        public void close() {
            try {
                process.destroy();
                reader.close();
                writer.close();
                outputThread.interrupt();
            } catch (IOException e) {
                System.out.println("Error al cerrar la sesión: " + e.getMessage());
            }
        }
    }
}
