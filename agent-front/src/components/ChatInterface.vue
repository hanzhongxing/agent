<template>
  <div class="app-container">
    <div class="chat-window">
      <!-- Sidebar -->
      <div class="sidebar" :class="{ 'collapsed': isSidebarCollapsed }">
        <div class="sidebar-header">
          <div class="logo">
            <el-icon :size="20" color="#fff"><Cpu /></el-icon>
            <span v-if="!isSidebarCollapsed">Agent AI</span>
          </div>
          <el-button link @click="isSidebarCollapsed = !isSidebarCollapsed" class="collapse-btn">
            <el-icon color="#9ca3af"><Fold v-if="!isSidebarCollapsed" /><Expand v-else /></el-icon>
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

          <div class="section-title" style="margin-top: auto">OPTIONS</div>
          
          <div class="control-card">
            <div class="control-row">
              <span class="label">Knowledge Base</span>
              <el-switch v-model="currentSession.useRag" size="small" active-color="#2563eb" @change="syncSession(currentSession)" />
            </div>
            <div class="control-row" style="margin-top: 10px">
              <span class="label">Memory</span>
              <el-switch v-model="currentSession.useMemory" size="small" active-color="#2563eb" @change="syncSession(currentSession)" />
            </div>
          </div>
        </div>
      </div>

      <!-- Main Chat Area -->
      <div class="chat-area">
        <div class="chat-header">
           <div class="header-info">
             <h3>Assistant</h3>
             <el-select v-model="selectedModel" placeholder="Select Model" size="small" style="width: 160px; margin-left: 12px">
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
          <!-- Empty State -->
          <div v-if="currentSession.messages.length === 0" class="empty-state">
             <div class="icon-wrapper">
               <el-icon :size="32" color="#2563eb"><ChatDotRound /></el-icon>
             </div>
             <h2>How can I help you today?</h2>
             <div class="suggestions">
               <div class="suggestion-card" @click="setInput('What can you do?')">What can you do?</div>
               <div class="suggestion-card" @click="setInput('Write a poem about code.')">Write a poem</div>
               <div class="suggestion-card" @click="setInput('Explain Quantum Computing.')">Explain Quantum Computing</div>
             </div>
          </div>

          <!-- Message List -->
          <div v-for="(msg, index) in currentSession.messages" :key="index" :class="['message-row', getMessageClass(msg)]">
             <!-- Avatar -->
             <div class="avatar" :style="{ visibility: showAvatar(msg, index) ? 'visible' : 'hidden' }">
               <el-avatar 
                  :size="32" 
                  :src="msg.role === 'user' ? '' : '/robot-avatar.png'" 
                  :class="['avatar-img', msg.role]" 
                  v-if="showAvatar(msg, index)"
                >
                  <template #default>
                    <el-icon v-if="msg.role === 'assistant'"><Cpu /></el-icon>
                    <span v-else>U</span>
                  </template>
               </el-avatar>
               <div v-else style="width: 32px; height: 32px;"></div>
             </div>

             <!-- Content Area -->
             <div class="message-content-wrapper">

                <div v-if="msg.loading && !msg.content && msg.role === 'assistant' && !msg.isTool" class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>

                <div v-else-if="msg.role === 'user' || (msg.role === 'assistant' && !msg.isTool)" class="message-bubble" :class="{ 'streaming': msg.loading }">
                    <div v-if="isHtmlOrXml(msg.content)" v-html="msg.content" class="xml-content"></div>
                    <div v-else class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
                </div>

                <!-- 2. Tool Card -->
                <div v-else-if="msg.isTool" class="tool-card">
                  <div class="tool-header" @click="msg.expanded = !msg.expanded">
                    <div class="tool-title">
                      <el-icon class="tool-icon"><Connection /></el-icon>
                      <span>Used: <strong>{{ msg.toolName || getToolName(msg) }}</strong></span>
                    </div>
                    <el-icon :class="['expand-icon', { expanded: msg.expanded }]"><ArrowRight /></el-icon>
                  </div>
                  
                  <el-collapse-transition>
                    <div v-show="msg.expanded" class="tool-details">
                      <div class="detail-section">
                        <div class="section-label">Input</div>
                        <div class="code-block json">{{ formatArgs(msg.args || msg.toolCalls) }}</div>
                      </div>
                      <div class="detail-section">
                        <div class="section-label">Output</div>
                        <pre class="code-block xml">{{ msg.output || msg.content }}</pre>
                      </div>
                    </div>
                  </el-collapse-transition>
                </div>

                <span class="timestamp" v-if="msg.role !== 'tool' && !msg.isTool">{{ formatTime(new Date()) }}</span>
             </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="input-section">
           <div class="input-container">
             <el-input 
               v-model="inputMessage" 
               placeholder="Send a message..." 
               class="chat-input"
               @keyup.enter="sendMessage"
               :disabled="loading"
               type="text"
             >
              <template #suffix>
                 <el-button 
                   circle 
                   type="primary" 
                   class="send-btn" 
                   @click="sendMessage"
                   :disabled="!inputMessage.trim() || loading"
                 >
                   <el-icon><Position /></el-icon>
                 </el-button>
              </template>
             </el-input>
           </div>
           <div class="input-footer">
             AI can make mistakes. Please check important information.
           </div>
        </div>
      </div>
    </div>

    <!-- Settings Dialog -->
    <el-dialog v-model="showSettingsDialog" title="Settings" width="850px" class="settings-dialog" align-center>
      <div class="dialog-layout">
        <div class="settings-sidebar">
          <div 
            v-for="tab in ['model','rag','mcp','prompt']" 
            :key="tab" 
            :class="['settings-tab', { active: (activeTab === tab || activeTab.indexOf(tab) > -1)}]" 
            @click="activeTab = tab"
          >
            <el-icon v-if="tab === 'model'"><Menu/></el-icon>
            <el-icon v-else-if="tab==='rag'"><DocumentAdd/></el-icon>
            <el-icon v-else-if="tab==='mcp'"><Connection /></el-icon>
            <el-icon v-else-if="tab==='prompt'"><MagicStick /></el-icon>
            <span class="tab-label">
              {{ tab === 'model' ? 'Models' : tab === 'rag' ? 'Knowledge' : tab === 'mcp' ? 'Tools (MCP)' : 'Prompts' }}
            </span>
          </div>
        </div>

        <div class="settings-main">
          <transition name="fade" mode="out-in">

            <!-- MODEL TAB -->
            <div v-if="activeTab === 'model'" key="model" class="settings-content">
              <div class="content-header">
                <h3>Models</h3>
                <el-button type="primary" size="small" @click="activeTab = 'addmodel'">Add Model</el-button>
              </div>
              <div class="table-wrapper">
                <el-table :data="allModel" style="width: 100%" height="400px">
                  <el-table-column prop="name" label="Name" min-width="120" />
                  <el-table-column prop="modelName" label="Model ID" min-width="150" />
                  <el-table-column prop="embed" label="Embed" width="80" align="center">
                    <template #default="scope">
                      <el-tag size="small" v-if="scope.row.embed">Yes</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="Actions" width="120" align="center">
                    <template #default="scope">
                      <el-button link type="primary" size="small" @click="editModel(scope.row)">Edit</el-button>
                      <el-button link type="danger" size="small" @click="deleteModel(scope.row.id)">Delete</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>

            <!-- ADD MODEL TAB -->
            <div v-else-if="activeTab === 'addmodel'" key="addmodel" class="settings-content">
               <div class="content-header">
                <h3>{{ isEditing ? 'Edit Model' : 'Add Model' }}</h3>
              </div>
              <el-form label-position="top" class="settings-form">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="Name">
                      <el-input v-model="newModel.name" placeholder="e.g. DeepSeek" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                     <el-form-item label="Model ID">
                      <el-input v-model="newModel.modelName" placeholder="e.g. deepseek-chat" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item label="Base URL">
                  <el-input v-model="newModel.baseUrl" placeholder="https://api.openai.com/v1" />
                </el-form-item>
                <el-form-item label="API Key">
                  <el-input v-model="newModel.apiKey" type="password" show-password placeholder="sk-..." />
                </el-form-item>
                <el-form-item>
                  <el-checkbox v-model="newModel.embed">Use for Embeddings</el-checkbox>
                </el-form-item>
              </el-form>
              <div class="footer-actions">
                 <el-button @click="cancelEdit">Cancel</el-button>
                 <el-button type="primary" @click="saveNewModel">Save</el-button>
              </div>
            </div>

            <!-- RAG TAB -->
            <div v-else-if="activeTab==='rag'" key="rag" class="settings-content">
              <div class="rag-container">
                <el-tabs v-model="activeRagTab">
                  <el-tab-pane label="Files" name="manage">
                    <div class="table-wrapper">
                      <el-table :data="ragFiles" v-loading="ragFilesLoading" height="400px">
                        <el-table-column prop="name" label="File Name" show-overflow-tooltip />
                        <el-table-column prop="size" label="Size" width="100">
                          <template #default="scope">{{ formatSize(scope.row.size) }}</template>
                        </el-table-column>
                        <el-table-column width="80" label="Actions" align="center">
                          <template #default="scope">
                            <el-button link type="danger" size="small" @click="deleteRagFile(scope.row.name)">Delete</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </el-tab-pane>
                  <el-tab-pane label="Upload" name="upload">
                    <div class="upload-zone" @click="$refs.fileInput.click()">
                      <el-icon :size="32" color="#94a3b8"><UploadFilled /></el-icon>
                      <p>Click to upload documents</p>
                      <input type="file" ref="fileInput" style="display: none" @change="handleFileUpload" />
                    </div>
                    <div v-if="ingestLoading" class="loading-text">Processing...</div>
                  </el-tab-pane>
                  <el-tab-pane label="Raw Text" name="text">
                    <el-input v-model="ingestTitle" placeholder="Title" class="mb-3" />
                    <el-input type="textarea" v-model="ingestContent" :rows="8" placeholder="Content..." />
                    <el-button type="primary" class="mt-3" @click="handleTextIngest" :loading="ingestLoading">Add</el-button>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </div>

            <!-- MCP LIST TAB -->
            <div v-else-if="activeTab === 'mcp'" key="mcp" class="settings-content">
                <div class="content-header">
                  <h3>MCP Servers</h3>
                  <el-button type="primary" size="small" @click="startAddMcp">Add Server</el-button>
                </div>
                <div class="table-wrapper">
                  <el-table :data="mcpConfigs" style="width: 100%" height="400px">
                      <el-table-column prop="name" label="Name" />
                      <el-table-column prop="baseUrl" label="URL" show-overflow-tooltip/>
                      <el-table-column label="Enabled" prop="enabled" width="80"></el-table-column>
                      <el-table-column label="Actions" width="150" align="center">
                      <template #default="scope">
                          <el-button link type="primary" size="small" @click="viewMcp(scope.row)">Tools</el-button>
                          <el-button link type="primary" size="small" @click="editMcp(scope.row)">Edit</el-button>
                          <el-button link type="danger" size="small" @click="deleteMcp(scope.row.id)">Del</el-button>
                      </template>
                      </el-table-column>
                  </el-table>
                </div>
            </div>

            <!-- MCP ADD/EDIT TAB -->
            <div v-else-if="activeTab === 'addmcp'" key="addmcp" class="settings-content">
                <div class="content-header">
                   <h3>{{ isEditingMcp ? 'Edit Server' : 'New Server' }}</h3>
                </div>
                <el-form label-position="top" class="settings-form">
                  <el-form-item label="Name">
                      <el-input v-model="newMcp.name" placeholder="Name" />
                  </el-form-item>
                  <el-form-item label="Base URL">
                      <el-input v-model="newMcp.baseUrl" placeholder="http://localhost:3000" />
                  </el-form-item>
                  <el-form-item>
                      <el-checkbox v-model="newMcp.enabled">Enabled</el-checkbox>
                  </el-form-item>
                </el-form>
                <div class="footer-actions">
                    <el-button @click="activeTab = 'mcp'">Cancel</el-button>
                    <el-button type="primary" @click="saveMcp">Save</el-button>
                </div>
            </div>

            <!-- MCP TOOLS TAB -->
             <div v-else-if="activeTab === 'viewmcp'" key="viewmcp" class="settings-content">
                <div class="content-header">
                  <h3>Available Tools</h3>
                  <el-button size="small" @click="activeTab = 'mcp'">Back</el-button>
                </div>
                <div class="tool-list">
                   <el-collapse accordion>
                      <el-collapse-item v-for="tool in mcpTools" :key="tool.name">
                        <template #title>
                          <span class="tool-list-item">{{ tool.name }}</span>
                        </template> 
                        <div class="tool-panel">
                          <p class="desc">{{ tool.description }}</p>
                          <div class="tool-inputs-grid" v-if="tool.inputs && tool.inputs.length">
                              <div class="tool-input-row" v-for="input in tool.inputs" :key="input.field">
                                <div class="input-meta">
                                  <span class="fname">{{ input.field }}</span>
                                  <span class="ftype">{{ input.type }}</span>
                                  <span class="freq" v-if="input.required">*</span>
                                </div>
                                <el-input v-model="input.value" size="small" :placeholder="input.desc" />
                              </div>
                          </div>
                          <div class="mt-3 text-right">
                            <el-button type="primary" size="small" :loading="tool.loading" @click="executeMcpTool(tool)">Run Tool</el-button>
                          </div>
                          <div class="tool-result" v-if="tool.response">
                            <pre>{{ tool.response }}</pre>
                          </div>
                        </div>
                      </el-collapse-item>
                   </el-collapse>
                </div>
            </div>

            <!-- PROMPT LIST TAB -->
            <div v-else-if="activeTab === 'prompt'" key="prompt" class="settings-content">
              <div class="content-header">
                <h3>Prompts</h3>
                <el-button type="primary" size="small" @click="startAddPrompt">New Prompt</el-button>
              </div>
              <div class="table-wrapper">
                <el-table :data="promptList" style="width: 100%" height="400px">
                  <el-table-column prop="name" label="Name" width="150" show-overflow-tooltip />
                  <el-table-column prop="content" label="Content" show-overflow-tooltip />
                  <el-table-column label="Active" width="80" align="center">
                    <template #default="scope">
                       <el-switch size="small" v-model="scope.row.active" @change="activatePrompt(scope.row)" />
                    </template>
                  </el-table-column>
                  <el-table-column label="Actions" width="100" align="center">
                    <template #default="scope">
                      <el-button link type="primary" size="small" @click="editPrompt(scope.row)">Edit</el-button>
                      <el-button link type="danger" size="small" @click="deletePrompt(scope.row.id)">Del</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>

            <!-- PROMPT ADD/EDIT TAB -->
            <div v-else-if="activeTab === 'addprompt'" key="addprompt" class="settings-content">
               <div class="content-header">
                <h3>{{ isEditingPrompt ? 'Edit Prompt' : 'New Prompt' }}</h3>
              </div>
              <el-form label-position="top" class="settings-form full-h">
                <el-form-item label="Name">
                  <el-input v-model="newPrompt.name" placeholder="e.g. Coding Expert" />
                </el-form-item>
                <el-form-item label="Instructions" class="flex-grow-item">
                  <el-input 
                    type="textarea" 
                    v-model="newPrompt.content" 
                    placeholder="You are a helpful assistant..." 
                    resize="none"
                    class="full-h-textarea"
                  />
                </el-form-item>
              </el-form>
              <div class="footer-actions">
                 <el-button @click="activeTab = 'prompt'">Cancel</el-button>
                 <el-button type="primary" @click="savePrompt">Save</el-button>
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
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import 'highlight.js/styles/atom-one-dark.css';

