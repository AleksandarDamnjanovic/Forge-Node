package com.aleksandar_damnjanovic.forge_node;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * REST endpoints for uploading and downloading TLS certificates/keys (SHARED/)
 * and Forge Node scripts (AAU/script/).
 */
@RestController
@RequestMapping("/api/files")
public class FileTransferController {

    private static final Set<String> ALLOWED_CERT_EXTENSIONS = Set.of(
            ".crt", ".key", ".pem", ".cer", ".ca", ".csr"
    );
    private static final Set<String> ALLOWED_SCRIPT_EXTENSIONS = Set.of(
            ".fn", ".txt"
    );

    private Path resolveProjectRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        for (int i = 0; i < 6 && cwd != null; i++) {
            if (Files.isDirectory(cwd.resolve("SHARED")) || Files.isDirectory(cwd.resolve("AAU"))) {
                return cwd;
            }
            cwd = cwd.getParent();
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    private Path getSharedDir() throws IOException {
        Path dir = resolveProjectRoot().resolve("SHARED");
        Files.createDirectories(dir);
        return dir;
    }

    private Path getScriptsDir() throws IOException {
        Path dir = resolveProjectRoot().resolve("AAU").resolve("script");
        Files.createDirectories(dir);
        return dir;
    }

    private static String sanitizeFileName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return null;
        }
        String name = Path.of(rawName).getFileName().toString().trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")
                || name.contains("/") || name.contains("\\")) {
            return null;
        }
        return name;
    }

    private static boolean hasAllowedExtension(String fileName, Set<String> allowedExtensions) {
        String lower = fileName.toLowerCase();
        for (String ext : allowedExtensions) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private List<String> listFiles(Path dir, Set<String> allowedExtensions) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<String> files = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> hasAllowedExtension(name, allowedExtensions))
                    .sorted(Comparator.naturalOrder())
                    .forEach(files::add);
        }
        return files;
    }

    private ResponseEntity<byte[]> downloadSingleFile(Path dir, String rawName, Set<String> allowedExtensions)
            throws IOException {
        String fileName = sanitizeFileName(rawName);
        if (fileName == null || !hasAllowedExtension(fileName, allowedExtensions)) {
            return ResponseEntity.badRequest().build();
        }
        Path target = dir.resolve(fileName).normalize();
        if (!target.startsWith(dir) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = Files.readAllBytes(target);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    private ResponseEntity<byte[]> downloadZip(Path dir, String zipFileName, Set<String> allowedExtensions)
            throws IOException {
        List<String> fileNames = listFiles(dir, allowedExtensions);
        if (fileNames.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String name : fileNames) {
                Path file = dir.resolve(name).normalize();
                if (file.startsWith(dir) && Files.isRegularFile(file)) {
                    zos.putNextEntry(new ZipEntry(name));
                    zos.write(Files.readAllBytes(file));
                    zos.closeEntry();
                }
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(baos.toByteArray());
    }

    private ResponseEntity<Map<String, Object>> uploadFiles(
            Path dir, MultipartFile[] files, Set<String> allowedExtensions) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "No files provided"));
        }
        List<String> saved = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }
                String fileName = sanitizeFileName(file.getOriginalFilename());
                if (fileName == null || !hasAllowedExtension(fileName, allowedExtensions)) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "ok", false,
                            "error", "Unsupported file type: " + file.getOriginalFilename()
                    ));
                }
                Path target = dir.resolve(fileName).normalize();
                if (!target.startsWith(dir)) {
                    return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid file path"));
                }
                try (var in = file.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                saved.add(fileName);
            }
            if (saved.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Uploaded files were empty"));
            }
            return ResponseEntity.ok(Map.of("ok", true, "files", saved));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    // --- Certificates & Keys (SHARED/) ---

    @GetMapping("/certs/list")
    public ResponseEntity<Map<String, Object>> listCerts() throws IOException {
        return ResponseEntity.ok(Map.of("ok", true, "files", listFiles(getSharedDir(), ALLOWED_CERT_EXTENSIONS)));
    }

    @GetMapping("/certs/download")
    public ResponseEntity<byte[]> downloadCert(@RequestParam("name") String name) throws IOException {
        return downloadSingleFile(getSharedDir(), name, ALLOWED_CERT_EXTENSIONS);
    }

    @GetMapping("/certs/download-all")
    public ResponseEntity<byte[]> downloadAllCerts() throws IOException {
        return downloadZip(getSharedDir(), "forge-node-certs.zip", ALLOWED_CERT_EXTENSIONS);
    }

    @PostMapping("/certs/upload")
    public ResponseEntity<Map<String, Object>> uploadCerts(@RequestParam("files") MultipartFile[] files)
            throws IOException {
        return uploadFiles(getSharedDir(), files, ALLOWED_CERT_EXTENSIONS);
    }

    // --- Scripts (AAU/script/) ---

    @GetMapping("/scripts/list")
    public ResponseEntity<Map<String, Object>> listScripts() throws IOException {
        return ResponseEntity.ok(Map.of("ok", true, "files", listFiles(getScriptsDir(), ALLOWED_SCRIPT_EXTENSIONS)));
    }

    @GetMapping("/scripts/download")
    public ResponseEntity<byte[]> downloadScript(@RequestParam("name") String name) throws IOException {
        return downloadSingleFile(getScriptsDir(), name, ALLOWED_SCRIPT_EXTENSIONS);
    }

    @GetMapping("/scripts/download-all")
    public ResponseEntity<byte[]> downloadAllScripts() throws IOException {
        return downloadZip(getScriptsDir(), "forge-node-scripts.zip", ALLOWED_SCRIPT_EXTENSIONS);
    }

    @PostMapping("/scripts/upload")
    public ResponseEntity<Map<String, Object>> uploadScripts(@RequestParam("files") MultipartFile[] files)
            throws IOException {
        return uploadFiles(getScriptsDir(), files, ALLOWED_SCRIPT_EXTENSIONS);
    }

    private static final Pattern FN_HEADER_PATTERN = Pattern.compile(
            "^\\s*([0-9]+)\\s*\\(\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\\[\\s*(MASTER|SLAVE|SHUNT|BRIDGE)\\s*\\]\\s*\\)\\s*$"
    );

    private static boolean hasTagPair(String block, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int b = block.indexOf(open);
        int e = block.indexOf(close);
        return b != -1 && e != -1 && e > b;
    }

    private static String validateFnScriptContent(String content) {
        if (content == null || content.isBlank()) {
            return "Invalid .fn script: content cannot be empty";
        }
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
                if (depth > 1) {
                    return "Invalid .fn script: unexpected nested '{'";
                }
            } else if (ch == '}') {
                depth--;
                if (depth < 0) {
                    return "Invalid .fn script: unexpected '}' without matching '{'";
                }
            }
        }
        if (inString) {
            return "Invalid .fn script: unterminated string literal";
        }
        if (depth != 0) {
            return "Invalid .fn script: unclosed node block (missing '}')";
        }

        String[] units = content.split("\\}", -1);
        int nodeCount = 0;
        Set<Integer> seenIds = new HashSet<>();
        for (String unit : units) {
            if (unit.isBlank()) {
                continue;
            }
            int openBrace = unit.indexOf('{');
            if (openBrace == -1) {
                return "Invalid .fn script: unexpected content outside node block";
            }
            String header = unit.substring(0, openBrace);
            Matcher m = FN_HEADER_PATTERN.matcher(header);
            if (!m.matches()) {
                return "Invalid .fn script: malformed node header";
            }
            int nodeId = Integer.parseInt(m.group(1));
            if (!seenIds.add(nodeId)) {
                return "Invalid .fn script: duplicate node index " + nodeId;
            }
            String role = m.group(4);
            if (("SHUNT".equals(role) || "BRIDGE".equals(role) || "SLAVE".equals(role))
                    && !hasTagPair(unit, "meta")) {
                return "Invalid .fn script: " + role + " node #" + nodeId + " is missing <meta>...</meta>";
            }
            for (String tag : List.of("variables", "sensors", "elements", "programs")) {
                if (!hasTagPair(unit, tag)) {
                    return "Invalid .fn script: node #" + nodeId + " is missing <" + tag + ">...</" + tag + ">";
                }
            }
            nodeCount++;
        }
        if (nodeCount == 0) {
            return "Invalid .fn script: at least one node block is required";
        }
        return null;
    }

    @PostMapping("/scripts/save")
    public ResponseEntity<Map<String, Object>> saveScript(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Request body is required"));
        }
        String rawName = body.get("fileName") == null ? "" : String.valueOf(body.get("fileName"));
        String fileName = sanitizeFileName(rawName);
        if (fileName == null || !fileName.toLowerCase().endsWith(".fn")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", "Invalid script filename (must end with .fn)"
            ));
        }
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content"));
        String validationError = validateFnScriptContent(content);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", validationError));
        }
        try {
            Path dir = getScriptsDir();
            Path target = dir.resolve(fileName).normalize();
            if (!target.startsWith(dir)) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid file path"));
            }
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return ResponseEntity.ok(Map.of("ok", true, "fileName", fileName, "path", "AAU/script/" + fileName));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
}
