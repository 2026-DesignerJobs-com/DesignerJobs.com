package at.ac.fhcampuswien.chat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

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
        try {
            if (currentUserId == null || currentUserId.isBlank()) {
                return Collections.emptyList();
            }
            return conversationRepository.findByUserId(currentUserId);
        } catch (Exception e) {
            System.err.println("Database error listing conversations: " + e.getMessage());
            return Collections.emptyList();
        }
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

        try {
            return conversationRepository.create(conversation);
        } catch (Exception e) {
            System.err.println("Database error creating conversation: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create conversation");
        }
    }

    public List<Message> getMessages(String conversationId, int page, String currentUserId) {
        validateParticipant(conversationId, currentUserId);

        try {
            return messageRepository.findByConversationId(conversationId, page);
        } catch (Exception e) {
            System.err.println("Database error fetching messages: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Message sendMessage(String conversationId, Message message, String currentUserId) {
        if (message.content == null || message.content.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "message content is required"
            );
        }

        validateParticipant(conversationId, currentUserId);

        message.conversationId = conversationId;
        message.senderId = currentUserId;
        message.flagged = false;

        try {
            return messageRepository.save(message);
        } catch (Exception e) {
            System.err.println("Database error saving message: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send message");
        }
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