import { chatApi } from '../services/api';
import { ElMessage } from 'element-plus';
import { 
  Cpu, Fold, Expand, DocumentAdd, Setting, 
  ChatDotRound, Position, Menu, Plus,
  ChatLineRound, Close, UploadFilled,
  Connection, ArrowRight, MagicStick
} from '@element-plus/icons-vue';

// Markdown Configuration
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>';
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>';
  }
});

// State
const sessions = ref([{ id: 'default', title: 'Default Conversation', messages: [], useMemory: true, useRag: false }]);
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
const promptList = ref([]);
const newPrompt = ref({ name: '', content: '', active: false });
const isEditingPrompt = computed(() => !!newPrompt.value.id);

const streamQueue = ref([]); // 缓冲队列
const isStreaming = ref(false); // 是否正在打字
let animationFrameId = null; // 用于取消动画帧

// --- Functions ---
const renderMarkdown = (text) => text ? md.render(text) : '';

const startTypewriter = () => {
  if (isStreaming.value) return;
  isStreaming.value = true;

  const animate = () => {
    if (streamQueue.value.length > 0) {
      // 智能速度控制：如果队列堆积太多（比如网络突然来了一大段），就一次多打几个字，避免打字太慢
      // 队列越长，step 越大，视觉上就是“追赶”效果
      const queueLength = streamQueue.value.length;
      const step = queueLength > 50 ? 5 : (queueLength > 20 ? 2 : 1);

      // 取出 step 个字符
      const chars = streamQueue.value.splice(0, step).join('');
      
      // 找到当前正在对话的最后一条 assistant 消息
      const currentMsgs = currentSession.value.messages;
      if (currentMsgs.length > 0) {
        const lastMsg = currentMsgs[currentMsgs.length - 1];
        // 只有当最后一条是 assistant 且不是工具时才拼接
        if (lastMsg.role === 'assistant' && !lastMsg.isTool) {
           lastMsg.content += chars;
           scrollToBottom(false); // 随打字滚动
        }
      }
    } else {
       // 队列空了，但不需要立即停止 isStreaming，
       // 因为可能网络还在请求中。我们在 sendMessage 的 finally 里彻底停止。
    }
    animationFrameId = requestAnimationFrame(animate);
  };
  animationFrameId = requestAnimationFrame(animate);
};

