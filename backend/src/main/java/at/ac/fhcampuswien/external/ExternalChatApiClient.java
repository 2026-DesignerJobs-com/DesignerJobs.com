package at.ac.fhcampuswien.external;

import at.ac.fhcampuswien.chat.Conversation;
import at.ac.fhcampuswien.chat.Message;
import org.springframework.stereotype.Component;

import java.util.List;

// Prepared external chat API client.
// Later this class can call Supabase or another external chat server.
// For now, it is intentionally not connected yet.
@Component
public class ExternalChatApiClient {

    public List<Conversation> listConversations(String currentUserId) {
        throw new UnsupportedOperationException(
                "External chat API is not connected yet."
        );
    }

    public Conversation createConversation(Conversation conversation, String currentUserId) {
        throw new UnsupportedOperationException(
                "External chat API is not connected yet."
        );
    }

    public List<Message> getMessages(String conversationId, int page, String currentUserId) {
        throw new UnsupportedOperationException(
                "External chat API is not connected yet."
        );
    }

    public Message sendMessage(String conversationId, Message message, String currentUserId) {
        throw new UnsupportedOperationException(
                "External chat API is not connected yet."
        );
    }
}