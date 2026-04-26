package esssaw.aria.service;

import esssaw.aria.dto.request.TaskCreateRequest;
import esssaw.aria.enums.TaskStatus;
import esssaw.aria.models.Task;
import esssaw.aria.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public List<Task> getPendingTasks() {
        return taskRepository.findAllByStatusOrderByDueDateAsc(TaskStatus.TODO);
    }

    public Task createTask(TaskCreateRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDueDate(request.dueDate());
        return taskRepository.save(task);
    }

    public Task markTaskASDone(UUID id){
        Task task = taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        task.setStatus(TaskStatus.DONE);
        return taskRepository.save(task);
    }

    public void deleteTask(UUID id){
        taskRepository.deleteById(id);
    }
}
