package at.ac.fhcampuswien.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

// Owner: Bruno — In-platform chat (REST-based, Phase 2)
@RestController
@RequestMapping("/conversations")
public class ChatController {

    // GET  /conversations              → list conversations for current user
    // POST /conversations              → open a new conversation { clientId, designerId, jobId }
    // GET  /conversations/{id}/messages → fetch messages (paginated)
    // POST /conversations/{id}/messages → send a message

    @GetMapping
    public ResponseEntity<?> listConversations() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PostMapping
    public ResponseEntity<?> createConversation(@RequestBody Conversation conversation) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "0") int page) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable String id, @RequestBody Message message) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }
}