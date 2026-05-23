package at.ac.fhcampuswien.chat;

import at.ac.fhcampuswien.external.ExternalChatApiClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Service layer for in-platform messaging.
// Current mode: local H2 repositories.
// Future mode: external chat API via ExternalChatApiClient.
@Service
public class ChatService {

    /*
     * Set this to true later when the external chat API is fully connected.
     * For now it must stay false, so the current working local chat flow does not break.
     */
    private static final boolean USE_EXTERNAL_CHAT_API = false;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ExternalChatApiClient externalChatApiClient;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       ExternalChatApiClient externalChatApiClient) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.externalChatApiClient = externalChatApiClient;
    }

    public List<Conversation> listConversations(String currentUserId) {
        if (USE_EXTERNAL_CHAT_API) {
            return externalChatApiClient.listConversations(currentUserId);
        }

        return conversationRepository.findByUserId(currentUserId);
    }

    public Conversation createConversation(Conversation conversation, String currentUserId) {
        validateConversationRequest(conversation);

        boolean currentUserIsParticipant =
                currentUserId.equals(conversation.clientId)
                        || currentUserId.equals(conversation.designerId);

        if (!currentUserIsParticipant) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "current user must be part of the conversation"
            );
        }

        if (USE_EXTERNAL_CHAT_API) {
            return externalChatApiClient.createConversation(conversation, currentUserId);
        }

        return conversationRepository.create(conversation);
    }

    public List<Message> getMessages(String conversationId, int page, String currentUserId) {
        if (USE_EXTERNAL_CHAT_API) {
            return externalChatApiClient.getMessages(conversationId, page, currentUserId);
        }

        validateParticipant(conversationId, currentUserId);

        return messageRepository.findByConversationId(conversationId, page);
    }

    public Message sendMessage(String conversationId, Message message, String currentUserId) {
        if (message.content == null || message.content.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "message content is required"
            );
        }

        if (USE_EXTERNAL_CHAT_API) {
            return externalChatApiClient.sendMessage(conversationId, message, currentUserId);
        }

        validateParticipant(conversationId, currentUserId);

        message.conversationId = conversationId;
        message.senderId = currentUserId;
        message.flagged = false;

        return messageRepository.save(message);
    }

    private void validateConversationRequest(Conversation conversation) {
        if (conversation.clientId == null || conversation.clientId.isBlank()
                || conversation.designerId == null || conversation.designerId.isBlank()
                || conversation.jobId == null || conversation.jobId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "clientId, designerId and jobId are required"
            );
        }
    }

    private void validateParticipant(String conversationId, String currentUserId) {
        boolean currentUserIsParticipant =
                conversationRepository.isParticipant(conversationId, currentUserId);

        if (!currentUserIsParticipant) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "current user is not part of this conversation"
            );
        }
    }
}