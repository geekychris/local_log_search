/*
 * MIT License — same terms as the parent project.
 */
package com.locallogsearch.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX-based native desktop wrapper for Local Log Search ("Little Log Peep").
 *
 * <p>Layout: a compact top toolbar (Back / Forward / Reload / Home / URL) sits
 * above a WebView pointed at the embedded Spring Boot service on 127.0.0.1.
 *
 * <p>The toolbar exists because the WebView itself has no chrome — before it
 * was added, clicking a link that returned 404 (or any broken navigation)
 * left the user stranded with no way back. Now Back / Home are always
 * available regardless of state, plus the URL field lets power users
 * navigate directly.
 *
 * <p>Navigation policy:
 *
 * <ul>
 *   <li>URLs matching the embedded loopback host load in-place.</li>
 *   <li>Any other origin (docs links, GitHub, anything the app might
 *       embed) is handed off to the system default browser via
 *       {@link Desktop}. This prevents users from accidentally
 *       navigating the WebView OUT of the app — a common way to get
 *       stuck when there's no home button.</li>
 *   <li>Failed loads (network error, 404 Whitelabel Error Page, etc.)
 *       show a recoverable status banner at the bottom with the
 *       message and a hint to press Home.</li>
 * </ul>
 */
public class DesktopMain extends Application {

    private static final Logger log = LoggerFactory.getLogger(DesktopMain.class);

    private static String[] cliArgs = new String[0];

    private int port;
    private Path serviceJar;
    private Path dataDir;
    private Process serviceProcess;

    // Toolbar controls — held so the load-state listener can enable /
    // disable them from a single place.
    private Button backBtn;
    private Button fwdBtn;
    private Button reloadBtn;
    private Button homeBtn;
    private TextField urlField;
    private Label statusLabel;

    // Cached home URL for the Home button + external-link check.
    private String homeUrl;

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
        homeUrl = "http://127.0.0.1:" + port + "/";
        log.info("Desktop wrapper: port={}, serviceJar={}, dataDir={}",
                port, serviceJar, dataDir);

        spawnService();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Little Log Peep");

        WebView web = new WebView();
        // Give the underlying WebEngine a browser-shaped user agent —
        // Vaadin apps sometimes gate features on the UA string, and the
        // default JavaFX WebView UA can trigger the "unsupported
        // browser" screen. Cheap to include, hard to attribute failures
        // to when omitted.
        web.getEngine().setUserAgent(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 "
            + "(KHTML, like Gecko) Version/17.0 Safari/605.1.15 LittleLogPeep/1.0");

        HBox toolbar = buildToolbar(web);
        StackPane center = new StackPane(web);
        center.setStyle("-fx-background-color: #14141a;");

        statusLabel = new Label("");
        statusLabel.setPadding(new Insets(2, 8, 2, 8));
        statusLabel.setTextFill(Color.web("#ffcf7a"));
        statusLabel.setStyle("-fx-background-color: #2a1f10;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        BorderPane root = new BorderPane();
        root.setTop(new VBox(toolbar, statusLabel));
        root.setCenter(center);
        root.setStyle("-fx-background-color: #14141a;");

        // Hide the WebView until the server is up so the user doesn't
        // see "Connection refused" chrome flash.
        Label loading = new Label("Starting log-search service…");
        loading.setTextFill(Color.web("#e6e6ea"));
        StackPane loadingPane = new StackPane(loading);
        loadingPane.setStyle("-fx-background-color: #14141a;");
        center.getChildren().add(loadingPane);
        web.setVisible(false);

        wireLoadListener(web, loading, loadingPane);
        wireExternalLinkRedirect(web);

        Scene scene = new Scene(root, 1400, 900);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(e -> shutdownService());
        stage.show();

        Thread waiter = new Thread(() -> {
            waitForPort(port, 60_000);
            Platform.runLater(() -> web.getEngine().load(homeUrl));
        }, "lls-port-waiter");
        waiter.setDaemon(true);
        waiter.start();
    }

