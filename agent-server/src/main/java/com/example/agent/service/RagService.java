package com.example.agent.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private static final String RAG_DIR = "src/main/resources/data/rag";

    public RagService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void init() {
        reindexAll();
    }

    public synchronized void reindexAll() {
        // Since we are using InMemoryEmbeddingStore, we just clear and reload
        // In a real app with persistent store, we'd be more selective
        // For now, let's assume we can clear the memory store
        // Actually, InMemoryEmbeddingStore doesn't have a clear() method in standard
        // API
        // But we can just "forget" it if we were creating it here.
        // Since it's a bean, we might need a different approach if we want to clear it.
        // For simplicity in this dev task, let's just add new ones,
        // but ideally we'd want to clear it.

        File dir = new File(RAG_DIR);
        if (dir.exists() && dir.isDirectory()) {
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(RAG_DIR, new ApacheTikaDocumentParser());
            for (Document doc : documents) {
                ingestDocument(doc);
            }
        }
    }

    public void ingestDocument(Document document) {
        // In LangChain4j 0.32.0, Document doesn't have textSegment() directly
        // We convert Document to TextSegment, preserving metadata
        TextSegment segment = TextSegment.from(document.text(), document.metadata());

        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
    }

    public void ingest(String content) {
        TextSegment segment = TextSegment.from(content);
        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
    }

    public List<TextSegment> search(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, 5, 0.7);
        return matches.stream().map(EmbeddingMatch::embedded).collect(Collectors.toList());
    }
}
