/* DESIGNJOBS.COM — chat.js
   REST-polling chat: load the current user + conversations, open new
   conversations, send/refresh messages. Requires auth.js (Auth.*) and
   escapeHtml from common.js. */
let activeConversationId = null;
let currentUser = null;

function getCurrentUserId() {
    return Auth.getUserId();
}

function getCurrentRole() {
    return Auth.getRole();
}

function setMessageInfo(text, type = "secondary") {
    const info = document.getElementById("message-info");
    info.textContent = text;
    info.className = "font-monospace small mt-3 mb-0 text-" + type;
}

function setConversationInfo(text, type = "secondary") {
    const info = document.getElementById("conversation-info");
    info.textContent = text;
    info.className = "font-monospace small mt-3 mb-0 text-" + type;
}

function setChatStatus(text, type = "success") {
    const status = document.getElementById("chat-status");
    status.textContent = text;
    status.className = "badge rounded-pill font-monospace text-uppercase text-bg-" + type;
    status.style.letterSpacing = ".12em";
}

function prefillFromUrl() {
    const params = new URLSearchParams(window.location.search);

    const jobIdFromUrl = params.get("jobId");
    const otherUserFromUrl =
        params.get("otherUserId") ||
        params.get("clientId") ||
        params.get("designerId");

    const jobInput = document.getElementById("job-id");
    const otherUserInput = document.getElementById("other-user-id");

    if (jobIdFromUrl && jobInput) {
        jobInput.value = jobIdFromUrl;
        setConversationInfo("Job ID was filled from the selected job.", "success");
    }

    if (otherUserFromUrl && otherUserInput) {
        otherUserInput.value = otherUserFromUrl;
    }
}

function renderConversations(conversations) {
    const conversationList = document.getElementById("conversation-list");
    const conversationCount = document.getElementById("conversation-count");

    conversationList.innerHTML = "";
    conversationCount.textContent = conversations.length;

    if (conversations.length === 0) {
        conversationList.innerHTML = '<div class="text-secondary">No conversations yet.</div>';
        return;
    }

    conversations.forEach(conversation => {
        const item = document.createElement("button");
        item.type = "button";

        const isActive = conversation.id === activeConversationId;

        item.className = isActive
            ? "btn rounded-3 text-start conversation-button active"
            : "btn btn-outline-primary rounded-3 text-start conversation-button";

        const jobId = escapeHtml(conversation.jobId || "Unknown job");
        const conversationId = escapeHtml(conversation.id || "");

        item.innerHTML = `
    <div class="fw-bold font-monospace text-uppercase"
         style="font-size:.85rem; letter-spacing:.08em">
      Job: ${jobId}
    </div>
    <div class="small text-secondary" style="word-break:break-all">
      ${conversationId}
    </div>
  `;

        item.addEventListener("click", async () => {
            activeConversationId = conversation.id;
            document.getElementById("active-conversation-label").textContent =
                "Active conversation: " + conversation.id;

            renderConversations(conversations);
            await loadMessages(conversation.id);
        });

        conversationList.appendChild(item);
    });
}

function renderMessages(messages) {
    const messageList = document.getElementById("message-list");
    messageList.innerHTML = "";

    if (messages.length === 0) {
        messageList.innerHTML =
            '<div class="text-secondary">No messages yet. Write the first message.</div>';
        return;
    }

    messages.forEach(message => {
        const isOwnMessage = message.senderId === getCurrentUserId();

        const bubble = document.createElement("div");
        bubble.className = isOwnMessage
            ? "message-bubble border border-primary rounded-4 p-3 ms-auto bg-primary text-white"
            : "message-bubble border border-primary rounded-4 p-3 me-auto bg-white";

        bubble.innerHTML = `
    <div class="font-monospace text-uppercase mb-1"
         style="font-size:.7rem; letter-spacing:.18em; opacity:.75">
      ${isOwnMessage ? "You" : "Other user"}
    </div>
    <div>${escapeHtml(message.content || "")}</div>
  `;

        messageList.appendChild(bubble);
    });

    messageList.scrollTop = messageList.scrollHeight;
}

async function loadCurrentUser() {
    const response = await Auth.authFetch(`${API_BASE}/auth/me`);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || "Could not load current user.");
    }

    currentUser = data;
    document.getElementById("current-user-id").textContent = data.userId;
}

