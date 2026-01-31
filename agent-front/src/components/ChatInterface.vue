<template>
  <div class="app-container">
    <div class="chat-window">
      <!-- Sidebar / Config Panel -->
      <div class="sidebar" :class="{ 'collapsed': isSidebarCollapsed }">
        <div class="sidebar-header">
          <div class="logo">
            <el-icon :size="24" color="#fff"><Cpu /></el-icon>
            <span v-if="!isSidebarCollapsed">Agent AI</span>
          </div>
          <el-button link @click="isSidebarCollapsed = !isSidebarCollapsed" class="collapse-btn">
            <el-icon color="#94a3b8"><Fold v-if="!isSidebarCollapsed" /><Expand v-else /></el-icon>
          </el-button>
        </div>

        <div class="sidebar-content" v-show="!isSidebarCollapsed">
          <div class="section-title">OPTIONS</div>
          
          <div class="control-card">
            <div class="control-row">
              <span class="label">Knowledge Base (RAG)</span>
              <el-switch v-model="useRag" size="small" active-color="#6366f1" />
            </div>
            <p class="description">Enable to use uploaded documents for improved context.</p>
            
            <div class="control-row" style="margin-top: 20px">
              <span class="label">Context Memory</span>
              <el-switch v-model="useMemory" size="small" active-color="#6366f1" />
            </div>
            <p class="description">Assistant remembers previous parts of the conversation.</p>
          </div>

          <div class="action-buttons">
            <el-button class="sidebar-btn primary" @click="showIngestDialog = true">
              <el-icon><DocumentAdd /></el-icon>
              <span>Upload Documents</span>
            </el-button>
            <!-- Model Settings moved to top right icon -->
            <el-button class="sidebar-btn" @click="clearHistory">
              <el-icon><Delete /></el-icon>
              <span>Clear History</span>
            </el-button>
          </div>
        </div>
        
        <div class="sidebar-footer" v-show="!isSidebarCollapsed">
           <span class="status-dot"></span> Online
        </div>
      </div>

      <!-- Main Chat Area -->
      <div class="chat-area">
        <div class="chat-header">
           <div class="header-info">
             <h3>Assistant</h3>
             <el-select v-model="selectedModel" placeholder="Select Model" size="small" style="width: 150px; margin-left: 10px">
                <el-option
                  v-for="item in modelOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
             </el-select>
             <span class="model-badge">{{ getCurrentModelName }}</span>
           </div>
           <div class="header-actions">
             <el-tooltip content="Model Settings" placement="bottom">
                <el-button circle text @click="showSettingsDialog = true"><el-icon><Setting /></el-icon></el-button>
             </el-tooltip>
           </div>
        </div>

        <div class="messages-container" ref="chatHistory">
          <div v-if="messages.length === 0" class="empty-state">
             <div class="icon-wrapper">
               <el-icon :size="40" color="#6366f1"><ChatDotRound /></el-icon>
             </div>
             <h2>Welcome, Human.</h2>
             <p>I am your advanced AI agent. Ask me anything or upload documents to get started.</p>
             <div class="suggestions">
               <el-tag class="suggestion-tag" @click="setInput('What can you do?')">What can you do?</el-tag>
               <el-tag class="suggestion-tag" @click="setInput('Write a poem about code.')">Write a poem</el-tag>
               <el-tag class="suggestion-tag" @click="setInput('Explain Quantum Computing.')">Explain Quantum Computing</el-tag>
             </div>
          </div>

          <div v-for="(msg, index) in messages" :key="index" :class="['message-row', msg.role]">
             <div class="avatar">
               <el-avatar :size="36" :src="msg.role === 'user' ? 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png' : ''" :icon="msg.role === 'assistant' ? 'ElementPlus' : ''" :class="msg.role">
                  <template #default v-if="msg.role === 'assistant'"><el-icon><Cpu /></el-icon></template>
               </el-avatar>
             </div>
             <div class="message-bubble-container">
               <div class="message-bubble">
                 {{ msg.content }}
               </div>
               <span class="timestamp">{{ formatTime(new Date()) }}</span>
             </div>
          </div>
          
          <div v-if="loading" class="message-row assistant">
             <div class="avatar"><el-avatar :size="36"><el-icon><Cpu /></el-icon></el-avatar></div>
             <div class="typing-indicator">
               <span></span><span></span><span></span>
             </div>
          </div>
        </div>

        <div class="input-section">
           <div class="input-wrapper">
             <el-input 
               v-model="inputMessage" 
               placeholder="Type your message..." 
               class="modern-input"
               @keyup.enter="sendMessage"
               :disabled="loading"
               type="text"
             >
              <template #prefix>
                <el-icon class="input-icon"><Search /></el-icon>
              </template>
              <template #suffix>
                 <el-button 
                   circle 
                   type="primary" 
                   class="send-btn-inner" 
                   @click="sendMessage"
                   :disabled="!inputMessage.trim() || loading"
                 >
                   <el-icon><Position /></el-icon>
                 </el-button>
              </template>
             </el-input>
           </div>
           <div class="input-footer">
             <span>Press Enter to send</span>
           </div>
        </div>
      </div>
    </div>

    <!-- Upload Dialog -->
    <el-dialog v-model="showIngestDialog" title="Add Knowledge" width="450px" class="glass-dialog" center align-center>
      <div class="dialog-content">
        <p class="dialog-desc">Paste text below to add it to the agent's long-term memory.</p>
        <el-input
          type="textarea"
          v-model="ingestContent"
          :rows="8"
          placeholder="Paste content here..."
          class="custom-textarea"
        />
      </div>
      <template #footer>
        <div class="dialog-actions">
           <el-button @click="showIngestDialog = false">Cancel</el-button>
           <el-button type="primary" @click="handleIngest" :loading="ingestLoading">Add to Memory</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Settings Dialog -->
    <el-dialog v-model="showSettingsDialog" title="Model Configuration" width="750px" class="premium-dialog" center align-center>
      <div class="dialog-layout">
        <div class="settings-sidebar">
          <div 
            v-for="tab in ['list', 'add']" 
            :key="tab" 
            :class="['settings-tab', { active: activeTab === tab || (tab === 'add' && isEditing) }]" 
            @click="activeTab = tab"
          >
            <el-icon v-if="tab === 'list'"><Menu /></el-icon>
            <el-icon v-else><Plus /></el-icon>
            <span>{{ tab === 'list' ? 'Model List' : (isEditing ? 'Edit Model' : 'Add New') }}</span>
          </div>
        </div>

        <div class="settings-main">
          <transition name="fade" mode="out-in">
            <div v-if="activeTab === 'list'" key="list" class="settings-content">
              <div class="content-header">
                <h3>Installed Models</h3>
                <p>Manage your AI backend configurations.</p>
              </div>
              <div class="table-container">
                <el-table :data="modelOptions" style="width: 100%" height="100%" class="custom-table">
                  <el-table-column prop="name" label="Provider" min-width="120">
                    <template #default="scope">
                      <div class="model-info">
                        <span class="model-name">{{ scope.row.name }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="modelName" label="Model ID" min-width="150" />
                  <el-table-column label="Actions" width="160" align="center">
                    <template #default="scope">
                      <el-button-group>
                        <el-tooltip content="Edit" placement="top">
                          <el-button circle size="small" type="primary" @click="editModel(scope.row)"><el-icon><EditPen /></el-icon></el-button>
                        </el-tooltip>
                        <el-tooltip content="Delete" placement="top">
                          <el-button circle size="small" type="danger" @click="deleteModel(scope.row.id)"><el-icon><DeleteFilled /></el-icon></el-button>
                        </el-tooltip>
                      </el-button-group>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <div class="footer-actions">
                 <el-button type="primary" size="large" @click="activeTab = 'add'" class="gradient-btn" >Add New Model</el-button>
              </div>
            </div>

            <div v-else-if="activeTab === 'add'" key="add" class="settings-content">
               <div class="content-header">
                <h3>{{ isEditing ? 'Edit Configuration' : 'New Configuration' }}</h3>
                <p>Configure access to your AI provider.</p>
              </div>
              <el-form label-position="top" class="premium-form">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="Configuration Name">
                      <el-input v-model="newModel.name" placeholder="e.g. My DeepSeek" prefix-icon="Edit" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                     <el-form-item label="Model ID">
                      <el-input v-model="newModel.modelName" placeholder="e.g. deepseek-chat" prefix-icon="PriceTag" />
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-form-item label="Base URL">
                  <el-input v-model="newModel.baseUrl" placeholder="https://api.openai.com/v1" prefix-icon="Link" />
                </el-form-item>

                <el-form-item label="API Key">
                  <el-input v-model="newModel.apiKey" type="password" show-password placeholder="Enter your API key" prefix-icon="Key" />
                </el-form-item>
              </el-form>
              <div class="footer-actions">
                 <el-button @click="cancelEdit" size="large" plain>Cancel</el-button>
                 <el-button type="primary" @click="saveNewModel" size="large" class="gradient-btn">
                   {{ isEditing ? 'Update Model' : 'Create Model' }}
                 </el-button>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed } from 'vue';
