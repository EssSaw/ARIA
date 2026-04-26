package esssaw.aria.controller;

import esssaw.aria.dto.request.IdeaCreateRequest;
import esssaw.aria.models.Idea;
import esssaw.aria.service.IdeaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ideas")
@RequiredArgsConstructor
public class IdeaController {
    private final IdeaService ideaService;

    @GetMapping
    public List<Idea> getActiveIdeas() {
        return ideaService.getActiveIdeas();
    }

    @PostMapping
    public Idea createIdea(@RequestBody IdeaCreateRequest request) {
        return ideaService.createIdea(request.content());
    }

    @DeleteMapping("/{id}")
    public void deleteIdea(@PathVariable UUID id) {
        ideaService.deleteIdea(id);
    }

    @PostMapping("/{id}/convert")
    public Idea convertIdea(@PathVariable UUID id) {
        return ideaService.convertIdea(id);
    }

}
