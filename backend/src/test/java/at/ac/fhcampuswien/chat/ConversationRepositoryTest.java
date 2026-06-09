package at.ac.fhcampuswien.chat;

import at.ac.fhcampuswien.testsupport.H2TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationRepositoryTest extends H2TestSupport {

    private ConversationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ConversationRepository();
    }

    private Conversation conversation(String client, String designer, String job) {
        Conversation c = new Conversation();
        c.clientId = client;
        c.designerId = designer;
        c.jobId = job;
        return c;
    }

    @Test
    void create_assignsIdAndTimestamp() {
        Conversation saved = repository.create(conversation("c1", "d1", "j1"));

        assertThat(saved.id).isNotBlank();
        assertThat(saved.createdAt).isNotBlank();
    }

    @Test
    void create_isIdempotent_forSameParticipantsAndJob() {
        Conversation first = repository.create(conversation("c1", "d1", "j1"));
        Conversation second = repository.create(conversation("c1", "d1", "j1"));

        assertThat(second.id).isEqualTo(first.id);
        assertThat(repository.findByUserId("c1")).hasSize(1);
    }

    @Test
    void findByUserId_findsByEitherSide() {
        repository.create(conversation("c1", "d1", "j1"));

        assertThat(repository.findByUserId("c1")).hasSize(1);
        assertThat(repository.findByUserId("d1")).hasSize(1);
        assertThat(repository.findByUserId("stranger")).isEmpty();
    }

    @Test
    void isParticipant_trueForBothPartiesOnly() {
        Conversation saved = repository.create(conversation("c1", "d1", "j1"));

        assertThat(repository.isParticipant(saved.id, "c1")).isTrue();
        assertThat(repository.isParticipant(saved.id, "d1")).isTrue();
        assertThat(repository.isParticipant(saved.id, "stranger")).isFalse();
    }

    @Test
    void isParticipant_falseForUnknownConversation() {
        assertThat(repository.isParticipant("missing", "c1")).isFalse();
    }

    @Test
    void findById_returnsNull_whenMissing() {
        assertThat(repository.findById("missing")).isNull();
    }
}