import { chatApi } from '../services/api';
import { ElMessage } from 'element-plus';
import { 
  Cpu, Fold, Expand, DocumentAdd, Delete, Setting, 
  ChatDotRound, Position, Search, Menu, Plus, Edit,
  Link, Key, PriceTag, EditPen, DeleteFilled
} from '@element-plus/icons-vue';

// State
const messages = ref([]);
const inputMessage = ref('');
const loading = ref(false);
const useRag = ref(false);
const useMemory = ref(true);
const showIngestDialog = ref(false);
const showSettingsDialog = ref(false);
const ingestContent = ref('');
const ingestLoading = ref(false);
const chatHistory = ref(null);
const isSidebarCollapsed = ref(false);

// Model Management State
const selectedModel = ref('');
const modelOptions = ref([]);
const activeTab = ref('list');
const newModel = ref({
  name: '',
  baseUrl: '',
  apiKey: '',
  modelName: ''
});

// Computed
const getCurrentModelName = computed(() => {
   const found = modelOptions.value.find(m => m.id === selectedModel.value);
   return found ? found.modelName : 'Default';
});

const isEditing = computed(() => !!newModel.value.id);

// Lifecycle
onMounted(async () => {
   await loadModels();
});

const loadModels = async () => {
    try {
       const models = await chatApi.getModels();
       modelOptions.value = models;
       if (models.length > 0 && !selectedModel.value) {
          selectedModel.value = models[0].id;
       }
    } catch (e) {
      console.error("Failed to load models", e);
   }
};

