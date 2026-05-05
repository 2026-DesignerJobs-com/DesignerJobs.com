package at.ac.fhcampuswien.contract;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

// Owner: Bruno — Auto-contracting (Phase 2)
@RestController
@RequestMapping("/contracts")
public class ContractController {

    // POST /contracts          → auto-generate contract from accepted application { "applicationId": "..." }
    // GET  /contracts/{id}     → fetch contract (client or designer)
    // PUT  /contracts/{id}/sign → sign contract { "party": "CLIENT"|"DESIGNER" }
    //                            activates when both parties have signed

    @PostMapping
    public ResponseEntity<?> generateContract(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getContract(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PutMapping("/{id}/sign")
    public ResponseEntity<?> signContract(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }
}