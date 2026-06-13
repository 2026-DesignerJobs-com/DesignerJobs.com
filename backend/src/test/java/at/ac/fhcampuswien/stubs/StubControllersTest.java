package at.ac.fhcampuswien.stubs;

import at.ac.fhcampuswien.contract.ContractController;
import at.ac.fhcampuswien.moderation.ModerationController;
import at.ac.fhcampuswien.account.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These controllers are intentional 501 stubs (Phase 2). The tests pin that
 * status so that when a package is implemented, the corresponding assertion
 * fails loudly and forces the author to replace it with real behavior tests.
 */
class StubControllersTest {

    @Test
    void userController_designerEndpoints_areNotImplemented() {
        UserController controller = new UserController();

        assertNotImplemented(controller.listDesigners(null, null));
        assertNotImplemented(controller.getDesignerProfile("id"));
        assertNotImplemented(controller.updateDesignerProfile("id", null));
        assertNotImplemented(controller.getPortfolio("id"));
        assertNotImplemented(controller.addPortfolioItem("id", null));
        assertNotImplemented(controller.deletePortfolioItem("id", "item"));
        assertNotImplemented(controller.getUser("id"));
        assertNotImplemented(controller.deleteUser("id"));
    }

    @Test
    void contractController_endpoints_areNotImplemented() {
        ContractController controller = new ContractController();

        assertNotImplemented(controller.generateContract(Map.of()));
        assertNotImplemented(controller.getContract("id"));
        assertNotImplemented(controller.signContract("id", Map.of()));
    }

    @Test
    void moderationController_endpoints_areNotImplemented() {
        ModerationController controller = new ModerationController();

        assertNotImplemented(controller.reportMessage("id", null));
        assertNotImplemented(controller.reportJob("id", null));
        assertNotImplemented(controller.reportUser("id", null));
        assertNotImplemented(controller.listReports(null));
        assertNotImplemented(controller.resolveReport("id", Map.of()));
    }

    private void assertNotImplemented(ResponseEntity<?> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }
}
