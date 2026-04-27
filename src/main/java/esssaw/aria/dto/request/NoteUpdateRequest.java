package esssaw.aria.dto.request;

import jakarta.validation.constraints.Size;

public record NoteUpdateRequest(
        @Size(max = 500)
        String title,
        String body,
        Boolean isPinned
) {
}
