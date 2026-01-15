import { marked } from "https://cdn.jsdelivr.net/npm/marked/lib/marked.esm.js";

// Render newlines like chat apps typically do
marked.setOptions({ gfm: true, breaks: true });

// Keep per-element markdown buffer + throttled render timer
const mdState = new WeakMap();

function appendMarkdownChunk(el, chunk, delayMs = 80) {
    if (!el) return;

    let state = mdState.get(el);
    if (!state) {
        state = { buffer: "", timerId: null };
        mdState.set(el, state);
    }

    state.buffer += chunk;

    // Throttle re-renders (avoid parsing on every tiny chunk)
    if (state.timerId) return;

    state.timerId = setTimeout(() => {
        state.timerId = null;
        // Re-render whole buffer (marked isn't incremental)
        el.innerHTML = marked.parse(state.buffer);
    }, delayMs);
}

function flushMarkdown(el) {
    const state = mdState.get(el);
    if (!el || !state) return;

    if (state.timerId) {
        clearTimeout(state.timerId);
        state.timerId = null;
    }
    el.innerHTML = marked.parse(state.buffer);
}

const chatWindowTemplate = document.createElement('template');
chatWindowTemplate.innerHTML = `

    <style 
    <style>

        :host {
            --chat-bg: #13171aff;
            --chat-font-family: 'Trebuchet MS', sans-serif;
            --chat-bubble-bg: #274C77;
            --bot-response-bg: transparent;
            --chat-bubble-text-color: white;
            --input-bg: #001d0cff;
        }

        .chat-window {
            height: 100%;
            width: 100%;
        }

        .chat-container {
            border-radius: 12px;
            height: 70%;
            width: 90%;
            overflow-y: auto;
            background-color: var(--chat-bg);
            padding: 5%;
        }
        
        .chat-bubble {
            border-radius: 8px;
            padding: 1em;
            color: var(--chat-bubble-text-color);
            font-family: var(--chat-font-family);
        }

        .bot-chat {
            background-color: var(--bot-response-bg);
        }

        .bot-chat::after {
            content: '...';
            display: inline-block;
            width: 1.5em;
            margin-left: 0.3em;
            text-align: left;
            animation: loading-dots .5s steps(4, end) infinite;
        }

        .bot-chat.complete::after {
            display: none;
        }

        @keyframes loading-dots {
            0% { content: '.'; }
            33% { content: '..'; }
            66% { content: '...'; }
            100% { content: '.'; }
        }

        .user-chat {
            margin-left: auto;
            max-width: 80%;
            align-self: flex-end;
            background-color: var(--chat-bubble-bg);
        }

        .chat-bubble .bot-thinking {
            position: relative;
            font-size: 0.9rem;
            cursor: default;
            font-style: italic;
            opacity: 0.7;
            max-height: none;
            transition: max-height 0.3s ease;
            margin-top: 0.5rem;
            margin-bottom: 0.5rem;
            padding: 0.5rem;
            border: 1px solid var(--thinking-border-color, #4a90e2);
            border-radius: 8px;
            background:transparent;
            overflow-y: visible;
        }

            .chat-bubble .bot-thinking .thinking-text {
                overflow: hidden;
            }

            .chat-bubble.complete .bot-thinking .thinking-text {
                max-height: 0.5em;
            }

            .chat-bubble.complete .bot-thinking .thinking-text.show {
                max-height: none;
            }
        
        .chat-container .thinking-collapse-button {
            z-index: 1000;
            position: absolute;
            top: -0.5em;
            background-color: var(--chat-bg);
        }

        .chat-row {
            display: flex;
        }
            
        .input-container {
            position: relative;
            height: 20%;
            width: 100%;
        }

        .input-area {
            width: 100%;
            height: 100%;
            padding: var(--input-padding, 1rem);
            border-radius: var(--input-border-radius, 8px);
            color: var(--input-text-color, white);
            background-color: var(--input-bg, #001d0cff);
            font-family: 'Trebuchet MS', sans-serif;
            resize: none;
        }

        .model-select {
            position: absolute;
            bottom: 1rem;
            right: 5%;
            padding: 0.3rem;
            color: var(--select-text-color, white);
            background-color: var(--select-bg, #003817ff);
            border: none;
            border-radius: 8px;
        }
            
    </style>

    <div class="chat-window">
        <div class="chat-container" id="chat-container">
            <!-- Chat messages will be appended here -->
        </div>
        <div class="input-container">
            <textarea id="promptInput" class="input-area" placeholder="Enter your prompt here..."></textarea>
            <select id="modelSelect" class="model-select">
                <option value="">Loading models...</option>
            </select>
        </div>
        
    </div>
`

let temp = ` <div class="chat-row">
                <div class="chat-bubble user-chat">
                    Hi there!
                </div>
            </div>

             <div class="chat-row">
                <div class="chat-bubble bot-chat">
                    <div class="bot-thinking"> 
                        The user is asking me to be a helpful assistant.
                        Maybe I should think about how to respond...
                    </div>

                    <div class="bot-content">
                        Hello! How can I assist you today?
                    </div>
                </div>
            </div>`


class ChatWindow extends HTMLElement {

    static observedAttributes = ['base-url'];

    constructor() {
        super();
        this.attachShadow({ mode: 'open' });

        const envBase = (import.meta?.env?.VITE_API_BASE_URL ?? '').trim();
        this._baseUrl = (this.getAttribute('base-url') ?? envBase ?? '').trim() || '/api';
    }

