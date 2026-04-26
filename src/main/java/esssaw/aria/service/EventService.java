package esssaw.aria.service;

import esssaw.aria.dto.request.EventCreateRequest;
import esssaw.aria.dto.request.EventUpdateRequest;
import esssaw.aria.models.Event;
import esssaw.aria.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public List<Event> getEvents(LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByStartTimeBetweenOrderByStartTimeAsc(start, end);
    }

    public Event getEventById(UUID id) {
        return eventRepository.findById(id).orElse(null);
    }

    public Event createEvent(EventCreateRequest request) {
        Event event = new Event();
        event.setTitle(request.title());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setDescription(request.description());
        event.setReminderMinutes(request.reminderMinutes());
        event.setSource(request.source());
        return eventRepository.save(event);
    }

    public Event updateEvent(UUID id, EventUpdateRequest request) {
        Event event = eventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Event not found"));

        event.setTitle(request.title());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setDescription(request.description());
        event.setReminderMinutes(request.reminderMinutes());

        return eventRepository.save(event);
    }

    public void deleteEvent(UUID id) {
        eventRepository.deleteById(id);
    }
}
