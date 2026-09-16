/*
 * MIT License — same terms as the parent project.
 */
package com.locallogsearch.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX-based native desktop wrapper for Local Log Search ("Little Log Peep").
 *
 * <p>Same "singular UI surface, no browser tab" pattern as
 * geekychris/history_viewer's hv-app and code_graph_search's desktop
 * module. Startup:
 *
 * <ol>
 *   <li>Parse CLI args ({@code --port}, {@code --service-jar}, {@code --data-dir}).</li>
 *   <li>Locate the sibling Spring Boot service JAR
 *       (jpackage layout, or dev-tree layout).</li>
 *   <li>Pick a free loopback port (or use user-supplied).</li>
 *   <li>Spawn {@code java -jar log-search-service.jar --server.port=…}
 *       as a child so the Spring Boot classloader stays isolated in
 *       its own JVM. This module contains only JavaFX + a launcher —
 *       no Spring on our classpath at all.</li>
 *   <li>Poll the port; once reachable, load
 *       {@code http://127.0.0.1:&lt;port&gt;/} into a WebView.</li>
 *   <li>On window close, terminate the child process cleanly.</li>
 * </ol>
 *
 * <p>Packaged with {@code jpackage} (see {@code Makefile}) to produce
 * a self-contained {@code Little Log Peep.app} with a bundled JRE
 * that satisfies the child-process {@code java} invocation from the
 * same runtime — no external Java required.
 */
public class DesktopMain extends Application {

    private static final Logger log = LoggerFactory.getLogger(DesktopMain.class);

    // Args stashed statically because JavaFX Application.start()
    // doesn't get them directly — Platform launches via Application
    // reflection and we lose the main() argv unless we grab them here.
    private static String[] cliArgs = new String[0];

    private int port;
    private Path serviceJar;
    private Path dataDir;
    private Process serviceProcess;

    public static void main(String[] args) {
        cliArgs = args;
        launch(args);
    }

    @Override
    public void init() throws Exception {
        int userPort = 0;
        Path userServiceJar = null;
        Path userDataDir = null;
        for (int i = 0; i < cliArgs.length; i++) {
            switch (cliArgs[i]) {
                case "--port" -> {
                    if (i + 1 < cliArgs.length) userPort = Integer.parseInt(cliArgs[++i]);
                }
                case "--service-jar" -> {
                    if (i + 1 < cliArgs.length) userServiceJar = Path.of(cliArgs[++i]);
                }
                case "--data-dir" -> {
                    if (i + 1 < cliArgs.length) userDataDir = Path.of(cliArgs[++i]);
                }
                default -> { /* ignored; not passed through */ }
            }
        }
        port = userPort > 0 ? userPort : pickFreePort();
        serviceJar = userServiceJar != null ? userServiceJar : locateServiceJar();
        dataDir = userDataDir != null ? userDataDir : defaultDataDir();
        Files.createDirectories(dataDir);
        log.info("Desktop wrapper: port={}, serviceJar={}, dataDir={}",
                port, serviceJar, dataDir);

        spawnService();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Little Log Peep");
        WebView web = new WebView();
        Label loading = new Label("Starting log-search service…");
        loading.setTextFill(Color.web("#e6e6ea"));
        StackPane root = new StackPane(web, loading);
        root.setStyle("-fx-background-color: #14141a;");
        // Hide until server up so user doesn't see "Connection refused" flash.
        web.setVisible(false);

        Scene scene = new Scene(root, 1400, 900);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(e -> shutdownService());
        stage.show();

        Thread waiter = new Thread(() -> {
            waitForPort(port, 60_000);
            String url = "http://127.0.0.1:" + port + "/";
            Platform.runLater(() -> {
                web.getEngine().load(url);
                web.getEngine().getLoadWorker().stateProperty().addListener((obs, old, s) -> {
                    if (s == Worker.State.SUCCEEDED) {
                        loading.setVisible(false);
                        web.setVisible(true);
                    } else if (s == Worker.State.FAILED) {
                        loading.setText("Failed to load " + url + " — check logs at " + logFile());
                    }
                });
            });
        }, "lls-port-waiter");
        waiter.setDaemon(true);
        waiter.start();
    }

    @Override
    public void stop() {
        shutdownService();
    }

    /**
     * Locate the child JVM's {@code java} binary. Under jpackage the
     * bundle ships its own runtime under {@code app/runtime/Contents/Home/bin/java}
     * on macOS; {@code java.home} points at that runtime dir.
     */
    private static Path javaBinary() {
        Path home = Path.of(System.getProperty("java.home"));
        Path bin = home.resolve("bin").resolve("java");
        if (Files.isExecutable(bin)) return bin;
        // Windows fallback (unlikely on this target but cheap to include).
        Path win = home.resolve("bin").resolve("java.exe");
        if (Files.isExecutable(win)) return win;
        return Path.of("java"); // last-ditch: rely on PATH
    }

    /**
     * Where the service JAR lives. jpackage stages the input dir
     * flat under {@code Contents/app/}, so both this wrapper's fat
     * JAR and the service JAR end up alongside each other. Falls
     * back to the dev-tree location for {@code make run}.
     */
    private static Path locateServiceJar() {
        // 1. jpackage layout: same dir as this wrapper JAR.
        try {
            Path selfJar = Path.of(DesktopMain.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path sibling = selfJar.getParent().resolve("log-search-service.jar");
            if (Files.exists(sibling)) return sibling;
        } catch (Exception ignored) { /* fall through */ }

        // 2. Dev tree: <repo>/log-search-service/target/log-search-service-1.0-SNAPSHOT.jar
        Path[] devCandidates = {
            Path.of("log-search-service/target/log-search-service-1.0-SNAPSHOT.jar"),
            Path.of("../log-search-service/target/log-search-service-1.0-SNAPSHOT.jar"),
        };
        for (Path c : devCandidates) {
            if (Files.exists(c)) return c.toAbsolutePath();
        }
        throw new IllegalStateException(
            "log-search-service.jar not found — pass --service-jar or run "
            + "`mvn package -pl log-search-service -am -DskipTests` first");
    }

    private static Path defaultDataDir() {
        String home = System.getProperty("user.home");
        return Path.of(home, "Library", "Application Support", "LittleLogPeep");
    }

    private static Path logFile() {
        return defaultDataDir().resolve("service.log");
    }

    private void spawnService() throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBinary().toString());
        cmd.add("-jar");
        cmd.add(serviceJar.toString());
        cmd.add("--server.port=" + port);
        // Pin all persistent state under our chosen data dir so a
        // jpackage'd .app never tries to write inside itself (state
        // defaults to ./state which lands wherever java was invoked)
        // and so every jpackage'd instance shares the same indices
        // regardless of cwd. Names match Spring @Value bindings in
        // ServiceConfiguration.
        cmd.add("--index.base-directory=" + dataDir.resolve("indices"));
        cmd.add("--state.directory=" + dataDir.resolve("state"));
        cmd.add("--spring.datasource.url=jdbc:h2:file:"
                + dataDir.resolve("database").resolve("logdb")
                + ";AUTO_SERVER=TRUE");

        Path logPath = dataDir.resolve("service.log");
        ProcessBuilder pb = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .redirectOutput(logPath.toFile());
        pb.directory(dataDir.toFile());
        serviceProcess = pb.start();

        // Cross-shutdown JVM hook: if the wrapper crashes without
        // stop() being called, still tear down the child.
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownService,
                "lls-service-shutdown"));
        log.info("Spawned service: pid={} log={}", serviceProcess.pid(), logPath);
    }

    private synchronized void shutdownService() {
        if (serviceProcess == null) return;
        Process p = serviceProcess;
        serviceProcess = null;
        if (!p.isAlive()) return;
        p.destroy();
        try {
            if (!p.waitFor(java.time.Duration.ofSeconds(5).getSeconds(),
                           java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
        }
        log.info("Service shut down");
    }

    private static int pickFreePort() {
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0, 0,
                java.net.InetAddress.getByName("127.0.0.1"))) {
            return ss.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("pick free port", e);
        }
    }

    private static void waitForPort(int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", port), 300);
                return;
            } catch (IOException ignored) {
                try { Thread.sleep(200); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("Port {} never opened within {}ms", port, timeoutMs);
    }
}
