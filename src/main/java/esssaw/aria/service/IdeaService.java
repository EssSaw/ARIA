package esssaw.aria.service;

import esssaw.aria.models.Idea;
import esssaw.aria.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequestMapping("/api/ideas")
@RequiredArgsConstructor
public class IdeaService {

    @Autowired
    private final IdeaRepository ideaRepository;

    public List<Idea> getActiveIdeas() {
        return ideaRepository.findByIsConvertedFalseOrderByCreatedAtDesc();
    }
    public Idea createIdea(String content) {
        Idea idea = new Idea();
        idea.setContent(content);
        return ideaRepository.save(idea);

    }
    public void deleteIdea(UUID id) {
        ideaRepository.deleteById(id);
    }
    public Idea convertIdea(UUID id){
        Idea idea = ideaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Idea not found"));
        idea.setConverted(true);
        return ideaRepository.save(idea);

    }
}