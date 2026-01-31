package com.example.agent.controller;

import com.example.agent.config.AgentConfig;
import com.example.agent.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RagFileController {
    private final static Logger logger= LoggerFactory.getLogger(RagFileController.class);

    @Autowired
    private RagService ragService;

    @Autowired
    private AgentConfig agentConfig;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        try {
            // ensure directory exists
            Path dir = Paths.get(agentConfig.getRAGFilePath());
            Files.createDirectories(dir);

            // sanitize filename (basic) and avoid overwrites
            String original = file.getOriginalFilename();
            String safeName = original == null ? "file_" + UUID.randomUUID() : original.replaceAll("[\\\\/:*?\"<>|]","_");
            Path target = dir.resolve(safeName);
            if (Files.exists(target)) {
                String name = safeName;
                String ext = "";
                int idx = safeName.lastIndexOf('.');
                if (idx > 0) {
                    name = safeName.substring(0, idx);
                    ext = safeName.substring(idx);
                }
                safeName = name + "_" + UUID.randomUUID().toString().substring(0,8) + ext;
                target = dir.resolve(safeName);
            }

            file.transferTo(target);

            // trigger reindex in background to avoid blocking upload
            new Thread(() -> {
                try {
                    ragService.reindexAll();
                } catch (Exception e) {
                    logger.error("Reindex failed for uploaded file: {}", target, e);
                }
            }, "rag-reindex-thread").start();

            return ResponseEntity.ok("File uploaded successfully: " + safeName);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file: " + e.getMessage());
        }
    }

    @PostMapping("/text")
    public ResponseEntity<String> uploadText(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        String title = payload.get("title");
        if (content == null || content.isEmpty()) {
            return ResponseEntity.badRequest().body("Content is empty");
        }
        if (title == null || title.isEmpty()) {
            title = "text_" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (!title.endsWith(".txt")) {
            title += ".txt";
        }
        try {
            Path path = Paths.get(agentConfig.getRAGFilePath(), title);
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
            ragService.reindexAll();
            return ResponseEntity.ok("Text saved successfully as " + title);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            return ResponseEntity.internalServerError().body("Failed to save text: " + e.getMessage());
        }
    }

    @GetMapping("/files")
    public List<Map<String, Object>> listFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        File dir = new File(agentConfig.getRAGFilePath());
        if (dir.exists() && dir.isDirectory()) {
            File[] fileList = dir.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    if (f.isFile() && !f.getName().startsWith(".")) {
                        files.add(Map.of(
                                "name", f.getName(),
                                "size", f.length(),
                                "lastModified", f.lastModified(),
                                "downloadUrl", "/api/rag/files/" + f.getName() + "/download"));
                    }
                }
            }
        }
        return files;
    }

    @DeleteMapping("/files/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        try {
            Path path = Paths.get(agentConfig.getRAGFilePath(), filename);
            if (Files.deleteIfExists(path)) {
                ragService.reindexAll();
                return ResponseEntity.ok("File deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            return ResponseEntity.internalServerError().body("Failed to delete file: " + e.getMessage());
        }
    }

    @GetMapping("/files/{filename}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path path = Paths.get(agentConfig.getRAGFilePath(), filename);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(path.toUri());
            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("downloadFile error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
