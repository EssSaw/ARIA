package esssaw.aria.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TaskCreateRequest (
        @NotBlank(message = "Title is required")
        @Size(max = 500)
        String title,
        LocalDateTime dueDate
){ }
