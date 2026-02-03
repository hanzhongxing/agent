<template>
  <div class="app-container">
    <div class="chat-window">
      <!-- Sidebar / Config Panel -->
      <div class="sidebar" :class="{ 'collapsed': isSidebarCollapsed }">
        <div class="sidebar-header">
          <div class="logo">
            <el-icon :size="24" color="#fff"><Cpu /></el-icon>
            <span v-if="!isSidebarCollapsed">My Agent AI</span>
          </div>
          <el-button link @click="isSidebarCollapsed = !isSidebarCollapsed" class="collapse-btn">
            <el-icon color="#94a3b8"><Fold v-if="!isSidebarCollapsed" /><Expand v-else /></el-icon>
          </el-button>
        </div>

        <div class="sidebar-content" v-show="!isSidebarCollapsed">
          <div class="session-actions">
            <el-button class="sidebar-btn primary" @click="createNewSession">
              <el-icon><Plus /></el-icon>
              <span>New Chat</span>
            </el-button>
          </div>

          <div class="section-title">CHATS</div>
          <div class="sessions-list">
            <div 
              v-for="session in sessions" 
              :key="session.id" 
              :class="['session-item', { active: currentSessionId === session.id }]"
              @click="switchSession(session.id)"
            >
              <el-icon><ChatLineRound /></el-icon>
              <span class="session-title-text">{{ session.title || 'New Conversation' }}</span>
              <el-icon class="delete-session" @click.stop="deleteSession(session.id)"><Close /></el-icon>
            </div>
          </div>

          <div class="section-title" style="margin-top: 10px">OPTIONS</div>
          
          <div class="control-card">
            <div class="control-row">
              <span class="label">Knowledge Base (RAG)</span>
              <el-switch v-model="currentSession.useRag" size="small" active-color="#6366f1" @change="syncSession(currentSession)" />
            </div>
            <div class="control-row" style="margin-top: 10px">
              <span class="label">Context Memory</span>
              <el-switch v-model="currentSession.useMemory" size="small" active-color="#6366f1" @change="syncSession(currentSession)" />
            </div>
          </div>
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
             <el-tooltip content="Settings" placement="bottom">
                <el-button circle text @click="openSetting"><el-icon><Setting /></el-icon></el-button>
             </el-tooltip>
           </div>
        </div>

        <div class="messages-container" ref="chatHistory">
          <div v-if="currentSession.messages.length === 0" class="empty-state">
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

          <div v-for="(msg, index) in currentSession.messages" :key="index" :class="['message-row', msg.role]">
             <div class="avatar">
               <el-avatar :size="36" :src="msg.role === 'user' ? 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png' : '/robot-avatar.png'" :icon="msg.role === 'assistant' ? 'ElementPlus' : ''" :class="msg.role">
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
             <div class="avatar"><el-avatar :size="36" src="/robot-avatar.png"><el-icon><Cpu /></el-icon></el-avatar></div>
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

    <!-- Settings Dialog -->
    <el-dialog v-model="showSettingsDialog" title="Configuration" width="800px" class="premium-dialog" center align-center>
      <div class="dialog-layout">
        <div class="settings-sidebar">
          <div 
            v-for="tab in ['model','rag','mcp']" 
            :key="tab" 
            :class="['settings-tab', { active: (activeTab === tab || activeTab.indexOf(tab) > -1)}]" 
            @click="activeTab = tab"
          >
            <el-icon v-if="tab === 'model'"><Menu/></el-icon>
            <el-icon v-else-if="tab==='rag'"><DocumentAdd/></el-icon>
            <el-icon v-else-if="tab==='mcp'"><Connection /></el-icon>

            <span v-if="tab==='model'">Model</span>
            <span v-else-if="tab==='rag'">RAG</span>
            <span v-else-if="tab==='mcp'">MCP</span>
          </div>
        </div>

        <div class="settings-main">
          <transition name="fade" mode="out-in">
            <!-- MODEL TAB -->
            <div v-if="activeTab === 'model'" key="model" class="settings-content">
              <div class="content-header">
                <h3 style="display: inline-block">Installed Models</h3>
                <el-button type="primary" size="small" style="float: right;" @click="activeTab = 'addmodel'" class="gradient-btn" >Add New Model</el-button>
              </div>
              <div class="table-container">
                <el-table :data="allModel" style="width: 100%" height="100%" class="custom-table">
                  <el-table-column prop="name" label="Provider" min-width="120">
                    <template #default="scope">
                      <div class="model-info">
                        <span class="model-name">{{ scope.row.name }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="modelName" label="Model ID" min-width="150" />
                  <el-table-column prop="embed" label="EMBED" min-width="80" />
                  <el-table-column label="Actions" width="80" align="center">
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
            </div>

            <!-- ADD MODEL TAB -->
            <div v-else-if="activeTab === 'addmodel'" key="addmodel" class="settings-content">
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
                <el-form-item>
                  <span class="label" style="margin-right: 10px;">EMBED LLM</span>
                  <el-switch v-model="newModel.embed" size="small" active-color="#6366f1" />
                </el-form-item>
              </el-form>
              <div class="footer-actions">
                 <el-button @click="cancelEdit" size="large" plain>Cancel</el-button>
                 <el-button type="primary" @click="saveNewModel" size="large" class="gradient-btn">
                   {{ isEditing ? 'Update Model' : 'Create Model' }}
                 </el-button>
              </div>
            </div>

            <!-- RAG TAB -->
            <div v-else-if="activeTab==='rag'" key="rag" class="settings-content">
              <div class="rag-dialog-container">
                <el-tabs v-model="activeRagTab" class="custom-tabs">
                  <el-tab-pane label="Manage Files" name="manage">
                    <div class="rag-tab-content manage-tab">
                      <el-table :data="ragFiles" v-loading="ragFilesLoading" height="300px" class="custom-table smaller">
                        <el-table-column prop="name" label="File Name" min-width="200" show-overflow-tooltip />
                        <el-table-column prop="size" label="Size" width="100">
                          <template #default="scope">{{ formatSize(scope.row.size) }}</template>
                        </el-table-column>
                        <el-table-column label="Actions" width="80" align="center">
                          <template #default="scope">
                            <el-button circle size="small" type="danger" @click="deleteRagFile(scope.row.name)">
                              <el-icon><Delete /></el-icon>
                            </el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </el-tab-pane>
                  <el-tab-pane label="Upload File" name="upload">
                    <div class="rag-tab-content upload-tab">
                      <div class="upload-zone" @click="$refs.fileInput.click()">
                        <el-icon :size="48" color="#6366f1"><UploadFilled /></el-icon>
                        <p>Click to select or drag and drop files</p>
                        <span class="upload-hint">Supports PDF, DOC, EXCEL, HTML, TXT</span>
                        <input type="file" ref="fileInput" style="display: none" @change="handleFileUpload" />
                      </div>
                      <div v-if="ingestLoading" class="upload-status">
                          <el-icon class="is-loading"><Loading /></el-icon> <span>Processing...</span>
                      </div>
                    </div>
                  </el-tab-pane>
                  <el-tab-pane label="Paste Text" name="text">
                    <div class="rag-tab-content text-tab">
                      <el-input v-model="ingestTitle" placeholder="Document Title (optional)" class="margin-bottom" />
                      <el-input
                        type="textarea"
                        v-model="ingestContent"
                        :rows="10"
                        placeholder="Paste content here..."
                        class="custom-textarea"
                      />
                      <div class="tab-actions">
                        <el-button type="primary" @click="handleTextIngest" :loading="ingestLoading" block>Add to Memory</el-button>
                      </div>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </div>

            <!-- MCP LIST TAB -->
            <div v-else-if="activeTab === 'mcp'" key="mcp" class="settings-content">
                <div class="content-header">
                <h3 style="display: inline-block">MCP Servers</h3>
                <p>Manage Model Context Protocol servers.</p>
                <el-button type="primary" size="small" style="float: right; margin-top: -30px" @click="startAddMcp" class="gradient-btn">Add Server</el-button>
                </div>
                <div class="table-container">
                <el-table :data="mcpConfigs" style="width: 100%" height="100%" class="custom-table">
                    <el-table-column prop="name" label="Name" min-width="120" />
                    <el-table-column prop="baseUrl" label="Endpoint URL" min-width="200" />
                    <el-table-column label="Status" width="100">
                    <template #default="scope">
                        <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="small">{{ scope.row.enabled ? 'Active' : 'Disabled' }}</el-tag>
                    </template>
                    </el-table-column>
                    <el-table-column label="Actions" width="100" align="center">
                    <template #default="scope">
                        <el-button-group>
                          <el-tooltip content="View" placement="top">
                              <el-button circle size="small" type="info" @click="viewMcp(scope.row)"><el-icon><View /></el-icon></el-button>
                          </el-tooltip>
                          <el-tooltip content="Edit" placement="top">
                              <el-button circle size="small" type="primary" @click="editMcp(scope.row)"><el-icon><EditPen /></el-icon></el-button>
                          </el-tooltip>
                          <el-tooltip content="Delete" placement="top">
                              <el-button circle size="small" type="danger" @click="deleteMcp(scope.row.id)"><el-icon><DeleteFilled /></el-icon></el-button>
                          </el-tooltip>
                        </el-button-group>
                    </template>
                    </el-table-column>
                </el-table>
                </div>
            </div>

            <!-- MCP ADD/EDIT TAB -->
            <div v-else-if="activeTab === 'addmcp'" key="addmcp" class="settings-content">
                <div class="content-header">
                <h3>{{ isEditingMcp ? 'Edit MCP Server' : 'New MCP Server' }}</h3>
                <p>Configure an external MCP-compliant server endpoint.</p>
                </div>
                <el-form label-position="top" class="premium-form">
                <el-form-item label="Server Name">
                    <el-input v-model="newMcp.name" placeholder="e.g. Google Search Tools" prefix-icon="Monitor" />
                </el-form-item>
                
                <el-form-item label="Base URL">
                    <el-input v-model="newMcp.baseUrl" placeholder="http://localhost:3000" prefix-icon="Link" />
                    <div style="font-size: 11px; color: #9ca3af; margin-top: 5px">
                    The server must expose GET /tools and POST /tools/{name}.
                    </div>
                </el-form-item>

                <el-form-item>
                    <span class="label" style="margin-right: 10px;">Enabled</span>
                    <el-switch v-model="newMcp.enabled" size="small" active-color="#10b981" />
                </el-form-item>
                </el-form>
                <div class="footer-actions">
                    <el-button @click="activeTab = 'mcp'" size="large" plain>Cancel</el-button>
                    <el-button type="primary" @click="saveMcp" size="large" class="gradient-btn">
                    {{ isEditingMcp ? 'Update Server' : 'Register Server' }}
                    </el-button>
                </div>
            </div>

              <!-- MCP view mcp tools -->
             <div v-else-if="activeTab === 'viewmcp'" key="viewmcp" class="settings-content">
                <div class="content-header">
                  <h3>MCP Tools</h3>
                  <p>View tools available from MCP servers.</p>
                </div>
                <div class="tool-list">
                   <el-collapse>
                      <el-collapse-item v-for="tool in mcpTools" :key="tool.name">
                        <template #title>
                          <div class="tool-header">
                            <el-icon class="tool-name-icon"><Connection /></el-icon>
                            <span class="tool-name">{{ tool.name }}</span>
                          </div>
                        </template> 
                        <div class="tool-details">
                          <span class="description">{{ tool.description }}</span>
                          <div class="tool-inputs" v-if="tool.inputs&&tool.inputs.length>0">
                              <div class="input-item" v-for="input in tool.inputs" :key="input.field">
                                <span class="input-name">{{ input.field }}</span>
                                <span class="input-type">({{ input.type }})</span>
                                <span class="input-required">{{ input.required ? 'Required' : 'Optional' }}</span>
                                <span class="input-desc">{{ input.desc }}</span>
                                <el-input v-model="input.value" placeholder="Enter value"></el-input>
                              </div>
                          </div>
                          <div class="tool-function">
                            <el-button type="primary" :loading="tool.loading" @click="executeMcpTool(tool)" size="small">Execute Tool</el-button>
                          </div>
                          <div class="tool-response" v-if="tool.response">
                            <h4>Response:</h4>
                            <pre>{{ tool.response }}</pre>
                          </div>
                        </div>
                      </el-collapse-item>
                   </el-collapse>
                </div>
                <div class="footer-actions">
                    <el-button @click="activeTab = 'mcp'" size="large" plain>Cancel</el-button>
                </div>
            </div>
          </transition>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed, watch } from 'vue';
