package at.ac.fhcampuswien.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock ChatService chatService;
    @Mock Authentication auth;

    @InjectMocks ChatController controller;

    @Test
    void listConversations_delegatesWithCurrentUser() {
        when(auth.getName()).thenReturn("u1");
        when(chatService.listConversations("u1")).thenReturn(List.of());

        ResponseEntity<?> response = controller.listConversations(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(chatService).listConversations("u1");
    }

    @Test
    void createConversation_returns201_andDelegates() {
        when(auth.getName()).thenReturn("u1");
        Conversation conv = new Conversation();
        when(chatService.createConversation(any(), eq("u1"))).thenReturn(conv);

        ResponseEntity<?> response = controller.createConversation(conv, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(chatService).createConversation(conv, "u1");
    }

    @Test
    void getMessages_delegatesWithPage() {
        when(auth.getName()).thenReturn("u1");
        when(chatService.getMessages("conv-1", 2, "u1")).thenReturn(List.of());

        controller.getMessages("conv-1", 2, auth);

        verify(chatService).getMessages("conv-1", 2, "u1");
    }

    @Test
    void sendMessage_returns201_andDelegates() {
        when(auth.getName()).thenReturn("u1");
        Message message = new Message();
        when(chatService.sendMessage(eq("conv-1"), any(), eq("u1"))).thenReturn(message);

        ResponseEntity<?> response = controller.sendMessage("conv-1", message, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void unauthenticatedRequest_throws401() {
        assertThatThrownBy(() -> controller.listConversations(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not authenticated");
    }

    @Test
    void exceptionHandler_mapsResponseStatusExceptionToBody() {
        ResponseEntity<?> response = controller.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "nope"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
