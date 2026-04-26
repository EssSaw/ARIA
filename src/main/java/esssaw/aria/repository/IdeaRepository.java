package esssaw.aria.repository;

import esssaw.aria.models.Idea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IdeaRepository extends JpaRepository<Idea, UUID> {

    List<Idea> findAllByOrderByCreatedAtDesc();
    List<Idea> findByIsConvertedFalseOrderByCreatedAtDesc();
}