import { chatApi } from '../services/api';
import axios from 'axios'; // Ensure axios is imported if not used via chatApi wrapper
import { ElMessage } from 'element-plus';
import { 
  Cpu, Fold, Expand, DocumentAdd, Delete, Setting, 
  ChatDotRound, Position, Search, Menu, Plus, Edit,
  Link, Key, PriceTag,View, EditPen,DeleteFilled,
  ChatLineRound, Close, UploadFilled, Loading,
  Connection, Monitor
} from '@element-plus/icons-vue';

// State
const sessions = ref([
    {
        id: 'default',
        title: 'Default Conversation',
        messages: [],
        useMemory: true,
        useRag: false
    }
]);
const currentSessionId = ref('default');
const inputMessage = ref('');
const loading = ref(false);
const showSettingsDialog = ref(false);
const ingestContent = ref('');
const ingestTitle = ref('');
const ingestLoading = ref(false);
const activeRagTab = ref('manage');
const ragFiles = ref([]);
const ragFilesLoading = ref(false);
const chatHistory = ref(null);
const isSidebarCollapsed = ref(false);

const currentSession = computed(() => {
    return sessions.value.find(s => s.id === currentSessionId.value) || sessions.value[0];
});

// Model Management State
const selectedModel = ref('');
const modelOptions = ref([]);
const activeTab = ref('model');
const allModel=ref([]);
const newModel = ref({
  name: '',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  embed: false
});