    /**
     * Build the top toolbar. Kept minimal: four navigation icons + a
     * URL field. Each button has a tooltip so hovering explains the
     * verb — the icons alone might be ambiguous.
     */
    private HBox buildToolbar(WebView web) {
        backBtn = iconButton("←", "Go back one page in history");
        backBtn.setOnAction(e -> {
            if (web.getEngine().getHistory().getCurrentIndex() > 0) {
                web.getEngine().getHistory().go(-1);
            }
        });
        fwdBtn = iconButton("→", "Go forward one page in history");
        fwdBtn.setOnAction(e -> {
            var history = web.getEngine().getHistory();
            if (history.getCurrentIndex() < history.getEntries().size() - 1) {
                history.go(1);
            }
        });
        reloadBtn = iconButton("⟳", "Reload the current page");
        reloadBtn.setOnAction(e -> web.getEngine().reload());
        homeBtn = iconButton("⌂", "Return to the app home page (recover from any dead-end)");
        homeBtn.setOnAction(e -> web.getEngine().load(homeUrl));

        urlField = new TextField();
        urlField.setPromptText("URL (Enter to load)");
        urlField.setPrefColumnCount(50);
        urlField.setStyle(
            "-fx-background-color: #22222a;"
            + "-fx-text-fill: #e6e6ea;"
            + "-fx-prompt-text-fill: #6a6a72;");
        urlField.setOnAction(e -> {
            String u = urlField.getText().trim();
            if (u.isEmpty()) return;
            if (!u.contains("://")) u = "http://" + u;
            web.getEngine().load(u);
        });
        HBox.setHgrow(urlField, Priority.ALWAYS);

        HBox bar = new HBox(4, backBtn, fwdBtn, reloadBtn, homeBtn, urlField);
        bar.setPadding(new Insets(4, 8, 4, 8));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #1a1a22; -fx-border-color: #2a2a32; -fx-border-width: 0 0 1 0;");

        // Refresh button-enabled state as history changes.
        web.getEngine().getHistory().currentIndexProperty().addListener((obs, o, n) -> refreshNavButtons(web));
        return bar;
    }

    private Button iconButton(String glyph, String tip) {
        Button b = new Button(glyph);
        b.setTooltip(new Tooltip(tip));
        b.setFocusTraversable(false);
        b.setStyle(
            "-fx-background-color: #22222a;"
            + "-fx-text-fill: #e6e6ea;"
            + "-fx-font-size: 14px;"
            + "-fx-min-width: 28;"
            + "-fx-padding: 2 8 2 8;");
        return b;
    }

    private void refreshNavButtons(WebView web) {
        var history = web.getEngine().getHistory();
        int idx = history.getCurrentIndex();
        int total = history.getEntries().size();
        backBtn.setDisable(idx <= 0);
        fwdBtn.setDisable(idx >= total - 1);
    }

