package esssaw.aria.controller;

import esssaw.aria.dto.request.EventCreateRequest;
import esssaw.aria.dto.request.EventUpdateRequest;
import esssaw.aria.models.Event;
import esssaw.aria.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public List<Event> getEvents(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
                                 ) {
        return eventService.getEvents(start, end);
    }

    @PostMapping
    public Event createEvent(@Valid @RequestBody EventCreateRequest request) {
        return eventService.createEvent(request);
    }

    @PutMapping("/{id}")
    public Event updateEvent(@PathVariable UUID id, @Valid @RequestBody EventUpdateRequest request) {
        return eventService.updateEvent(id,request);
    }

    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
    }

}
