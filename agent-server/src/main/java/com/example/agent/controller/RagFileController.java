package com.example.agent.controller;

import com.example.agent.service.RagService;
import org.springframework.http.ResponseEntity;
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

    private final RagService ragService;
    private static final String RAG_DIR = "src/main/resources/data/rag";

    public RagFileController(RagService ragService) {
        this.ragService = ragService;
        // Ensure directory exists
        File dir = new File(RAG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        try {
            Path path = Paths.get(RAG_DIR, file.getOriginalFilename());
            file.transferTo(path);
            ragService.reindexAll(); // Simple reindex for now
            return ResponseEntity.ok("File uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
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
            Path path = Paths.get(RAG_DIR, title);
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
            ragService.reindexAll();
            return ResponseEntity.ok("Text saved successfully as " + title);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to save text: " + e.getMessage());
        }
    }

    @GetMapping("/files")
    public List<Map<String, Object>> listFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        File dir = new File(RAG_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] fileList = dir.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    if (f.isFile() && !f.getName().startsWith(".")) {
                        files.add(Map.of(
                                "name", f.getName(),
                                "size", f.length(),
                                "lastModified", f.lastModified()));
                    }
                }
            }
        }
        return files;
    }

    @DeleteMapping("/files/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        try {
            Path path = Paths.get(RAG_DIR, filename);
            if (Files.deleteIfExists(path)) {
                ragService.reindexAll();
                return ResponseEntity.ok("File deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to delete file: " + e.getMessage());
        }
    }
}