    /**
     * Load-worker listener: updates the URL field, toggles the loading
     * splash, and — on FAILED — shows a recoverable banner instead of
     * leaving the user staring at a broken page.
     */
    private void wireLoadListener(WebView web, Label loading, StackPane loadingPane) {
        var engine = web.getEngine();
        // Keep the URL field in sync when navigation happens via link
        // clicks (not just the field).
        engine.locationProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isBlank()) urlField.setText(n);
        });
        engine.getLoadWorker().stateProperty().addListener(
            (ChangeListener<Worker.State>) (obs, old, s) -> {
                switch (s) {
                    case SCHEDULED, RUNNING -> {
                        hideStatus();
                    }
                    case SUCCEEDED -> {
                        loading.setVisible(false);
                        loadingPane.setVisible(false);
                        web.setVisible(true);
                        refreshNavButtons(web);
                        maybeShowErrorBanner(engine);
                    }
                    case FAILED -> {
                        loading.setVisible(false);
                        loadingPane.setVisible(false);
                        web.setVisible(true);
                        refreshNavButtons(web);
                        showStatus("Failed to load. Press ⌂ Home to return to the app root, or "
                                + "check " + logFile() + " if the server crashed.");
                    }
                    case CANCELLED -> {
                        // Expected when we cancel a link click that
                        // pointed at an external origin — nothing to do.
                    }
                    default -> { /* READY / SCHEDULED — no-op */ }
                }
            });
    }

    /**
     * Intercept location changes that leave the loopback host. A user
     * who clicks a docs link ("go to github.com/geekychris/...") would
     * otherwise navigate the WebView OUT of the app UI with no way
     * back except history — instead we cancel the load and hand the
     * URL to the OS default browser.
     */
    private void wireExternalLinkRedirect(WebView web) {
        var engine = web.getEngine();
        engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc == null || newLoc.isBlank()) return;
            if (isSameHost(newLoc)) return;
            // Only redirect real http/https — file: and about: are internal.
            if (!newLoc.startsWith("http://") && !newLoc.startsWith("https://")) return;
            engine.getLoadWorker().cancel();
            openInSystemBrowser(newLoc);
            // Bring back the previous page so the WebView doesn't stay
            // half-loaded on the external URL.
            Platform.runLater(() -> {
                if (oldLoc != null && !oldLoc.isBlank()) {
                    engine.load(oldLoc);
                } else {
                    engine.load(homeUrl);
                }
            });
        });
    }

    private boolean isSameHost(String url) {
        try {
            URI u = URI.create(url);
            URI home = URI.create(homeUrl);
            return home.getHost().equals(u.getHost()) && home.getPort() == u.getPort();
        } catch (IllegalArgumentException e) {
            return true; // parse fail → don't interfere
        }
    }

    private void openInSystemBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception e) {
            log.warn("Desktop.browse failed for {}: {}", url, e.toString());
        }
        // Fallback: shell out to `open` on macOS.
        try {
            new ProcessBuilder("open", url).start();
        } catch (IOException ignored) { /* nothing more we can do */ }
    }

    /**
     * Post-load error detector. Spring Boot serves error pages in two
     * shapes depending on content negotiation:
     *
     * <ul>
     *   <li>Browser navigation (Accept: text/html) → HTML Whitelabel
     *       Error Page with a distinctive title.</li>
     *   <li>AJAX / direct URL → JSON body
     *       {@code {"status":404,"error":"Not Found",...}} rendered as
     *       plain text in the WebView.</li>
     * </ul>
     *
     * We check both. The banner is advisory — the Home button is always
     * available regardless of what the detector concludes.
     */
    private void maybeShowErrorBanner(javafx.scene.web.WebEngine engine) {
        try {
            String title = engine.getTitle();
            if (title != null && (title.contains("Whitelabel Error Page")
                    || title.contains("404")
                    || title.startsWith("Error"))) {
                showStatus("Page not found — press ⌂ Home to return to the app root.");
                return;
            }
            // JSON error body: read the first ~120 chars of visible text
            // and look for a status/error field pair.
            Object result = engine.executeScript(
                "document && document.body ? document.body.innerText.substring(0, 200) : ''");
            if (result instanceof String body && !body.isBlank()) {
                String b = body.trim();
                boolean looksJsonError = b.startsWith("{")
                        && b.contains("\"status\":")
                        && (b.contains("\"error\":") || b.contains("\"message\":"));
                if (looksJsonError) {
                    // Try to extract the numeric status for a clearer hint.
                    String statusFragment = b;
                    int i = b.indexOf("\"status\":");
                    if (i >= 0) statusFragment = b.substring(i, Math.min(b.length(), i + 30));
                    showStatus("Server returned an error (" + statusFragment.replace("\"", "")
                            + ") — press ⌂ Home to return to the app root.");
                }
            }
        } catch (Exception e) {
            // JS execution can fail on about:blank etc.; ignore.
            log.debug("error-banner check failed: {}", e.toString());
        }
    }

    private void showStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        });
    }

    private void hideStatus() {
        Platform.runLater(() -> {
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        });
    }

    @Override
    public void stop() {
        shutdownService();
    }

    private static Path javaBinary() {
        Path home = Path.of(System.getProperty("java.home"));
        Path bin = home.resolve("bin").resolve("java");
        if (Files.isExecutable(bin)) return bin;
        Path win = home.resolve("bin").resolve("java.exe");
        if (Files.isExecutable(win)) return win;
        return Path.of("java");
    }

    private static Path locateServiceJar() {
        try {
            Path selfJar = Path.of(DesktopMain.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path sibling = selfJar.getParent().resolve("log-search-service.jar");
            if (Files.exists(sibling)) return sibling;
        } catch (Exception ignored) { /* fall through */ }

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