const saveNewModel = async () => {
   if(!newModel.value.name || !newModel.value.apiKey) {
      ElMessage.warning("Name and API Key are required");
      return;
   }
   try {
      if (isEditing.value) {
          await chatApi.updateModel(newModel.value.id, newModel.value);
          ElMessage.success("Model updated!");
      } else {
          await chatApi.addModel(newModel.value);
          ElMessage.success("Model saved!");
      }
      await loadModels();
      resetForm();
      activeTab.value = 'list';
   } catch(e) {
      ElMessage.error("Failed to save model");
   }
};

const editModel = (row) => {
    newModel.value = { ...row }; // Deep copy
    activeTab.value = 'add';
};

const cancelEdit = () => {
    resetForm();
    activeTab.value = 'list';
};

const resetForm = () => {
    newModel.value = { name: '', baseUrl: '', apiKey: '', modelName: '' };
};

const deleteModel = async (id) => {
   try {
      await chatApi.deleteModel(id);
      ElMessage.success("Model deleted");
      await loadModels();
      if(selectedModel.value === id && modelOptions.value.length > 0) {
         selectedModel.value = modelOptions.value[0].id;
      }
   } catch(e) {
      ElMessage.error("Failed to delete");
   }
};



// Formatting
const formatTime = (date) => {
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

// Actions
const setInput = (text) => {
  inputMessage.value = text;
};

const clearHistory = () => {
  messages.value = [];
};

const scrollToBottom = async () => {
  await nextTick();
  if (chatHistory.value) {
    chatHistory.value.scrollTop = chatHistory.value.scrollHeight;
  }
};

const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return;
  
  const userMsg = inputMessage.value;
  messages.value.push({ role: 'user', content: userMsg });
  inputMessage.value = '';
  loading.value = true;
  await scrollToBottom();

  try {
    const assistantMsg = ref({ role: 'assistant', content: '' });
    messages.value.push(assistantMsg.value);

    let charQueue = [];
    let isTyping = false;
    let streamingFinished = false;

    const startTypewriter = () => {
      if (isTyping) return;
      isTyping = true;
      
      const interval = setInterval(() => {
        if (charQueue.length > 0) {
          // Progressively hide loading once we start receiving content
          loading.value = false; 
          assistantMsg.value.content += charQueue.shift();
          scrollToBottom();
        } else if (streamingFinished) {
          clearInterval(interval);
          isTyping = false;
        }
      }, 20); // Adjust speed here (20ms per character)
    };

    await chatApi.sendMessage(userMsg, useRag.value, useMemory.value, selectedModel.value, (token) => {
      // Split token into characters and add to queue
      charQueue.push(...token.split(''));
      startTypewriter();
    });

    streamingFinished = true;
  } catch (error) {
    console.error(error);
    messages.value.push({ role: 'assistant', content: 'Connection Error: Please ensure the backend is running.' });
  } finally {
    // If no tokens were ever received, we still need to stop loading
    setTimeout(() => {
        loading.value = false;
        scrollToBottom();
    }, 500);
  }
};