const showAvatar = (msg, index) => {
  if (msg.role === 'user'||msg.role === 'assistant') return true;

  return false;
};

const getMessageClass = (msg) => {
  if (msg.role === 'user') return 'user';
  if (msg.isTool) return 'tool-row';
  return 'assistant';
};

const formatArgs = (args) => {
  if (!args) return '{}';
  if (Array.isArray(args) && args.length > 0) {
      try {
          const argStr = args[0].arguments;
          const obj = JSON.parse(argStr);
          return JSON.stringify(obj, null, 2);
      } catch (e) {
          return args[0].arguments;
      }
  }
  if (typeof args === 'object') return JSON.stringify(args, null, 2);
  try {
    const obj = JSON.parse(args);
    return JSON.stringify(obj, null, 2);
  } catch (e) {
    return args;
  }
};

const getToolName = (msg) => {
    if (msg.toolName) return msg.toolName;
    if (msg.toolCalls && msg.toolCalls.length > 0) return msg.toolCalls[0].name;
    if (msg.args) return 'Tool';
    return 'Unknown Tool';
};

const isHtmlOrXml = (content) => {
    if (!content || typeof content !== 'string') return false;
    const trimmed = content.trim();
    return trimmed.startsWith('<') && (trimmed.includes('>') || trimmed.length < 50);
}