// MCP Management State
const mcpConfigs = ref([]);
const newMcp = ref({ name: '', baseUrl: '', enabled: true });
const isEditingMcp = computed(() => !!newMcp.value.id);
const currentMcpId = ref(null);
const mcpTools=ref([]);
const openSetting = () => {
   showSettingsDialog.value = true;
   loadModels();
   loadMcps();
};

// execute MCP tool
const executeMcpTool = async (tool) => {
   try {
      tool.loading = true;
      const inputs = {};
      if(tool.inputs && tool.inputs.length > 0) {
         tool.inputs.forEach(input => {
            inputs[input.field] = input.value || '';
         });
      }
      const response = await chatApi.executeMcpTool(currentMcpId.value,tool.name,JSON.stringify(inputs));
      tool.loading = false;
      tool.response = response;
      ElMessage.success("Tool executed successfully");
      // Optionally handle response
   } catch(e) {
      tool.loading = false;
      ElMessage.error("Failed to execute tool");
   }
};

// Computed
const getCurrentModelName = computed(() => {
   const found = modelOptions.value.find(m => m.id === selectedModel.value);
   return found ? found.modelName : 'Default';
});

const isEditing = computed(() => !!newModel.value.id);

// Lifecycle
onMounted(async () => {
   await loadChatModels();
   await loadSessions();
});

