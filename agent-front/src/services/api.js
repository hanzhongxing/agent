import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

export const chatApi = {
    // Chat & RAG
    async sendMessage(message, useRag = false, modelId = null, onChunk = null) {
        const payload = { message, useRag };
        if (modelId) {
            payload.modelId = modelId;
        }

        const response = await fetch(`${API_URL}/chat`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let fullContent = '';

        while (true) {
            const { value, done } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });
            // Handle SSE format "data: token\n\n" or raw text (Spring Flux usually sends raw text if not configured for SSE specifically, but produces SSE as requested)
            // If produces = "text/event-stream", we might get "data: ..."
            // Let's handle both raw and tokenized
            const lines = chunk.split('\n');
            for (const line of lines) {
                if (line.trim() === '') continue;
                let token = line;
                if (line.startsWith('data:')) {
                    token = line.substring(5).trim();
                }
                fullContent += token;
                if (onChunk) onChunk(token);
            }
        }
        return fullContent;
    },

    async ingestDocument(content) {
        const response = await axios.post(`${API_URL}/rag/ingest`, { content });
        return response.data;
    },

    async searchDocuments(query) {
        const response = await axios.get(`${API_URL}/rag/search`, { params: { query } });
        return response.data;
    },

    // Model Management
    async getModels() {
        const response = await axios.get(`${API_URL}/models`);
        return response.data;
    },

    async addModel(config) {
        const response = await axios.post(`${API_URL}/models`, config);
        return response.data;
    },

    async updateModel(id, config) {
        const response = await axios.put(`${API_URL}/models/${id}`, config);
        return response.data;
    },

    async deleteModel(id) {
        await axios.delete(`${API_URL}/models/${id}`);
    }
};