const currentSession = computed(() => sessions.value.find(s => s.id === currentSessionId.value) || sessions.value[0]);

// Model & MCP State
const selectedModel = ref('');
const modelOptions = ref([]);
const activeTab = ref('model');
const allModel=ref([]);
const newModel = ref({ name: '', baseUrl: '', apiKey: '', modelName: '', embed: false });
const mcpConfigs = ref([]);
const newMcp = ref({ name: '', baseUrl: '', enabled: true });
const isEditingMcp = computed(() => !!newMcp.value.id);
const currentMcpId = ref(null);
const mcpTools=ref([]);

const openSetting = () => {
   activeTab.value = 'model';
   showSettingsDialog.value = true;
   loadModels();
};

const executeMcpTool = async (tool) => {
   try {
      tool.loading = true;
      const inputs = {};
      if(tool.inputs && tool.inputs.length > 0) {
         tool.inputs.forEach(input => inputs[input.field] = input.value || '');
      }
      const response = await chatApi.executeMcpTool(currentMcpId.value,tool.name,JSON.stringify(inputs));
      tool.loading = false;
      tool.response = response;
      ElMessage.success("Executed successfully");
   } catch(e) {
      tool.loading = false;
      ElMessage.error("Execution failed");
   }
};

const getCurrentModelName = computed(() => {
   const found = modelOptions.value.find(m => m.id === selectedModel.value);
   return found ? found.modelName : 'Default';
});
const isEditing = computed(() => !!newModel.value.id);

// Lifecycle & Data Loading
onMounted(async () => {
   await loadChatModels();
   await loadSessions();
});

const loadChatModels = async () => {
    try {
       const models = await chatApi.getChatModels();
       modelOptions.value = models;
       if (models.length > 0 && !selectedModel.value) selectedModel.value = models[0].id;
    } catch (e) { console.error("Failed to load models", e); }
};

const loadModels = async () => {
    try { allModel.value = await chatApi.getModels(); } catch (e) {}
};

const loadMcps = async () => {
  try { mcpConfigs.value = await chatApi.getMcpServers(); } catch(e) {}
};

const startAddMcp = () => {
    newMcp.value = { name: '', baseUrl: '', enabled: true };
    activeTab.value = 'addmcp';
};

const viewMcp = async (row) => {
    activeTab.value = 'viewmcp';
    currentMcpId.value=row.id;
    mcpTools.value = await chatApi.getMcpTools(row.id);
};

const editMcp = (row) => {
    newMcp.value = { ...row };
    activeTab.value = 'addmcp';
};

