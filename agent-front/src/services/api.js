import axios from 'axios';

const API_URL = 'http://localhost:8070/api';

export const chatApi = {
    // Chat & RAG
    async sendMessage(message, useRag = false, useMemory = true, modelId = null, sessionId = null, onChunk = null) {
        const payload = { message, useRag, useMemory };
        if (sessionId) {
            payload.sessionId = sessionId;
        }
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

            // Handle SSE format "data: token\n\n"
            // Spring Flux might send multiple data lines in one chunk or partial lines
            const lines = chunk.split('\n');
            for (const line of lines) {
                if (!line) continue;

                let token = line;
                if (line.startsWith('data:')) {
                    token = line.substring(5);
                    // If it was "data: ", keep the space. If it was "data:token", it's still "token".
                }

                if (token) {
                    fullContent += token;
                    if (onChunk) onChunk(token);
                }
            }
        }
        return fullContent;
    },

    async getRagFiles() {
        const response = await axios.get(`${API_URL}/rag/files`);
        return response.data;
    },
    async uploadRagFile(file) {
        const formData = new FormData();
        formData.append('file', file);
        const response = await axios.post(`${API_URL}/rag/upload`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    },
    async uploadRagText(title, content) {
        const response = await axios.post(`${API_URL}/rag/text`, { title, content });
        return response.data;
    },
    async deleteRagFile(filename) {
        const response = await axios.delete(`${API_URL}/rag/files/${filename}`);
        return response.data;
    },

    async searchDocuments(query) {
        const response = await axios.get(`${API_URL}/rag/search`, { params: { query } });
        return response.data;
    },

    // Session Management
    async getSessions() {
        const response = await axios.get(`${API_URL}/session/sessions`);
        return response.data;
    },

    async saveSession(session) {
        await axios.post(`${API_URL}/session/sessions`, session);
    },

    async deleteSession(sessionId) {
        await axios.delete(`${API_URL}/session/sessions/${sessionId}`);
    },

    async getSessionMessages(sessionId) {
        const response = await axios.get(`${API_URL}/memory/sessions/${sessionId}/messages`);
        return response.data;
    },

    // Model Management
    async getChatModels() {
        const response = await axios.get(`${API_URL}/models/chat`);
        return response.data;
    },

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
    },

    async getMcpServers() {
        const response = await axios.get(`${API_URL}/mcp`);
        return response.data;
    },

    async addMcpServer(config) {
        const response = await axios.post(`${API_URL}/mcp`, config);
        return response.data;
    },

    async updateMcpServer(id, config) {
        const response = await axios.put(`${API_URL}/mcp/${id}`, config);
        return response.data;
    },

    async deleteMcpServer(id) {
        await axios.delete(`${API_URL}/mcp/${id}`);
    },

    async getMcpTools(id) {
        const response = await axios.get(`${API_URL}/mcp/${id}/tools`);
        return response.data;
    },

    async executeMcpTool(id, name, inputs) {
         const response = await axios.post(`${API_URL}/mcp/tool/call/${id}`, {name,inputs });
        return response.data;
    }
};
