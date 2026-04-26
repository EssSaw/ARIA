package esssaw.aria.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventUpdateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 500)
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String description,
        int reminderMinutes
) {
}
