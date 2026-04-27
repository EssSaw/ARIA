package esssaw.aria.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteCreateRequest(
        @NotBlank
        @Size(max = 500)
        String title,
        String body,
        Boolean isPinned
) {
}
