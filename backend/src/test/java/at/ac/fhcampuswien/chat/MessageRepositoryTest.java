package at.ac.fhcampuswien.chat;

import at.ac.fhcampuswien.testsupport.H2TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageRepositoryTest extends H2TestSupport {

    private MessageRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MessageRepository();
    }

    private Message message(String conversationId, String sender, String content) {
        Message m = new Message();
        m.conversationId = conversationId;
        m.senderId = sender;
        m.content = content;
        return m;
    }

    @Test
    void save_assignsId_timestamp_andClearsFlag() {
        Message saved = repository.save(message("conv-1", "u1", "hello"));

        assertThat(saved.id).isNotBlank();
        assertThat(saved.createdAt).isNotBlank();
        assertThat(saved.flagged).isFalse();
    }

    @Test
    void findByConversationId_returnsOnlyThatConversation() {
        repository.save(message("conv-1", "u1", "in conv 1"));
        repository.save(message("conv-2", "u2", "in conv 2"));

        List<Message> result = repository.findByConversationId("conv-1", 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content).isEqualTo("in conv 1");
    }

    @Test
    void findByConversationId_paginatesFiftyPerPage() {
        for (int i = 0; i < 60; i++) {
            repository.save(message("conv-1", "u1", "msg " + i));
        }

        assertThat(repository.findByConversationId("conv-1", 0)).hasSize(50);
        assertThat(repository.findByConversationId("conv-1", 1)).hasSize(10);
    }

    @Test
    void findByConversationId_negativePageTreatedAsFirstPage() {
        repository.save(message("conv-1", "u1", "only"));

        assertThat(repository.findByConversationId("conv-1", -5)).hasSize(1);
    }
}