const handleIngest = async () => {
  if (!ingestContent.value.trim()) return;
  ingestLoading.value = true;
  try {
    await chatApi.ingestDocument(ingestContent.value);
    ElMessage.success({
      message: 'Knowledge successfully ingested.',
      type: 'success',
      plain: true,
    });
    ingestContent.value = '';
    showIngestDialog.value = false;
  } catch (error) {
    ElMessage.error('Failed to upload knowledge.');
  } finally {
    ingestLoading.value = false;
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

/* Global Reset & Container */
.app-container {
  height: 100vh;
  width: 100vw;
  background-color: #eef2f6;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: 'Inter', sans-serif;
}

.chat-window {
  width: 1200px;
  max-width: 95vw;
  height: 85vh;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.1);
  display: flex;
  overflow: hidden;
  position: relative;
}

/* Sidebar */
.sidebar {
  width: 280px;
  background: #111827;
  color: #fff;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  border-right: 1px solid #1f2937;
}

.sidebar.collapsed {
  width: 70px;
}

.sidebar-header {
  height: 70px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  justify-content: space-between;
  border-bottom: 1px solid #1f2937;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 18px;
  color: #fff;
}

.sidebar-content {
  flex: 1;
  padding: 30px 20px;
}

.section-title {
  font-size: 10px;
  text-transform: uppercase;
  color: #6b7280;
  margin-bottom: 15px;
  font-weight: 600;
  letter-spacing: 1px;
}

.control-card {
  background: #1f2937;
  padding: 15px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.control-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.label {
  font-size: 14px;
  font-weight: 500;
}

.description {
  font-size: 11px;
  color: #9ca3af;
  margin: 0;
  line-height: 1.4;
}

.sidebar-btn {
  width: 100%;
  justify-content: flex-start;
  background: transparent;
  border: 1px solid #374151;
  color: #d1d5db;
  margin-bottom: 10px;
  margin-left: 0 !important;
  height: 42px;
}

.sidebar-btn:hover {
  background: #374151;
  color: #fff;
}

.sidebar-btn.primary {
  background: #4f46e5;
  border-color: #4f46e5;
  color: white;
}

.sidebar-btn.primary:hover {
  background: #4338ca;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid #1f2937;
  font-size: 12px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  display: inline-block;
}

/* Chat Area */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.chat-header {
  height: 70px;
  border-bottom: 1px solid #f3f4f6;
  padding: 0 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info h3 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.model-badge {
  font-size: 11px;
  background: #e0e7ff;
  color: #4f46e5;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
  margin-left: 8px;
}

.messages-container {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Empty State */
.empty-state {
  text-align: center;
  margin-top: 100px;
  color: #374151;
}

.icon-wrapper {
  background: #eef2ff;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.empty-state h2 {
  font-size: 24px;
  margin-bottom: 10px;
}

.suggestions {
  margin-top: 30px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

.suggestion-tag {
  cursor: pointer;
  transition: all 0.2s;
  padding: 8px 16px;
  height: auto;
  font-size: 13px;
}

.suggestion-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

/* Messages */
.message-row {
  display: flex;
  gap: 16px;
  max-width: 80%;
}

.message-row.user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-bubble {
  padding: 16px 20px;
  font-size: 15px;
  line-height: 1.6;
  position: relative;
}

.user .message-bubble {
  background: #4f46e5;
  color: white;
  border-radius: 20px 20px 4px 20px;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

.assistant .message-bubble {
  background: #f3f4f6;
  color: #1f2937;
  border-radius: 20px 20px 20px 4px;
}

.avatar {
  margin-top: auto; /* Bottom align avatar */
  margin-bottom: 8px;
}

.timestamp {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 5px;
  display: block;
  text-align: right;
}

.user .timestamp {
  text-align: right;
  color: #c7d2fe;
}

/* Typing Indicator */
.typing-indicator span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #9ca3af;
  border-radius: 50%;
  margin: 0 2px;
  animation: bounce 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Input Section */
.input-section {
  padding: 24px 30px;
  border-top: 1px solid #f3f4f6;
}

.input-wrapper {
  position: relative;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  border-radius: 30px;
}

.modern-input :deep(.el-input__wrapper) {
  border-radius: 30px;
  padding: 6px 15px;
  box-shadow: 0 0 0 1px #e5e7eb;
}

.modern-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #4f46e5;
}

.send-btn-inner {
  transform: scale(0.9);
}

.input-footer {
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
  margin-top: 10px;
}

/* Transitions */
/* Premium Settings Dialog */
.premium-dialog :deep(.el-dialog__body) {
  padding: 0 !important;
}

.dialog-layout {
  display: flex;
  height: 500px;
  background: #f8fafc;
  border-radius: 0 0 16px 16px;
  overflow: hidden;
}

.settings-sidebar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e2e8f0;
  padding: 20px 0;
}

.settings-tab {
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s;
  color: #64748b;
  font-weight: 500;
  font-size: 14px;
}

.settings-tab:hover {
  background: #f1f5f9;
  color: #4f46e5;
}

.settings-tab.active {
  background: #eef2ff;
  color: #4f46e5;
  border-right: 3px solid #4f46e5;
}

.settings-main {
  flex: 1;
  padding: 30px;
  overflow: hidden;
}

.settings-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.content-header {
  margin-bottom: 24px;
}

.content-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #0f172a;
}

.content-header p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.table-container {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 10px;
  margin-bottom: 20px;
  min-height: 0; /* Important for flex child to be able to shrink and scroll */
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.gradient-btn {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%) !important;
  border: none !important;
  color: white !important;
}

.gradient-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.premium-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #334155;
  font-size: 13px;
  margin-bottom: 6px;
}

.model-info .model-name {
  font-weight: 600;
  color: #0f172a;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.sidebar {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>