const loadChatModels = async () => {
    try {
       const models = await chatApi.getChatModels();
       modelOptions.value = models;
       if (models.length > 0 && !selectedModel.value) {
          selectedModel.value = models[0].id;
       }
    } catch (e) {
      console.error("Failed to load models", e);
   }
};

const loadModels = async () => {
    try {
       const models = await chatApi.getModels();
       allModel.value = models;
    } catch (e) {
      console.error("Failed to load models", e);
   }
};

// MCP Functions
const loadMcps = async () => {
  try {
     const res = await chatApi.getMcpServers();
     mcpConfigs.value = res;
  } catch(e) { console.error("Failed to load MCPs", e); }
};

const startAddMcp = () => {
    newMcp.value = { name: '', baseUrl: '', enabled: true };
    activeTab.value = 'addmcp';
};

const viewMcp = async (row) => {
    activeTab.value = 'viewmcp';
    currentMcpId.value=row.id;
    const tools = await chatApi.getMcpTools(row.id);
    mcpTools.value = tools;
};

const editMcp = (row) => {
    newMcp.value = { ...row };
    activeTab.value = 'addmcp';
};

const saveMcp = async () => {
    if(!newMcp.value.name || !newMcp.value.baseUrl) {
        ElMessage.warning("Name and URL are required");
        return;
    }
    try {
        if (newMcp.value.id) {
            await chatApi.updateMcpServer(newMcp.value.id,newMcp.value);
            ElMessage.success("MCP updated");
        } else {
            await chatApi.addMcpServer(newMcp.value);
            ElMessage.success("MCP registered");
        }
        await loadMcps();
        activeTab.value = 'mcp';
    } catch(e) {
        ElMessage.error("Failed to save MCP");
    }
};

