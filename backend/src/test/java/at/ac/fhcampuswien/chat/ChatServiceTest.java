package at.ac.fhcampuswien.chat;

import at.ac.fhcampuswien.external.ExternalChatApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock ExternalChatApiClient externalChatApiClient;

    @InjectMocks ChatService chatService;

    private Conversation conversation(String client, String designer, String job) {
        Conversation c = new Conversation();
        c.clientId = client;
        c.designerId = designer;
        c.jobId = job;
        return c;
    }

    // ---- createConversation ----

    @Test
    void createConversation_rejectsMissingFields() {
        assertThatThrownBy(() -> chatService.createConversation(new Conversation(), "c1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("required");
    }

    @Test
    void createConversation_rejectsNonParticipant() {
        Conversation req = conversation("c1", "d1", "j1");

        assertThatThrownBy(() -> chatService.createConversation(req, "stranger"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("part of the conversation");

        verify(conversationRepository, never()).create(any());
    }

    @Test
    void createConversation_persists_whenCurrentUserIsParticipant() {
        Conversation req = conversation("c1", "d1", "j1");
        when(conversationRepository.create(req)).thenReturn(req);

        Conversation result = chatService.createConversation(req, "d1");

        assertThat(result).isSameAs(req);
        verify(conversationRepository).create(req);
    }

    // ---- sendMessage ----

    @Test
    void sendMessage_rejectsBlankContent() {
        Message message = new Message();
        message.content = "  ";

        assertThatThrownBy(() -> chatService.sendMessage("conv-1", message, "u1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("content is required");
    }

    @Test
    void sendMessage_rejectsNonParticipant() {
        Message message = new Message();
        message.content = "hi";
        when(conversationRepository.isParticipant("conv-1", "stranger")).thenReturn(false);

        assertThatThrownBy(() -> chatService.sendMessage("conv-1", message, "stranger"))
                .isInstanceOf(ResponseStatusException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_forcesServerSetSenderAndConversation() {
        Message message = new Message();
        message.content = "hi";
        message.senderId = "SPOOFED";          // must be ignored
        message.conversationId = "SPOOFED";     // must be ignored
        when(conversationRepository.isParticipant("conv-1", "u1")).thenReturn(true);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chatService.sendMessage("conv-1", message, "u1");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message saved = captor.getValue();
        assertThat(saved.senderId).isEqualTo("u1");
        assertThat(saved.conversationId).isEqualTo("conv-1");
        assertThat(saved.flagged).isFalse();
    }

    // ---- getMessages ----

    @Test
    void getMessages_rejectsNonParticipant() {
        when(conversationRepository.isParticipant("conv-1", "stranger")).thenReturn(false);

        assertThatThrownBy(() -> chatService.getMessages("conv-1", 0, "stranger"))
                .isInstanceOf(ResponseStatusException.class);

        verify(messageRepository, never()).findByConversationId(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void getMessages_delegatesToRepository_forParticipant() {
        when(conversationRepository.isParticipant("conv-1", "u1")).thenReturn(true);
        when(messageRepository.findByConversationId("conv-1", 0)).thenReturn(java.util.List.of());

        chatService.getMessages("conv-1", 0, "u1");

        verify(messageRepository).findByConversationId("conv-1", 0);
    }

    // ---- listConversations ----

    @Test
    void listConversations_delegatesToRepository() {
        when(conversationRepository.findByUserId("u1")).thenReturn(java.util.List.of());

        chatService.listConversations("u1");

        verify(conversationRepository).findByUserId("u1");
        verify(externalChatApiClient, never()).listConversations(any());
    }
}
