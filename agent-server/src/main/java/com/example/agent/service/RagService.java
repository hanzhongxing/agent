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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Value("agent.base.path")
    private String basePath;

    @Value("agent.rag.folder")
    private String ragFolder;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public RagService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void init() {
        reindexAll();
    }

    public synchronized void reindexAll() {
        File dir = new File(basePath+"/"+ragFolder);
        if (dir.exists() && dir.isDirectory()) {
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(basePath+"/"+ragFolder, new ApacheTikaDocumentParser());
            for (Document doc : documents) {
                ingestDocument(doc);
            }
        }
    }

    public void ingestDocument(Document document) {
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
