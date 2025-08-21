package ec.edu.ups.Backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ShellWebSocketHandler extends TextWebSocketHandler {
    private static final ConcurrentHashMap<String, ShellSession> sessions = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        ShellSession shellSession = new ShellSession(session);
        sessions.put(session.getId(), shellSession);
        shellSession.start();
        sendJson(session, "status", "connected");
        System.out.println("✅ Nueva sesión WebSocket: " + session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        ShellSession shellSession = sessions.get(session.getId());
        if (shellSession == null) return;

        try {
            ObjectNode node = (ObjectNode) MAPPER.readTree(message.getPayload());
            String type = node.path("type").asText();
            switch (type) {
                case "cmd" -> shellSession.sendCommand(node.path("data").asText(""));
                case "close" -> shellSession.close();
                default -> sendJson(session, "error", "unknown message type");
            }
        } catch (Exception e) {
            sendJson(session, "error", "invalid payload");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ShellSession sh = sessions.remove(session.getId());
        if (sh != null) sh.close();
        System.out.println("👋 Sesión WebSocket cerrada: " + session.getId());
    }

    private void sendJson(WebSocketSession session, String type, String data) throws IOException {
        ObjectNode out = MAPPER.createObjectNode();
        out.put("type", type);
        out.put("data", data);
        session.sendMessage(new TextMessage(out.toString()));
    }

    // ---------------- ShellSession ----------------

    private static class ShellSession {
        private final WebSocketSession ws;
        private final Process process;
        private final BufferedWriter stdin;
        private final Thread stdoutThread;
        private final Thread stderrThread;

        private static final String PWD_START = "__PWD_START__";
        private static final String PWD_END   = "__PWD_END__";

        ShellSession(WebSocketSession session) throws IOException {
            this.ws = session;

            String containerName = getContainerName(session);
            if (containerName == null || containerName.isEmpty()) {
                throw new IllegalArgumentException("❌ Falta query param 'container'.");
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "-i", containerName,
                    "bash", "--noprofile", "--norc", "-i"
            );
            pb.redirectErrorStream(false);
            process = pb.start();

            stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

            stdoutThread = new Thread(() -> pump(process.getInputStream(), false));
            stderrThread = new Thread(() -> pump(process.getErrorStream(), true));

            stdoutThread.setName("shell-stdout-" + session.getId());
            stderrThread.setName("shell-stderr-" + session.getId());
        }

        void start() throws IOException {
            stdoutThread.start();
            stderrThread.start();

            // Entorno "simple" + aliases
            sendRaw(
                    "export PS1=;\n" +
                            "unset PROMPT_COMMAND 2>/dev/null;\n" +
                            "export TERM=dumb; export CLICOLOR=0; export LS_COLORS=;\n" +
                            "alias ls='ls -1 --color=never';\n" +
                            "alias ll='ls -alF -1 --group-directories-first --color=never';\n" +
                            "alias dir='ls -alF -1 --group-directories-first --color=never';\n" +
                            "alias cls='clear';\n"
            );

            // (Opcional) directorio inicial:
            // sendRaw("cd /var/lib/odoo 2>/dev/null || true\n");

            // Prompt inicial
            sendRaw("printf \"" + PWD_START + "%s" + PWD_END + "\\n\" \"$PWD\"\n");
        }

        void sendCommand(String cmd) throws IOException {
            if (cmd == null) cmd = "";
            String normalized = cmd.isBlank() ? ":" : cmd; // no-op si vacío
            sendRaw(normalized + "\n" +
                    "printf \"" + PWD_START + "%s" + PWD_END + "\\n\" \"$PWD\"\n");
        }

        private void pump(InputStream in, boolean err) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!ws.isOpen()) break;

                    if (line.contains(PWD_START) && line.contains(PWD_END)) {
                        String pwd = line.substring(
                                line.indexOf(PWD_START) + PWD_START.length(),
                                line.indexOf(PWD_END)
                        );
                        sendJson("pwd", pwd);
                    } else {
                        sendJson(err ? "stderr" : "stdout", line + "\n");
                    }
                }
            } catch (IOException ignored) {}
        }

        private void sendRaw(String s) throws IOException {
            stdin.write(s);
            stdin.flush();
        }

        void close() {
            try { stdin.close(); } catch (IOException ignored) {}
            try {
                process.destroy();
                if (process.isAlive()) {
                    if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                        process.destroyForcibly();
                    }
                }
            } catch (InterruptedException ignored) {}
            stdoutThread.interrupt();
            stderrThread.interrupt();
        }

        private void sendJson(String type, String data) {
            try {
                ObjectNode out = MAPPER.createObjectNode();
                out.put("type", type);
                out.put("data", data);
                synchronized (ws) {
                    if (ws.isOpen()) ws.sendMessage(new TextMessage(out.toString()));
                }
            } catch (IOException ignored) {}
        }

        private String getContainerName(WebSocketSession session) {
            try {
                String query = session.getUri() != null ? session.getUri().getQuery() : null;
                if (query == null) return null;
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "container".equals(kv[0])) {
                        String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        if (!val.matches("^[a-zA-Z0-9._-]+$")) return null; // validar
                        return val;
                    }
                }
                return null;
            } catch (Exception e) { return null; }
        }
    }
}
