import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class SketchXServer {

    static final int PORT = 8080;
    static final Path ROOT = Paths.get("").toAbsolutePath();
    static final Path WEB = ROOT.resolve("web");
    static final Path MODELS = WEB.resolve("models");
    static final Path GENERATED = WEB.resolve("generated");

    public static void main(String[] args) throws Exception {

        Files.createDirectories(WEB);
        Files.createDirectories(MODELS);
        Files.createDirectories(GENERATED);

        HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0);

        server.createContext("/", SketchXServer::staticFiles);
        server.createContext("/api/health", SketchXServer::health);
        server.createContext("/api/create-case", SketchXServer::createCase);
        server.createContext("/api/extract-features", SketchXServer::extractFeatures);
        server.createContext("/api/interview", SketchXServer::interview);
        server.createContext("/api/generate", SketchXServer::generate);
        server.createContext("/api/refine", SketchXServer::refine);
        server.createContext("/api/collision", SketchXServer::collision);
        server.createContext("/api/provenance", SketchXServer::provenance);
        server.createContext("/api/memory", SketchXServer::memory);
        server.createContext("/api/reconstruct-3d", SketchXServer::reconstruct3D);
        server.createContext("/api/audit", SketchXServer::audit);

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println();
        System.out.println("==============================================");
        System.out.println("       SKETCH-X AI FORENSIC PLATFORM");
        System.out.println("==============================================");
        System.out.println("Open: http://localhost:" + PORT);
        System.out.println("AI extraction : LOCAL DEMO MODE");
        System.out.println("3D engine     : LOCAL GLB");
        System.out.println("OpenAI        : NOT REQUIRED");
        System.out.println("Meshy         : NOT REQUIRED");
        System.out.println("3D model      : /models/sketchx_latest.glb");
        System.out.println("==============================================");
    }

    static void staticFiles(HttpExchange ex) throws IOException {

        String path = ex.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        Path file = WEB.resolve(path.substring(1)).normalize();

        if (!file.startsWith(WEB)) {
            send(ex, 403, "Forbidden", "text/plain");
            return;
        }

        if (!Files.exists(file) || Files.isDirectory(file)) {
            send(ex, 404, "Not Found", "text/plain");
            return;
        }

        byte[] data = Files.readAllBytes(file);

        String type = contentType(file.toString());

        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, data.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    static String contentType(String file) {

        String f = file.toLowerCase();

        if (f.endsWith(".html")) return "text/html; charset=UTF-8";
        if (f.endsWith(".css")) return "text/css; charset=UTF-8";
        if (f.endsWith(".js")) return "application/javascript";
        if (f.endsWith(".json")) return "application/json";
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".svg")) return "image/svg+xml";
        if (f.endsWith(".glb")) return "model/gltf-binary";
        if (f.endsWith(".gltf")) return "model/gltf+json";

        return "application/octet-stream";
    }

    static void health(HttpExchange ex) throws IOException {

        String json = """
        {
          "success":true,
          "system":"SKETCH-X",
          "mode":"LOCAL DEMO",
          "openai":false,
          "meshy":false,
          "model3d":"/models/sketchx_latest.glb"
        }
        """;

        send(ex, 200, json, "application/json");
    }

    static void createCase(HttpExchange ex) throws IOException {

        String caseId = "SKX-" +
                String.format("%04d", new Random().nextInt(9999));

        String json = """
        {
          "success":true,
          "caseId":"%s",
          "status":"ACTIVE",
          "location":"Bengaluru",
          "type":"Robbery Case"
        }
        """.formatted(caseId);

        send(ex, 200, json, "application/json");
    }

    static void extractFeatures(HttpExchange ex) throws IOException {

        String body = read(ex);

        String text = body.toLowerCase();

        String age = findAge(text);
        String face = findFace(text);
        String eyebrows = findValue(
                text,
                new String[]{"thick", "thin", "bushy", "heavy"},
                "Not specified"
        );

        String nose = findValue(
                text,
                new String[]{"broad", "wide", "narrow", "small", "large"},
                "Not specified"
        );

        String hair = "Not specified";

        if (text.contains("short")) hair = "Short";
        if (text.contains("long")) hair = "Long";

        if (text.contains("black hair")) hair = "Short / Black";
        else if (text.contains("black")) hair = "Black";

        String beard = "Not specified";

        if (text.contains("no beard") ||
                text.contains("clean shaven") ||
                text.contains("clean-shaven")) {
            beard = "None";
        } else if (text.contains("beard")) {
            beard = "Present";
        }

        String json = """
        {
          "success":true,
          "mode":"LOCAL AI FEATURE EXTRACTION",
          "features":{
            "age":"%s",
            "faceShape":"%s",
            "eyebrows":"%s",
            "nose":"%s",
            "hair":"%s",
            "beard":"%s"
          }
        }
        """.formatted(
                esc(age),
                esc(face),
                esc(eyebrows),
                esc(nose),
                esc(hair),
                esc(beard)
        );

        send(ex, 200, json, "application/json");
    }

    static String findAge(String text) {

        for (String word : text.split("[^0-9]+")) {

            if (!word.isBlank()) {

                try {
                    int n = Integer.parseInt(word);

                    if (n >= 10 && n <= 100) {
                        return n + " years";
                    }

                } catch (Exception ignored) {}
            }
        }

        return "Unknown";
    }

    static String findFace(String text) {

        String[] shapes = {
                "Oval",
                "Round",
                "Long",
                "Square",
                "Heart",
                "Diamond"
        };

        for (String s : shapes) {
            if (text.contains(s.toLowerCase())) {
                return s;
            }
        }

        return "Not specified";
    }

    static String findValue(
            String text,
            String[] values,
            String fallback) {

        for (String v : values) {

            if (text.contains(v)) {

                String result =
                        v.substring(0, 1).toUpperCase()
                        + v.substring(1);

                return result;
            }
        }

        return fallback;
    }

    static void interview(HttpExchange ex) throws IOException {

        String json = """
        {
          "success":true,
          "questions":[
            "Do you remember the eye shape?",
            "Was the jaw broad, narrow or average?",
            "Was the face more oval, round or long?",
            "Do you remember any distinctive facial feature?"
          ],
          "memoryDNA":{
            "stability":"72%",
            "state":"PARTIALLY STABLE"
          }
        }
        """;

        send(ex, 200, json, "application/json");
    }

    static void generate(HttpExchange ex) throws IOException {

        String json = """
        {
          "success":true,
          "version":"V1",
          "source":"LOCAL SKETCH-X ENGINE",
          "image":"/generated/forensic-composite-demo.svg",
          "model3d":"/models/sketchx_latest.glb",
          "message":"Composite generated successfully"
        }
        """;

        createDemoSVG();

        send(ex, 200, json, "application/json");
    }

    static void createDemoSVG() throws IOException {

        Path file = GENERATED.resolve(
                "forensic-composite-demo.svg");

        String svg = """
        <svg xmlns="http://www.w3.org/2000/svg"
             width="700" height="850"
             viewBox="0 0 700 850">

          <rect width="700" height="850" fill="#071426"/>

          <text x="350" y="45"
                text-anchor="middle"
                fill="#58e6ff"
                font-size="24"
                font-family="Arial">
            SKETCH-X FORENSIC COMPOSITE
          </text>

          <ellipse cx="350" cy="420"
                   rx="210" ry="280"
                   fill="#c9b39a"
                   stroke="#69e6ff"
                   stroke-width="4"/>

          <path d="M200 350 Q270 300 315 340"
                fill="none"
                stroke="#171717"
                stroke-width="18"/>

          <path d="M385 340 Q440 300 500 350"
                fill="none"
                stroke="#171717"
                stroke-width="18"/>

          <ellipse cx="265" cy="400"
                   rx="32" ry="18"
                   fill="#111"/>

          <ellipse cx="435" cy="400"
                   rx="32" ry="18"
                   fill="#111"/>

          <path d="M350 400
                   Q325 470 350 500
                   Q375 470 350 400"
                fill="#a0836b"/>

          <path d="M285 560
                   Q350 600 415 560"
                fill="none"
                stroke="#642e2e"
                stroke-width="12"/>

          <path d="M235 250
                   Q350 180 465 250"
                fill="#111"
                stroke="#111"
                stroke-width="20"/>

          <text x="350" y="790"
                text-anchor="middle"
                fill="#a8c8d8"
                font-size="17"
                font-family="Arial">
            AI-assisted investigative composite
          </text>

        </svg>
        """;

        Files.writeString(file, svg, StandardCharsets.UTF_8);
    }

    static void refine(HttpExchange ex) throws IOException {

        String json = """
        {
          "success":true,
          "version":"V2",
          "changes":[
            "Jaw refined",
            "Eye shape refined",
            "Feature confidence recalculated"
          ],
          "model3d":"/models/sketchx_latest.glb"
        }
        """;

        send(ex, 200, json, "application/json");
    }

    static void collision(HttpExchange ex) throws IOException {

        String json = """
        {
          "success":true,
          "collisionDetected":true,
          "severity":"MEDIUM",
          "original":"No beard",
          "laterMemory":"Red beard",
          "message":"Witness memory changed between recollections. Original memory remains locked."
        }
        """;

        send(ex, 200, json, "application/json");
    }

    static void provenance(HttpExchange ex) throws IOException {

        String json = """
        {
          "success":true,
          "features":[
            {
              "feature":"Face Shape",
              "value":"Oval",
              "source":"WITNESS",
              "confidence":"HIGH"
            },
            {
              "feature":"Eyebrows",
              "value":"Thick",
              "source":"WITNESS",
              "confidence":"HIGH"
            },
            {
              "feature":"Nose",
              "value":"Broad",
              "source":"WITNESS",
              "confidence":"MEDIUM"
            },
            {
              "feature":"3D Surface",
              "value":"Reconstructed",
              "source":"AI-ASSISTED",
              "confidence":"MODEL OUTPUT"
            }
          ]
        }
        """;

        send(ex, 200, json, "application/json");
    }

    static void memory(HttpExchange ex) throws IOException {

        String json = """
        {
          "success":true,
          "locked":true,
          "version":"ORIGINAL",
          "status":"PRESERVED",
          "message":"Original witness memory preserved and cannot be overwritten."
        }
        """;

        send(ex, 200, json, "application/json");
    }

    static void reconstruct3D(HttpExchange ex) throws IOException {

        String json = """
        {
          "success":true,
          "status":"READY",
          "engine":"LOCAL GLB",
          "model":"/models/sketchx_latest.glb",
          "format":"GLB",
          "message":"Real 3D forensic reconstruction loaded."
        }
        """;

        send(ex, 200, json, "application/json");
    }

    static void audit(HttpExchange ex) throws IOException {

        String json = """
        {
          "success":true,
          "auditLog":[
            {
              "user":"Investigator",
              "action":"Case Created",
              "time":"2026-10-29 09:00"
            },
            {
              "user":"Investigator",
              "action":"Original Memory Locked",
              "time":"2026-10-29 09:02"
            },
            {
              "user":"AI Engine",
              "action":"Composite Generated",
              "time":"2026-10-29 09:04"
            },
            {
              "user":"Investigator",
              "action":"3D Reconstruction Viewed",
              "time":"2026-10-29 09:06"
            }
          ]
        }
        """;

        send(ex, 200, json, "application/json");
    }

    static String read(HttpExchange ex) throws IOException {

        return new String(
                ex.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    static String esc(String s) {

        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    static void send(
            HttpExchange ex,
            int code,
            String body,
            String contentType) throws IOException {

        byte[] bytes =
                body.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders()
                .set("Content-Type", contentType);

        ex.getResponseHeaders()
                .set("Access-Control-Allow-Origin", "*");

        ex.sendResponseHeaders(code, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}