    connectedCallback() {
        this.shadowRoot.appendChild(chatWindowTemplate.content.cloneNode(true));
        this.chatId = crypto.randomUUID();

        this.chatContainer = this.shadowRoot.getElementById('chat-container');

        this.modelSelection = this.shadowRoot.getElementById('modelSelect');
        this.fillModels();

        

        this.textInput = this.shadowRoot.getElementById('promptInput');
        this.textInput.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                this.sendAndHandleMessage();
            }
        });

    }

    async fillModels(){
        const response = await fetch(`${this._baseUrl}/api/ai/models`);
        var json = await response.json();
        this.modelSelection.innerHTML = '';
        
        const defaultModel = json.defaultModel;
        json.availableModels.forEach(model => {
            const option = document.createElement('option');
            option.value = model;
            option.textContent = model;
            if(model === defaultModel){
                option.selected = true;
            }
            this.modelSelection.appendChild(option);
        });

    }

    sendAndHandleMessage() {

        const message = this.textInput.value.trim();
        if (message === '') return;

        this.appendUserMessage(message);
        this.textInput.value = '';

        this.startAndHandleChatStream(message)
        .catch(error => {
            console.error('Error during chat stream:', error);
        });

    }

    async startAndHandleChatStream(prompt){
        
        const botChatRow = document.createElement('div');
        botChatRow.classList.add('chat-row');
        const botMessageBubble = document.createElement('div');
        botMessageBubble.classList.add('chat-bubble', 'bot-chat');
        botChatRow.appendChild(botMessageBubble);
        this.chatContainer.appendChild(botChatRow);
        this.chatContainer.scrollTop = this.chatContainer.scrollHeight;

        let currentContentDiv = null;
        let currentThinkingDiv = null;
        let currentToolCallDiv = null;

        function onThinkingReceived(chunk){
            currentToolCallDiv = null;
            currentContentDiv = null;

            if(!currentThinkingDiv){
                const thinkingContainerDiv = document.createElement('div');
                thinkingContainerDiv.classList.add('bot-thinking');

                const collapseButton = document.createElement('div');
                collapseButton.classList.add('thinking-collapse-button');
                collapseButton.textContent = 'thinking... (click to expand/collapse)';
                collapseButton.onclick = (target) => {
                    const thinkingTextElement = target.currentTarget.closest('.bot-thinking').querySelector('.thinking-text');
                    if (thinkingTextElement) {
                        thinkingTextElement.classList.toggle('show');
                    }
                };

                currentThinkingDiv = document.createElement('div');
                currentThinkingDiv.classList.add('thinking-text', 'markdown-replace');

                thinkingContainerDiv.appendChild(collapseButton);
                thinkingContainerDiv.appendChild(currentThinkingDiv);
                botMessageBubble.appendChild(thinkingContainerDiv);
            }

            appendMarkdownChunk(currentThinkingDiv, chunk);
        }

        function onToolCallReceived(chunk){
            // TODO
        }

        function onContentReceived(chunk){
            currentThinkingDiv = null;
            currentToolCallDiv = null;

            if(!currentContentDiv){
                currentContentDiv = document.createElement('div');
                currentContentDiv.classList.add('bot-content', 'markdown-replace');
                botMessageBubble.appendChild(currentContentDiv);
            }

            appendMarkdownChunk(currentContentDiv, chunk);
        }

        const response = await fetch(`${this._baseUrl}/api/ai/chat/${this.chatId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: prompt })
        });

        function handleEvent(event){
            // Expecting SSE-ish chunks: "data: {...}\n\n"
            if (!event.startsWith('data:')) return;

            const data = event.slice('data:'.length).trim();
            if (!data) return;

            let json;
            try {
                json = JSON.parse(data);
            } catch {
                return;
            }

            const chunk = json.chunk ?? '';
            if (String(chunk).trim() === '') return;

            switch(json.chunkType){
                case 'thinking':
                    onThinkingReceived(String(chunk));
                    break;
                case 'tool_call':
                    onToolCallReceived(String(chunk));
                    break;
                case 'content':
                    onContentReceived(String(chunk));
                    break;
            }
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');
        let buffer = '';

        while (true) {
            const {done, value} = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, {stream: true});

            const events = buffer.split('\n\n');
            buffer = events.pop();
            for (const rawLine of events) {
                handleEvent(rawLine);
            }

            // Keep scroll pinned while streaming
            this.chatContainer.scrollTop = this.chatContainer.scrollHeight;
        }

        // Flush any throttled renders at the end (so final state is correct)
        botMessageBubble.querySelectorAll('.markdown-replace').forEach(elem => flushMarkdown(elem));

        botMessageBubble.classList.add('complete');
    }

    appendUserMessage(message) {
        const userMessageRow = document.createElement('div');
        userMessageRow.classList.add('chat-row');

        const userMessageBubble = document.createElement('div');
        userMessageBubble.classList.add('chat-bubble', 'user-chat');
        userMessageBubble.textContent = message;

        userMessageRow.appendChild(userMessageBubble);
        this.chatContainer.appendChild(userMessageRow);
        this.chatContainer.scrollTop = this.chatContainer.scrollHeight;
    }

}

customElements.define('chat-window', ChatWindow);