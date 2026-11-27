// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Serves the local website-style frontend assets strictly from bundled classpath
 * resources (or dev fallback) with path-traversal protection and security headers.
 */
public class StaticAssetHandler implements HttpHandler {

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry(".html", "text/html; charset=utf-8"),
            Map.entry(".css", "text/css; charset=utf-8"),
            Map.entry(".js", "application/javascript; charset=utf-8"),
            Map.entry(".json", "application/json; charset=utf-8"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".png", "image/png"),
            Map.entry(".ico", "image/x-icon"),
            Map.entry(".woff2", "font/woff2"),
            Map.entry(".woff", "font/woff"),
            Map.entry(".txt", "text/plain; charset=utf-8")
    );

    private final String sessionToken;
    private final Path devWebRoot;

    public StaticAssetHandler(String sessionToken) {
        this.sessionToken = sessionToken;
        Path devCandidate = Paths.get("src", "main", "resources", "web").toAbsolutePath().normalize();
        this.devWebRoot = Files.isDirectory(devCandidate) ? devCandidate : null;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        if (rawPath == null || rawPath.isEmpty() || "/".equals(rawPath)) {
            rawPath = "/index.html";
        }

        if (rawPath.contains("..") || rawPath.contains("\\")) {
            HttpContextHelper.sendError(exchange, 403, "Forbidden path");
            return;
        }

        String relPath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        byte[] content = loadAsset(relPath);
        if (content == null) {
            HttpContextHelper.sendError(exchange, 404, "Asset not found: " + rawPath);
            return;
        }

        if ("index.html".equals(relPath)) {
            String html = new String(content, StandardCharsets.UTF_8)
                    .replace("<!--PPMS_TOKEN-->", "<meta name=\"ppms-token\" content=\"" + sessionToken + "\">");
            content = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
        } else {
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        }

        String mime = resolveMime(relPath);
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "same-origin");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none';");

        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private byte[] loadAsset(String relPath) throws IOException {
        if (devWebRoot != null) {
            Path diskFile = devWebRoot.resolve(relPath).normalize();
            if (diskFile.startsWith(devWebRoot) && Files.isRegularFile(diskFile)) {
                return Files.readAllBytes(diskFile);
            }
        }
        String cpPath = "/web/" + relPath;
        try (InputStream in = StaticAssetHandler.class.getResourceAsStream(cpPath)) {
            if (in != null) {
                return in.readAllBytes();
            }
        }
        return null;
    }

    private String resolveMime(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            String ext = fileName.substring(dot).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
