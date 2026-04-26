package esssaw.aria.repository;

import esssaw.aria.enums.TaskStatus;
import esssaw.aria.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findAllByStatusOrderByDueDateAsc(TaskStatus status);
    List<Task> findByDueDateBeforeAndStatus(LocalDateTime duedate, TaskStatus status);
    List<Task> findAllByOrderByCreatedAtDesc();
}
