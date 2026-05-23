package at.ac.fhcampuswien.chat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Service layer for in-platform messaging.
// Currently uses local repositories.
// Later this layer can delegate to ExternalChatApiClient for Supabase or another external chat API.
@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public List<Conversation> listConversations(String currentUserId) {
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

        return conversationRepository.create(conversation);
    }

    public List<Message> getMessages(String conversationId, int page, String currentUserId) {
        validateParticipant(conversationId, currentUserId);

        return messageRepository.findByConversationId(conversationId, page);
    }

    public Message sendMessage(String conversationId, Message message, String currentUserId) {
        validateParticipant(conversationId, currentUserId);

        if (message.content == null || message.content.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "message content is required"
            );
        }

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