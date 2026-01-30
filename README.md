# Agent Server

A Spring Boot-based backend for an AI Agent, leveraging **LangChain4j** for LLM orchestration and **Spring AI** for RAG capabilities.

## 🚀 Key Features

- **Multi-Model Support**: Supports multiple LLM configurations (OpenAI, local models via base URL).
- **RAG (Retrieval-Augmented Generation)**: Built-in support for ingesting and searching documents to enhance AI responses.
- **MCP (Model Context Protocol)**: Integration with MCP servers for extended capabilities.
- **Streaming Responses**: Real-time token streaming using WebFlux.
- **Dynamic Configuration**: Hot-swappable LLM settings via API.

## 🛠 Tech Stack

- **Java**: 21
- **Framework**: Spring Boot 3.3.2
- **LLM Library**: LangChain4j 0.32.0 (OpenAI, Embeddings)
- **Reactive Stack**: Project Reactor (WebFlux)
- **Build Tool**: Maven

## 📂 Project Structure

```text
agent-server/
├── src/main/java/com/example/agent/
│   ├── controller/      # REST Endpoints (Chat, Config, RAG)
│   ├── service/         # Business Logic (ModelConfig, Rag, MCP)
│   ├── model/           # Data Models
│   └── config/          # Application Configurations
├── src/main/resources/
│   ├── application.yml  # Main configuration
│   └── llm_conf.json    # Default LLM configurations
├── pom.xml              # Dependency management
└── MODEL_CONFIG.md      # Detailed model setup guide
```

## 📡 API Reference

### Chat Endpoints
- `POST /api/chat`: Send a prompt to the enabled LLM.
- `POST /api/chat/stream`: Stream response tokens in real-time.

### Model Configuration
- `GET /api/models`: List all configured models.
- `POST /api/models`: Add a new model configuration.
- `PUT /api/models/{id}`: Update an existing model.
- `DELETE /api/models/{id}`: Remove a model.

### RAG Operations
- `POST /api/rag/ingest`: Ingest text content into the vector store.
- `GET /api/rag/search`: Search for relevant documents.

## ⚙️ Getting Started

### Prerequisites
- JDK 21+
- Maven 3.6+

### Build and Run
```bash
./mvnw clean install
./mvnw spring-boot:run
```
The server will start on `http://localhost:8080` by default.

## 📝 Configuration

Model definitions are stored in `models.json` (or `llm_conf_working.json` for active state). You can update these via the `/api/models` endpoints to change providers, base URLs, and API keys without restarting the server.