const saveMcp = async () => {
    if(!newMcp.value.name || !newMcp.value.baseUrl) return ElMessage.warning("Name and URL are required");
    try {
        if (newMcp.value.id) await chatApi.updateMcpServer(newMcp.value.id,newMcp.value);
        else await chatApi.addMcpServer(newMcp.value);
        ElMessage.success("MCP saved");
        await loadMcps();
        activeTab.value = 'mcp';
    } catch(e) { ElMessage.error("Failed to save MCP"); }
};

const deleteMcp = async (id) => {
    try {
        await chatApi.deleteMcpServer(id);
        ElMessage.success("MCP deleted");
        await loadMcps();
    } catch(e) { ElMessage.error("Failed to delete"); }
}

const loadSessions = async () => {
    try {
        const backendSessions = await chatApi.getSessions();
        if (backendSessions && backendSessions.length > 0) {
            sessions.value = backendSessions.map(s => ({ ...s, messages: [] }));
            currentSessionId.value = sessions.value[0].id;
            await loadCurrentSessionMessages();
        }
    } catch (e) { console.error(e); }
};

const loadCurrentSessionMessages = async () => {
    if (!currentSessionId.value) return;
    try {
        const history = await chatApi.getSessionMessages(currentSessionId.value);
        const processedMessages = [];
        for (let i = 0; i < history.length; i++) {
            const msg = history[i];
            if (msg.role === 'assistant_tool_request') {
                if (i + 1 < history.length && history[i+1].role === 'tool') {
                    const toolResultMsg = history[i+1];
                    processedMessages.push({
                        role: 'tool', isTool: true,
                        toolName: msg.toolCalls[0].name,
                        args: msg.toolCalls,
                        output: toolResultMsg.output,
                        expanded: false
                    });
                    i++; 
                } else {
                    processedMessages.push({
                         role: 'tool', isTool: true,
                         toolName: msg.toolCalls[0].name,
                         args: msg.toolCalls,
                         output: '(No output)', expanded: false
                    });
                }
            } else if (msg.role === 'tool') {
                 processedMessages.push({ ...msg, isTool: true, expanded: false, args: msg.args || '{}' });
            } else {
                processedMessages.push(msg);
            }
        }
        currentSession.value.messages = processedMessages;
        scrollToBottom(true);
    } catch (e) {}
};


const saveNewModel = async () => {
   if(!newModel.value.name || !newModel.value.apiKey) return ElMessage.warning("Required fields missing");
   try {
      if (isEditing.value) await chatApi.updateModel(newModel.value.id, newModel.value);
      else await chatApi.addModel(newModel.value);
      ElMessage.success("Model saved!");
      await loadModels();
      newModel.value = { name: '', baseUrl: '', apiKey: '', modelName: '' };
      activeTab.value = 'model';
   } catch(e) { ElMessage.error("Failed to save model"); }
};

const editModel = (row) => {
    newModel.value = { ...row };
    activeTab.value = 'addmodel';
};

const cancelEdit = () => {
    newModel.value = { name: '', baseUrl: '', apiKey: '', modelName: '' };
    activeTab.value = 'model';
};

const deleteModel = async (id) => {
   try {
      await chatApi.deleteModel(id);
      ElMessage.success("Model deleted");
      await loadModels();
      if(selectedModel.value === id && modelOptions.value.length > 0) selectedModel.value = modelOptions.value[0].id;
   } catch(e) { ElMessage.error("Failed"); }
};

const formatTime = (date) => date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

const createNewSession = () => {
    const newId = Date.now().toString();
    const newSession = { id: newId, title: 'New Conversation', messages: [], useMemory: true, useRag: false };
    sessions.value.unshift(newSession);
    currentSessionId.value = newId;
    syncSession(newSession);
};

const syncSession = async (session) => {
    try { await chatApi.saveSession(session); } catch (e) {}
};

const switchSession = async (id) => {
    currentSessionId.value = id;
    if (currentSession.value.messages.length === 0) await loadCurrentSessionMessages();
};

const deleteSession = async (id) => {
    if (sessions.value.length === 1) return ElMessage.info("Cannot delete last session");
    const index = sessions.value.findIndex(s => s.id === id);
    if (index === -1) return;
    sessions.value.splice(index, 1);
    if (currentSessionId.value === id) currentSessionId.value = sessions.value[0].id;
    try { await chatApi.deleteSession(id); } catch (e) {}
};

const setInput = (text) => inputMessage.value = text;