async function loadConversations() {
    try {
        setChatStatus("Loading", "secondary");

        const response = await Auth.authFetch(`${API_BASE}/conversations`);
        const conversations = await response.json();

        if (!response.ok) {
            throw new Error(conversations.error || "Could not load conversations.");
        }

        renderConversations(conversations);
        setMessageInfo("Messaging is ready.", "success");
        setChatStatus("Ready", "success");

        if (conversations.length > 0 && activeConversationId === null) {
            activeConversationId = conversations[0].id;

            document.getElementById("active-conversation-label").textContent =
                "Active conversation: " + conversations[0].id;

            renderConversations(conversations);
            await loadMessages(activeConversationId);
        }

    } catch (error) {
        console.error("Conversation loading failed:", error);
        setChatStatus("Error", "danger");
        setMessageInfo("Could not load conversations. Please check login and backend.", "danger");
    }
}

async function createConversation(otherUserId, jobId) {
    const currentUserId = getCurrentUserId();
    const currentRole = getCurrentRole();

    const conversationData =
        currentRole === "DESIGNER"
            ? {
                clientId: otherUserId,
                designerId: currentUserId,
                jobId: jobId
            }
            : {
                clientId: currentUserId,
                designerId: otherUserId,
                jobId: jobId
            };

    const response = await Auth.authFetch(`${API_BASE}/conversations`, {
        method: "POST",
        body: JSON.stringify(conversationData)
    });

    const conversation = await response.json();

    if (!response.ok) {
        throw new Error(conversation.error || "Could not create conversation.");
    }

    activeConversationId = conversation.id;

    document.getElementById("active-conversation-label").textContent =
        "Active conversation: " + conversation.id;

    setConversationInfo("Conversation opened for job " + conversation.jobId + ".", "success");

    await loadConversations();
    await loadMessages(activeConversationId);

    return conversation;
}

async function loadMessages(conversationId) {
    try {
        const response = await Auth.authFetch(
            `${API_BASE}/conversations/${conversationId}/messages`
        );

        const messages = await response.json();

        if (!response.ok) {
            throw new Error(messages.error || "Could not load messages.");
        }

        renderMessages(messages);

    } catch (error) {
        console.error("Message loading failed:", error);

        document.getElementById("message-list").innerHTML =
            '<div class="text-danger">Messages could not be loaded.</div>';
    }
}

async function sendMessage(content) {
    if (!activeConversationId) {
        throw new Error("No active conversation selected.");
    }

    const response = await Auth.authFetch(
        `${API_BASE}/conversations/${activeConversationId}/messages`,
        {
            method: "POST",
            body: JSON.stringify({
                content: content
            })
        }
    );

    const savedMessage = await response.json();

    if (!response.ok) {
        throw new Error(savedMessage.error || "Could not send message.");
    }

    await loadMessages(activeConversationId);

    return savedMessage;
}

document.getElementById("conversation-form").addEventListener("submit", async event => {
    event.preventDefault();

    const otherUserId = document.getElementById("other-user-id").value.trim();
    const jobId = document.getElementById("job-id").value.trim();

    if (!otherUserId || !jobId) {
        setConversationInfo("Please enter the other user ID and job ID.", "danger");
        return;
    }

    try {
        await createConversation(otherUserId, jobId);
    } catch (error) {
        console.error("Create conversation failed:", error);
        setConversationInfo("Conversation could not be opened. Please check the IDs.", "danger");
    }
});

document.getElementById("message-form").addEventListener("submit", async event => {
    event.preventDefault();

    const input = document.getElementById("message-input");
    const content = input.value.trim();

    if (!content) {
        setMessageInfo("Please write a message first.", "danger");
        return;
    }

    try {
        await sendMessage(content);

        setMessageInfo("Message sent and saved in backend.", "success");
        input.value = "";

    } catch (error) {
        console.error("Send message failed:", error);
        setMessageInfo("Message could not be sent. Please select or open a conversation first.", "danger");
    }
});

document.getElementById("refresh-chat-button").addEventListener("click", async () => {
    await loadConversations();

    if (activeConversationId) {
        await loadMessages(activeConversationId);
    }

    setMessageInfo("Chat refreshed.", "success");
});

document.getElementById("copy-user-id-button").addEventListener("click", async () => {
    const userId = getCurrentUserId();

    try {
        await navigator.clipboard.writeText(userId);
        setConversationInfo("Your user ID was copied.", "success");
    } catch (error) {
        setConversationInfo("Could not copy user ID.", "danger");
    }
});

async function initChatPage() {
    try {
        prefillFromUrl();
        await loadCurrentUser();
        await loadConversations();
    } catch (error) {
        console.error("Chat page initialization failed:", error);
        setChatStatus("Error", "danger");
        setMessageInfo("Chat page could not be initialized. Please check login and backend.", "danger");
    }
}

initChatPage();
