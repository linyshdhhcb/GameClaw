package ai.gameclaw.chat.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/diff")
public class DiffPreviewController {

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(Map.of(
                "status", "not_implemented",
                "message", "Diff preview will be available in Phase 2 (Playbook 14)"
        ));
    }
}
