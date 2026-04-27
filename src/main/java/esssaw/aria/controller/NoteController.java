package esssaw.aria.controller;

import esssaw.aria.dto.request.NoteCreateRequest;
import esssaw.aria.dto.request.NoteUpdateRequest;
import esssaw.aria.models.Note;
import esssaw.aria.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;

    @GetMapping
    public List<Note> getAllNotes() {
        return noteService.getAllNotes();
    }

    @GetMapping("/{id}")
    public Note getNoteById(@PathVariable UUID id) {
        return noteService.getNoteById(id);
    }

    @PostMapping
    public Note createNote(@Valid @RequestBody NoteCreateRequest request) {
        return noteService.createNote(request);
    }

    @PutMapping("/{id}")
    public Note updateNote(@PathVariable UUID id,@Valid @RequestBody NoteUpdateRequest request) {
        return noteService.updateNote(id, request);
    }

    @PatchMapping("/{id}")
    public Note autosaveNote(@PathVariable UUID id,@Valid @RequestBody NoteUpdateRequest request) {
        return noteService.autosaveNote(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteNote(@PathVariable UUID id) {
        noteService.deleteNote(id);
    }
}
