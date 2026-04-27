package esssaw.aria.service;

import esssaw.aria.dto.request.NoteCreateRequest;
import esssaw.aria.dto.request.NoteUpdateRequest;
import esssaw.aria.models.Note;
import esssaw.aria.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;

    public List<Note> getAllNotes(){
        return noteRepository.findAll();
    }

    public Note getNoteById(UUID id){
        return noteRepository.findById(id).orElse(null);
    }

    public Note createNote(NoteCreateRequest request){
        Note note = new Note();
        note.setTitle(request.title());
        note.setBody(request.body());
        note.setPinned(request.isPinned());
        return noteRepository.save(note);
    }

    public Note updateNote(UUID id,NoteUpdateRequest request){
        Note note = noteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Note not found"));
        note.setTitle(request.title());
        note.setBody(request.body());
        note.setPinned(request.isPinned());
        return noteRepository.save(note);
    }

    public Note autosaveNote(UUID id, NoteUpdateRequest request){
        Note note = noteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Note not found"));
        if (request.title() != null){
            note.setTitle(request.title());
        }
        if (request.body() != null){
            note.setBody(request.body());
        }
        if (request.isPinned() != null){
            note.setPinned(request.isPinned());
        }
        return noteRepository.save(note);
    }

    public Note deleteNote(UUID id){
        Note note = noteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Note not found"));
        noteRepository.delete(note);
    }
}
