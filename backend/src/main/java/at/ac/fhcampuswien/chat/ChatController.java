package at.ac.fhcampuswien.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Owner: Bruno — In-platform chat (REST-based, Phase 2)
@RestController
@RequestMapping("/conversations")
public class ChatController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatController(ConversationRepository conversationRepository,
                          MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    // GET  /conversations              → list conversations for current user
    @GetMapping
    public ResponseEntity<?> listConversations(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        String currentUserId = auth.getName();

        List<Conversation> conversations =
                conversationRepository.findByUserId(currentUserId);

        return ResponseEntity.ok(conversations);
    }

    // POST /conversations              → open a new conversation { clientId, designerId, jobId }
    @PostMapping
    public ResponseEntity<?> createConversation(@RequestBody Conversation conversation,
                                                Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        String currentUserId = auth.getName();

        if (conversation.clientId == null || conversation.clientId.isBlank()
                || conversation.designerId == null || conversation.designerId.isBlank()
                || conversation.jobId == null || conversation.jobId.isBlank()) {

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "clientId, designerId and jobId are required"
            ));
        }

        boolean currentUserIsParticipant =
                currentUserId.equals(conversation.clientId)
                        || currentUserId.equals(conversation.designerId);

        if (!currentUserIsParticipant) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "current user must be part of the conversation"
            ));
        }

        Conversation savedConversation =
                conversationRepository.create(conversation);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedConversation);
    }

    // GET  /conversations/{id}/messages → fetch messages (paginated)
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String id,
                                         @RequestParam(required = false, defaultValue = "0") int page,
                                         Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        String currentUserId = auth.getName();

        boolean currentUserIsParticipant =
                conversationRepository.isParticipant(id, currentUserId);

        if (!currentUserIsParticipant) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "current user is not part of this conversation"
            ));
        }

        List<Message> messages =
                messageRepository.findByConversationId(id, page);

        return ResponseEntity.ok(messages);
    }

    // POST /conversations/{id}/messages → send a message
    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable String id,
                                         @RequestBody Message message,
                                         Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        String currentUserId = auth.getName();

        boolean currentUserIsParticipant =
                conversationRepository.isParticipant(id, currentUserId);

        if (!currentUserIsParticipant) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "current user is not part of this conversation"
            ));
        }

        if (message.content == null || message.content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "message content is required"
            ));
        }

        message.conversationId = id;
        message.senderId = currentUserId;
        message.flagged = false;

        Message savedMessage = messageRepository.save(message);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedMessage);
    }
}