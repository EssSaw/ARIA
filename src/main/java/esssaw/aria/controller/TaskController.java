package esssaw.aria.controller;

import esssaw.aria.dto.request.TaskCreateRequest;
import esssaw.aria.models.Task;
import esssaw.aria.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<Task> getPendingTasks() {
        return taskService.getPendingTasks();
    }

    @PostMapping
    public Task createTask(@Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(request);
    }

    @PatchMapping("/{id}/done")
    public Task markTaskAsDone(@PathVariable UUID id) {
        return taskService.markTaskAsDone(id);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
    }

}
