package at.ac.fhcampuswien.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// Owner: Bruno — In-platform chat (REST-based, Phase 2)
@RestController
@RequestMapping("/conversations")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // GET  /conversations              → list conversations for current user
    @GetMapping
    public ResponseEntity<?> listConversations(Authentication auth) {
        String currentUserId = getCurrentUserId(auth);

        return ResponseEntity.ok(
                chatService.listConversations(currentUserId)
        );
    }

    // POST /conversations              → open a new conversation { clientId, designerId, jobId }
    @PostMapping
    public ResponseEntity<?> createConversation(@RequestBody Conversation conversation,
                                                Authentication auth) {
        String currentUserId = getCurrentUserId(auth);

        Conversation savedConversation =
                chatService.createConversation(conversation, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedConversation);
    }

    // GET  /conversations/{id}/messages → fetch messages (paginated)
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String id,
                                         @RequestParam(required = false, defaultValue = "0") int page,
                                         Authentication auth) {
        String currentUserId = getCurrentUserId(auth);

        return ResponseEntity.ok(
                chatService.getMessages(id, page, currentUserId)
        );
    }

    // POST /conversations/{id}/messages → send a message
    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable String id,
                                         @RequestBody Message message,
                                         Authentication auth) {
        String currentUserId = getCurrentUserId(auth);

        Message savedMessage =
                chatService.sendMessage(id, message, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedMessage);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "error", exception.getReason() == null ? "request failed" : exception.getReason()
        ));
    }

    private String getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "not authenticated"
            );
        }

        return auth.getName();
    }
}