package at.ac.fhcampuswien.worldclock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldClockControllerTest {

    @Mock WorldClockService worldClockService;

    @InjectMocks WorldClockController controller;

    @Test
    void getWorldClock_delegatesToService() {
        WorldClockResponse vienna = new WorldClockResponse("Vienna", "Europe/Vienna", "2026-06-10", "12:00", "Wednesday");
        when(worldClockService.getWorldClockTimes()).thenReturn(List.of(vienna));

        List<WorldClockResponse> result = controller.getWorldClock();

        assertThat(result).containsExactly(vienna);
    }
}