const scrollToBottom = async (force = false) => {
  await nextTick();
  if (chatHistory.value) {
    const el = chatHistory.value;
    const isAtBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 150;
    if (force || isAtBottom) el.scrollTop = el.scrollHeight;
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

  streamQueue.value = []; // 清空残留
  startTypewriter();

  loading.value = true;
  await scrollToBottom(true); 

  try {

    let currentMsg = { role: 'assistant', content: '' ,loading: true};
    session.messages.push(currentMsg);
    
    let isHandlingTool = false;
    let currentToolMsg = null;

    await chatApi.sendMessage(
        userMsg, session.useRag, session.useMemory, selectedModel.value, session.id,
        (token) => {
            if (token.includes(':::TOOL_START:::')) {
                isHandlingTool = true;
                if (!currentMsg.content.trim()) {
                    session.messages.pop(); 
                }
                currentToolMsg = { role: 'tool', isTool: true, expanded: true, toolName: 'Detecting...', args: '', output: '' };
                session.messages.push(currentToolMsg);

                const parts = token.split(':::TOOL_START:::');
                if (parts.length > 1) {
                    const jsonPart = parts[1].split(':::TOOL_END:::')[0];
                    try {
                        const info = JSON.parse(jsonPart);
                        currentToolMsg.toolName = info.name;
                        currentToolMsg.args = JSON.stringify(info.args, null, 2);
                    } catch(e) { currentToolMsg.toolName = 'Processing...'; }
                }
                scrollToBottom();
            } else if (token.includes(':::TOOL_OUTPUT_START:::')) {
               if (currentToolMsg) currentToolMsg.output = ''; 
            } else if (token.includes(':::TOOL_OUTPUT_END:::')) {
               isHandlingTool = false;
               if (currentToolMsg) currentToolMsg.expanded = false; 
               currentMsg = { role: 'assistant', content: '' ,loading: true};
               session.messages.push(currentMsg);
               scrollToBottom();
            } else {
                if (isHandlingTool && currentToolMsg) {
                    currentToolMsg.output += token;
                } else {
                    const chars = token.split('');
                    streamQueue.value.push(...chars);
                }
                scrollToBottom(false);
            }
        }
    );
  } catch (error) {
    session.messages.push({ role: 'assistant', content: 'Error: ' + error.message });
  } finally {
    // 等待队列打完再结束 loading 状态
    const checkQueueFinished = setInterval(() => {
        if (streamQueue.value.length === 0) {
            clearInterval(checkQueueFinished);
            loading.value = false;
            isStreaming.value = false;
            cancelAnimationFrame(animationFrameId);
            
            // 将最后一条消息状态设为完成
            if(session.messages.length > 0) {
                const lastMsg = session.messages[session.messages.length - 1];
                if(lastMsg.role === 'assistant') lastMsg.loading = false;
            }
            syncSession(session); // 保存会话
        }
    }, 100);
  }
};

const loadPrompts = async () => {
  try { promptList.value = await chatApi.loadPrompts(); } catch (e) {}
};

const startAddPrompt = () => {
    newPrompt.value = { name: '', content: '', active: false };
    activeTab.value = 'addprompt';
};
const editPrompt = (row) => {
    newPrompt.value = { ...row };
    activeTab.value = 'addprompt';
};
const savePrompt = async () => {
    if (!newPrompt.value.name || !newPrompt.value.content) return ElMessage.warning("Required fields missing");
    try {
        if (newPrompt.value.id) await chatApi.updatePrompt(newPrompt.value.id, newPrompt.value);
        else await chatApi.savePrompt(newPrompt.value);
        ElMessage.success("Prompt saved");
        await loadPrompts();
        activeTab.value = 'prompt';
    } catch (e) { ElMessage.error("Failed"); }
};
const deletePrompt = async (id) => {
    try {
        await chatApi.deletePrompt(id);
        ElMessage.success("Prompt deleted");
        await loadPrompts();
    } catch (e) { ElMessage.error("Failed"); }
};
const activatePrompt = async (row) => {
    if (row.active) {
        promptList.value.forEach(p => { if (p.id !== row.id) p.active = false; });
        row.loading = true; 
        try {
            await chatApi.activatePrompt(row.id);
            ElMessage.success(`Activated: ${row.name}`);
        } catch (e) { row.active = !row.active; } finally {
            row.loading = false;
            await loadPrompts(); 
        }
    } else {
        try {
            row.active = false;
            await chatApi.updatePrompt(row.id, row);
        } catch(e) { row.active = true; }
    }
};

const loadRagFiles = async () => {
  ragFilesLoading.value = true;
  try { ragFiles.value = await chatApi.getRagFiles(); } catch (error) {} finally { ragFilesLoading.value = false; }
};

const handleFileUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  ingestLoading.value = true;
  try {
    await chatApi.uploadRagFile(file);
    ElMessage.success("Indexed!");
    await loadRagFiles();
    activeRagTab.value = 'manage';
  } catch (error) { ElMessage.error("Failed"); } finally {
    ingestLoading.value = false;
    event.target.value = ''; 
  }
};

const handleTextIngest = async () => {
  if (!ingestContent.value.trim()) return;
  ingestLoading.value = true;
  try {
    await chatApi.uploadRagText(ingestTitle.value, ingestContent.value);
    ElMessage.success("Added!");
    ingestContent.value = ''; ingestTitle.value = '';
    await loadRagFiles();
    activeRagTab.value = 'manage';
  } catch (error) { ElMessage.error("Failed"); } finally { ingestLoading.value = false; }
};

const deleteRagFile = async (filename) => {
  try {
    await chatApi.deleteRagFile(filename);
    ElMessage.success("Deleted");
    await loadRagFiles();
  } catch (error) {}
};

watch(activeRagTab, (val) => { if (val==='manage') loadRagFiles(); });
watch(activeTab, (val) => {
  if (val === 'model') loadRagFiles();
  if (val === 'rag'){ activeRagTab.value='manage'; loadRagFiles(); }
  if (val === 'mcp') loadMcps();
  if (val === 'prompt') loadPrompts();
});

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + ['B', 'KB', 'MB', 'GB'][i];
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap');

/* --- Reset & Layout --- */
.app-container {
  height: 100vh;
  width: 100vw;
  background-color: #f8fafc; /* Slate 50 */
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #1e293b;
}