const deleteMcp = async (id) => {
    try {
        await chatApi.deleteMcpServer(id);
        ElMessage.success("MCP deleted");
        await loadMcps();
    } catch(e) { ElMessage.error("Failed to delete MCP"); }
}

const loadSessions = async () => {
    try {
        const backendSessions = await chatApi.getSessions();
        if (backendSessions && backendSessions.length > 0) {
            sessions.value = backendSessions.map(s => ({
                ...s,
                messages: []
            }));
            currentSessionId.value = sessions.value[0].id;
            await loadCurrentSessionMessages(); // Load for first session
        }
    } catch (e) {
        console.error("Failed to load sessions", e);
    }
};

const loadCurrentSessionMessages = async () => {
    if (!currentSessionId.value) return;
    try {
        const history = await chatApi.getSessionMessages(currentSessionId.value);
        currentSession.value.messages = history;
        scrollToBottom();
    } catch (e) {
        console.error("Failed to load session messages", e);
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
      activeTab.value = 'model';
   } catch(e) {
      ElMessage.error("Failed to save model");
   }
};

const editModel = (row) => {
    newModel.value = { ...row }; // Deep copy
    activeTab.value = 'addmodel';
};

const cancelEdit = () => {
    resetForm();
    activeTab.value = 'model';
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

const createNewSession = () => {
    const newId = Date.now().toString();
    const newSession = {
        id: newId,
        title: 'New Conversation',
        messages: [],
        useMemory: true,
        useRag: false
    };
    sessions.value.push(newSession);
    currentSessionId.value = newId;
    syncSession(newSession);
};

const syncSession = async (session) => {
    try {
        await chatApi.saveSession(session);
    } catch (e) {
        console.error("Failed to sync session to backend", e);
    }
};

const switchSession = async (id) => {
    currentSessionId.value = id;
    if (currentSession.value.messages.length === 0) {
        await loadCurrentSessionMessages();
    }
};

const deleteSession = async (id) => {
    if (sessions.value.length === 1) {
        ElMessage.info("Cannot delete the last session");
        return;
    }
    const index = sessions.value.findIndex(s => s.id === id);
    if (index === -1) return;

    sessions.value.splice(index, 1);
    if (currentSessionId.value === id) {
        currentSessionId.value = sessions.value[0].id;
    }
    
    try {
        await chatApi.deleteSession(id);
        ElMessage.success("Session deleted");
    } catch (e) {
        console.error("Failed to delete session from backend", e);
    }
};

// Actions
const setInput = (text) => {
  inputMessage.value = text;
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
  const session = currentSession.value;

  if (session.messages.length === 0) {
      session.title = userMsg.length > 20 ? userMsg.substring(0, 17) + '...' : userMsg;
      syncSession(session);
  }

  session.messages.push({ role: 'user', content: userMsg });
  inputMessage.value = '';
  loading.value = true;
  await scrollToBottom();

  try {
    const assistantMsg = ref({ role: 'assistant', content: '' });
    let assistantMsgPushed = false;

    let charQueue = [];
    let isTyping = false;
    let streamingFinished = false;

    const startTypewriter = () => {
      if (isTyping) return;
      isTyping = true;
      
      const interval = setInterval(() => {
        if (charQueue.length > 0) {
          if (!assistantMsgPushed) {
            session.messages.push(assistantMsg.value);
            assistantMsgPushed = true;
          }
          loading.value = false; 
          assistantMsg.value.content += charQueue.shift();
          scrollToBottom();
        } else if (streamingFinished && charQueue.length === 0) {
          clearInterval(interval);
          isTyping = false;
        }
      }, 20);
    };

    await chatApi.sendMessage(
        userMsg, 
        session.useRag, 
        session.useMemory, 
        selectedModel.value, 
        session.id,
        (token) => {
            charQueue.push(...token.split(''));
            startTypewriter();
        }
    );

    streamingFinished = true;
  } catch (error) {
    console.error(error);
    session.messages.push({ role: 'assistant', content: 'Connection Error: Please ensure the backend is running.' });
  } finally {
    // Keep loading true slightly longer if typing is happening
    if(!isTyping) {
        loading.value = false;
        scrollToBottom();
    }
  }
};

const loadRagFiles = async () => {
  ragFilesLoading.value = true;
  try {
    ragFiles.value = await chatApi.getRagFiles();
  } catch (error) {
    console.error("Failed to load RAG files");
  } finally {
    ragFilesLoading.value = false;
  }
};

const handleFileUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  ingestLoading.value = true;
  try {
    await chatApi.uploadRagFile(file);
    ElMessage.success("File uploaded and indexed!");
    await loadRagFiles();
    activeRagTab.value = 'manage';
  } catch (error) {
    ElMessage.error("Failed to upload file");
  } finally {
    ingestLoading.value = false;
    event.target.value = ''; 
  }
};

