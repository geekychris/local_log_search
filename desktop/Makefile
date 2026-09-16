# Desktop wrapper for Local Log Search — build + package targets.
#
# The Maven build produces target/local-log-search-desktop.jar (a
# runnable fat JAR launching JavaFX WebView that spawns the Spring
# Boot service as a child java process). This Makefile adds the
# jpackage step that bundles a JRE + JavaFX modules + the service
# JAR into a self-contained macOS .app — end users need nothing on
# PATH.
#
# Targets:
#   run             — launch the fat JAR (assumes service JAR is built)
#   app             — build the self-contained .app via jpackage
#   install         — copy the built .app to ~/Applications
#   clean

REPO_ROOT   := $(shell cd .. && pwd)
DESKTOP_JAR := target/local-log-search-desktop.jar
SERVICE_JAR := $(REPO_ROOT)/log-search-service/target/log-search-service-1.0-SNAPSHOT.jar
BUILD_DIR   := target/dist
BUNDLE      := $(BUILD_DIR)/Little Log Peep.app
APPS_DIR    := $(HOME)/Applications

# JavaFX modules the WebView + Controls stack needs. Passed to
# jpackage via --add-modules so jlink pulls the right JavaFX bits
# into the bundled runtime.
JFX_MODULES := javafx.controls,javafx.web,javafx.graphics,javafx.base,\
               javafx.media,jdk.zipfs,java.desktop,java.instrument,\
               java.logging,java.management,java.naming,java.net.http,\
               java.rmi,java.security.jgss,java.sql,java.xml,\
               jdk.crypto.ec,jdk.unsupported

.PHONY: run app install clean require-jars require-jpackage

# Quick dev launch. Assumes both JARs already built at repo root:
#   mvn package -pl log-search-service -am -DskipTests
#   mvn package -pl desktop -am -DskipTests
run: require-jars
	java -jar "$(DESKTOP_JAR)" --service-jar "$(SERVICE_JAR)"

# Self-contained .app bundle. Requires jpackage (bundled with any
# JDK >= 14). Stages BOTH jars into a clean input dir so jpackage
# doesn't recursively include its own dist/ output.
app: require-jars require-jpackage
	rm -rf "$(BUILD_DIR)" target/jp-input
	mkdir -p "$(BUILD_DIR)" target/jp-input
	cp "$(DESKTOP_JAR)" target/jp-input/
	cp "$(SERVICE_JAR)" target/jp-input/log-search-service.jar
	jpackage \
		--type app-image \
		--input target/jp-input \
		--main-jar local-log-search-desktop.jar \
		--name "Little Log Peep" \
		--app-version 1.0.0 \
		--dest "$(BUILD_DIR)" \
		--java-options "--enable-native-access=ALL-UNNAMED" \
		--icon ../AppIcon.icns 2>/dev/null || \
	jpackage \
		--type app-image \
		--input target/jp-input \
		--main-jar local-log-search-desktop.jar \
		--name "Little Log Peep" \
		--app-version 1.0.0 \
		--dest "$(BUILD_DIR)" \
		--java-options "--enable-native-access=ALL-UNNAMED"
	@echo "Built self-contained bundle at $(BUNDLE)"
	@du -sh "$(BUNDLE)" 2>/dev/null || true

install:
	@[ -d "$(BUNDLE)" ] || { echo "$(BUNDLE) missing — run 'make app' first"; exit 1; }
	@mkdir -p "$(APPS_DIR)"
	-@pkill -f "$(BUNDLE)/Contents/MacOS" 2>/dev/null; sleep 0.3
	@rm -rf "$(APPS_DIR)/Little Log Peep.app"
	@cp -R "$(BUNDLE)" "$(APPS_DIR)/"
	@echo "Installed to $(APPS_DIR)/Little Log Peep.app"

require-jars:
	@[ -f "$(DESKTOP_JAR)" ] || { \
		echo "$(DESKTOP_JAR) missing — run at repo root:"; \
		echo "  mvn package -pl desktop -am -DskipTests"; \
		exit 1; \
	}
	@[ -f "$(SERVICE_JAR)" ] || { \
		echo "$(SERVICE_JAR) missing — run at repo root:"; \
		echo "  mvn package -pl log-search-service -am -DskipTests"; \
		exit 1; \
	}

require-jpackage:
	@command -v jpackage >/dev/null 2>&1 || { \
		echo "jpackage not on PATH — install a JDK >= 14 (openjdk 21 is fine)"; \
		exit 1; \
	}

clean:
	rm -rf target/