.chat-window {
  width: 95%;
  max-width: 1400px;
  height: 92vh;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  display: flex;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

/* --- Sidebar --- */
.sidebar {
  width: 260px;
  background: #f1f5f9; /* Slate 100 */
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-right: 1px solid #e2e8f0;
}

.sidebar.collapsed { width: 64px; }

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #e2e8f0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #0f172a;
}
.logo .el-icon { background: #0f172a; padding: 4px; border-radius: 6px; box-sizing: content-box; }

.sidebar-content {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-btn.primary {
  width: 100%;
  justify-content: flex-start;
  background: #2563eb;
  border: 1px solid #e2e8f0;
  color: #fff;
  margin-bottom: 20px;
  font-weight: 500;
}
.sidebar-btn.primary:hover { border-color: #2563eb; color: #fff; background: #034ae3; }

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 8px;
  padding-left: 4px;
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 20px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: #475569;
  font-size: 13px;
  margin-bottom: 2px;
  transition: background 0.15s;
}
.session-item:hover { background: #e2e8f0; }
.session-item.active { background: #fff; color: #2563eb; font-weight: 500; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.session-title-text { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0 8px; }
.delete-session { opacity: 0; color: #94a3b8; }
.session-item:hover .delete-session { opacity: 1; }
.delete-session:hover { color: #ef4444; }

.control-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
}
.control-row { display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: 500; color: #334155; }

/* --- Chat Area --- */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.chat-header {
  height: 60px;
  border-bottom: 1px solid #f1f5f9;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-info { display: flex; align-items: center; }
.header-info h3 { font-size: 16px; font-weight: 600; margin: 0; }
.model-badge { font-size: 11px; background: #f1f5f9; color: #64748b; padding: 2px 8px; border-radius: 4px; margin-left: 8px; border: 1px solid #e2e8f0;}

/* Messages */
.messages-container {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  scroll-behavior: smooth;
}

.empty-state { text-align: center; margin-top: 10vh; color: #64748b; }
.icon-wrapper { background: #eff6ff; width: 64px; height: 64px; border-radius: 16px; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; }
.empty-state h2 { font-size: 18px; font-weight: 600; color: #1e293b; margin-bottom: 24px; }
.suggestions { display: flex; justify-content: center; gap: 12px; flex-wrap: wrap; }
.suggestion-card {
  border: 1px solid #e2e8f0;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  background: #fff;
  transition: all 0.2s;
}
.suggestion-card:hover { border-color: #2563eb; color: #2563eb; }

.message-row { display: flex; gap: 12px; max-width: 90%; margin-bottom: 8px; }
.message-row.user {
  flex-direction: row-reverse;
  align-self: flex-end; /* 关键：让用户消息靠右对齐 */
  margin-left: auto; /* 辅助 flexbox */
}
.message-row.assistant { align-self: flex-start; }

.avatar { flex-shrink: 0; margin-top: 2px; }
.avatar-img { background: #f1f5f9; color: #64748b; }
.avatar-img.user { background: #dbeafe; color: #2563eb; }

.message-content-wrapper { display: flex; flex-direction: column; max-width: 100%; min-width: 0; }

.message-bubble {
  padding: 10px 16px;
  font-size: 15px;
  line-height: 1.6; /* 增加行高 */
  position: relative;
  word-break: break-word;
  width: fit-content; /* 关键：气泡宽度适应内容 */
}

/* User Bubble Style */
.user .message-bubble {
  background: #2563eb; /* 纯蓝 */
  color: white;
  padding:0 10px;
  border-radius: 12px 12px 2px 12px; /* 更小的圆角 */
  margin-left: auto; /* 确保靠右 */
}

.assistant .message-bubble {
  background: #f8fafc;
  color: #1e293b;
  border: 1px solid #f1f5f9;
  border-radius: 10px 12px 12px 12px;
}

.timestamp { font-size: 10px; color: #94a3b8; margin-top: 4px; opacity: 0.8; }
.user .timestamp { text-align: right; }

/* Typing Indicator */
.typing-indicator { display: inline-flex; gap: 4px; padding: 10px 0; }
.typing-indicator span { width: 6px; height: 6px; background: #cbd5e1; border-radius: 50%; animation: bounce 1.4s infinite; }
.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

/* Tool Card Style */
.tool-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
  width: 100%;
  max-width: 600px;
  margin-top: 5px;
}
.tool-header {
  padding: 8px 12px;
  background: #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  border-bottom: 1px solid transparent;
  font-size: 13px;
}
.tool-header:hover { background: #f1f5f9; border-color: #e2e8f0; }
.tool-title { display: flex; align-items: center; gap: 6px; color: #475569; }
.tool-icon { color: #64748b; }
.expand-icon { font-size: 12px; color: #94a3b8; transition: transform 0.2s; }
.expand-icon.expanded { transform: rotate(90deg); }
.tool-details { padding: 12px; border-top: 1px solid #e2e8f0; background: #fff; }
.detail-section { margin-bottom: 12px; }
.detail-section:last-child { margin-bottom: 0; }
.section-label { font-size: 10px; text-transform: uppercase; color: #94a3b8; font-weight: 600; margin-bottom: 4px; letter-spacing: 0.5px; }

/* Input Section */
.input-section { padding: 20px 24px; border-top: 1px solid #f1f5f9; background: #fff; }
.input-container { position: relative; }
.chat-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 10px 12px;
  box-shadow: 0 0 0 1px #e2e8f0;
  background: #fff;
}
.chat-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px #2563eb; }
.send-btn { background-color: #f1f5f9; border: none; color: #64748b; transition: all 0.2s; }
.send-btn:hover { background-color: #2563eb; color: #fff; }
.input-footer { text-align: center; font-size: 11px; color: #94a3b8; margin-top: 8px; }

.xml-content { overflow-x: auto; background: #f8fafc; padding: 10px; border-radius: 4px; border: 1px solid #e2e8f0; font-family: monospace; font-size: 13px; }

/* --- Settings Dialog (Clean Style) --- */
.settings-dialog :deep(.el-dialog__body) { padding: 0 !important; }
.dialog-layout { display: flex; height: 500px; background: #fff;}
.settings-sidebar { width: 20%; border-right: 1px solid #f1f5f9; padding: 16px 8px; background: #f8fafc; }
.settings-tab {
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  border-radius: 6px;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}
.settings-tab:hover { background: #e2e8f0; color: #1e293b; }
.settings-tab.active { background: #fff; color: #2563eb; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.settings-main { flex: 1; padding: 24px; overflow: hidden; display: flex; flex-direction: column; }

.content-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; border-bottom: 1px solid #f1f5f9; padding-bottom: 16px; }
.content-header h3 { margin: 0; font-size: 18px; font-weight: 600; color: #1e293b; }

.table-wrapper { flex: 1; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }
.settings-form { max-width: 600px; }
.settings-form :deep(.el-form-item__label) { font-weight: 500; color: #475569; }
.footer-actions { margin-top: auto; padding-top: 16px; display: flex; justify-content: flex-end; gap: 12px; border-top: 1px solid #f1f5f9; }

/* Upload Zone */
.upload-zone { border: 2px dashed #e2e8f0; border-radius: 8px; padding: 30px; text-align: center; cursor: pointer; background: #f8fafc; transition: 0.2s; }
.upload-zone:hover { border-color: #2563eb; background: #eff6ff; }
.upload-zone p { margin: 10px 0 0; color: #64748b; font-size: 13px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; background: #e2e8f0; }
.status-dot.active { background: #10b981; }

/* MCP Tools Panel */
.tool-list-item { font-weight: 600; color: #334155; }
.tool-panel { padding: 8px 0; }
.tool-panel .desc { color: #64748b; font-size: 13px; margin: 0 0 12px; }
.tool-inputs-grid { background: #f8fafc; border: 1px solid #f1f5f9; padding: 12px; border-radius: 6px; }
.tool-input-row { display: flex; align-items: center; margin-bottom: 8px; gap: 10px; }
.tool-input-row:last-child { margin-bottom: 0; }
.input-meta { width: 120px; font-size: 12px; color: #475569; display: flex; flex-direction: column; }
.fname { font-weight: 600; } .ftype { color: #94a3b8; font-size: 10px; } .freq { color: #ef4444; }
.tool-result { margin-top: 12px; background: #f1f5f9; padding: 12px; border-radius: 6px; font-size: 12px; font-family: monospace; max-height: 200px; overflow: auto; }

/* Full Height Form Helpers */
.full-h { height: 100%; display: flex; flex-direction: column; }
.flex-grow-item { flex: 1; display: flex; flex-direction: column; margin-bottom: 0 !important; }
.flex-grow-item :deep(.el-form-item__content) { flex: 1; }
.full-h-textarea { height: 100%; }
.full-h-textarea :deep(.el-textarea__inner) { height: 100% !important; font-family: monospace; }
.mt-3 { margin-top: 12px; } .mb-3 { margin-bottom: 12px; } .text-right { text-align: right; }


/* --- 光标动画 (Mainstream Agent Effect) --- */
/* 只有当消息处于 loading (streaming) 状态时，才在内容末尾添加光标 */
.streaming .markdown-body > *:last-child::after {
  content: "●"; /* 或者使用 "|" */
  font-family: "Inter", sans-serif;
  color: #2563eb;
  font-size: 0.8em;
  animation: blink 1s steps(2, start) infinite;
  margin-left: 4px;
  vertical-align: baseline;
  display: inline-block;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* --- Markdown Body 样式优化 (解决换行、加粗无效问题) --- */
.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  font-size: 15px;
  line-height: 1.75; /* 增加行高，解决拥挤 */
  color: #1e293b;
  word-wrap: break-word;
}

/* 解决换行不明显的问题 */
.markdown-body p {
  margin-bottom: 1em; /* 段落间距 */
  min-height: 1em;    /* 防止空行塌陷 */
  white-space: pre-wrap; /* 关键：保留 Markdown 中的换行符 */
}

/* 解决加粗样式 */
.markdown-body strong {
  font-weight: 700;
  color: #0f172a; /* 加深颜色 */
}

/* 列表样式 */
.markdown-body ul, .markdown-body ol {
  margin-bottom: 1em;
  padding-left: 1.5em;
}
.markdown-body li {
  margin-bottom: 0.25em;
}

/* 标题样式 */
.markdown-body h1, .markdown-body h2, .markdown-body h3 {
  font-weight: 600;
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  line-height: 1.25;
  color: #0f172a;
}

/* 代码块样式 (类似 ChatGPT/Github) */
.markdown-body pre {
  background-color: #282c34;
  color: #abb2bf;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}
.markdown-body code {
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
  background-color: #f1f5f9;
  color: #ef4444; /* 行内代码红色高亮，区分度高 */
  padding: 2px 4px;
  border-radius: 4px;
}
.markdown-body pre code {
  background-color: transparent;
  color: inherit;
  padding: 0;
}

/* --- 用户气泡的 Markdown 特殊处理 --- */
/* 因为用户气泡是深色的，我们需要反转颜色 */
.user .markdown-body {
  color: #ffffff;
}
.user .markdown-body strong { color: #ffffff; }
.user .markdown-body code {
  background-color: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}
.user .markdown-body a { color: #bfdbfe; }
</style>