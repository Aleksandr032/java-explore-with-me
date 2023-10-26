package ru.practicum.main.service;

import ru.practicum.main.dto.*;
import ru.practicum.main.model.State;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    EventFullDto addEvent(long userId, NewEventDto newEventDto);

    public List<EventFullDto> getAllForAdmin(List<Long> users, List<State> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    List<EventShortDto> getAllForInitiator(long userId, int from, int size);

    EventFullDto getByIdForInitiator(long userId, long eventId);

    List<ParticipationRequestDto> getParticipationRequestsForInitiator(long userId, long eventId);

    List<EventShortDto> getAllForPublic(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd, boolean onlyAvailable,
                                        SortMode sort,
                                        int from, int size, HttpServletRequest request);

    EventFullDto getByIdForPublic(long eventId, HttpServletRequest request);

    EventFullDto updateByAdmin(long eventId, UpdateEventAdminRequest eventUpdate);

    EventFullDto updateByInitiator(long userId, long eventId, UpdateEventUserRequest eventUpdate);

    EventRequestStatusUpdate updateRequestsByInitiator(long userId,
                                                       long eventId,
                                                       EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest);
}