const handleTextIngest = async () => {
  if (!ingestContent.value.trim()) return;
  ingestLoading.value = true;
  try {
    await chatApi.uploadRagText(ingestTitle.value, ingestContent.value);
    ElMessage.success("Knowledge added!");
    ingestContent.value = '';
    ingestTitle.value = '';
    await loadRagFiles();
    activeRagTab.value = 'manage';
  } catch (error) {
    ElMessage.error("Failed to ingest text");
  } finally {
    ingestLoading.value = false;
  }
};

const deleteRagFile = async (filename) => {
  try {
    await chatApi.deleteRagFile(filename);
    ElMessage.success("File deleted");
    await loadRagFiles();
  } catch (error) {
    ElMessage.error("Failed to delete file");
  }
};

watch(activeRagTab, (val) => {
  if (val==='manage') {
    loadRagFiles();
  }
});

watch(activeTab, (val) => {
  if (val==='rag') {
    loadRagFiles();
  }
  if (val==='mcp') {
    loadMcps();
  }
});

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
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
  width: 90%;
  max-width: 95vw;
  height: 90vh;
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
  height: 35px;
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
  font-size: 16px;
  color: #fff;
}

.sidebar-content {
  flex: 1;
  padding: 10px 16px; 
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-title {
  font-size: 10px;
  text-transform: uppercase;
  color: #6b7280;
  margin-bottom: 10px;
  font-weight: 600;
  letter-spacing: 1px;
}

.control-card {
  background: #1f2937;
  padding: 12px;
  border-radius: 12px;
  margin-bottom: 12px;
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

.sidebar-btn {
  width: 100%;
  justify-content: flex-start;
  background: transparent;
  border: 1px solid #374151;
  color: #d1d5db;
  margin-left: 0 !important;
  height: 30px;
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

.session-actions {
  margin-bottom: 10px;
}

.sessions-list {
  flex: 2;
  overflow-y: auto;
  margin-bottom: 10px;
  padding-right: 5px;
}

.sessions-list::-webkit-scrollbar {
  width: 4px;
}

.sessions-list::-webkit-scrollbar-thumb {
  background: #374151;
  border-radius: 2px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: #9ca3af;
  gap: 10px;
  margin-bottom: 4px;
  font-size: 13px;
}

.session-item:hover {
  background: #1f2937;
  color: #fff;
}

.session-item.active {
  background: #1f2937;
  color: #6366f1;
  font-weight: 500;
}

.session-title-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.delete-session {
  opacity: 0;
  transition: opacity 0.2s;
  color: #9ca3af;
}

.session-item:hover .delete-session {
  opacity: 1;
}

.delete-session:hover {
  color: #ef4444;
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
  margin-top: auto; 
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

/* Settings Dialog */
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
  width: 150px;
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
  min-height: 0; 
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.rag-dialog-container {
  padding: 10px 0;
}

.rag-tab-content {
  padding-top: 20px;
  min-height: 350px;
}

.upload-zone {
  border: 2px dashed #e2e8f0;
  border-radius: 16px;
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #f8fafc;
}

.upload-zone:hover {
  border-color: #6366f1;
  background: #f1f5f9;
}

.upload-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 10px;
}

.upload-status {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #6366f1;
  font-weight: 500;
}

.margin-bottom {
  margin-bottom: 20px;
}

.tab-actions {
  margin-top: 20px;
}

.custom-table.smaller {
  font-size: 13px;
}

.custom-tabs :deep(.el-tabs__item) {
  font-weight: 600;
  color: #94a3b8;
}

.custom-tabs :deep(.el-tabs__item.is-active) {
  color: #6366f1;
}

.custom-tabs :deep(.el-tabs__active-bar) {
  background-color: #6366f1;
}

.tool-list{
  display: inline-block;
  height: 100%;
  overflow-y: auto;
}

.tool-list :deep(.tool-header) {
  gap: 10px;
}

.tool-list :deep(.tool-name-icon) {
   display: inline-block;
  vertical-align: middle; /* 垂直对齐 */
  font-size: 14px; /* 重置字体大小 */
}

.tool-list :deep(.tool-name) {
   display: inline-block;
  vertical-align: middle; /* 垂直对齐 */
  font-size: 14px; /* 重置字体大小 */
  margin-left:10px;
  font-weight: bold;
}

.tool-list :deep(.tool-details) {
  display: inline-block;
  width: 100%;
  padding: 10px 20px;
}

.tool-list :deep(.tool-details .description) {
  display: inline-block;
  width: 100%;
  font-size:14px;
  margin-bottom: 10px;
}

.tool-list :deep(.tool-inputs) {
  display: inline-block;
  width: 100%;
  font-size:14px;
  margin-bottom: 10px;
}

.tool-list :deep(.input-desc) {
  display: inline-block;
  width: 100%;
  font-size:12px;
  margin-bottom: 10px;
  color: #6b7280;
}


</style>