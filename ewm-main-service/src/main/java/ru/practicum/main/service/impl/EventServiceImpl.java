package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.main.controller.PublicEventController;
import ru.practicum.main.dto.*;
import ru.practicum.main.exception.model.DataConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.exception.model.ValidationException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.mapper.LocationMapper;
import ru.practicum.main.mapper.ParticipationRequestMapper;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.*;
import ru.practicum.main.service.EventService;
import ru.practicum.main.service.ParticipationRequestService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final ParticipationRequestService requestService;
    private final LocationRepository locationRepository;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${STAT_SERVER_URL:http://localhost:9090}")
    private String statClientUrl;

    private StatsClient statsClient;

    @PostConstruct
    private void init() {
        statsClient = new StatsClient(statClientUrl);
    }

    @Override
    public EventFullDto addEvent(long userId, NewEventDto newEventDto) {
        checkEventDate(newEventDto.getEventDate());
        User user = checkUserById(userId);
        Category eventCategory = checkCategoryById(newEventDto.getCategory());
        Location eventLocation = getOrSaveLocation(LocationMapper.toLocationDto(newEventDto.getLocation()));
        Event newEvent = EventMapper.toEvent(newEventDto, eventCategory, user, eventLocation, State.PENDING);
        if (newEventDto.getPaid() == null) {
            newEvent.setPaid(false);
        }
        if (newEventDto.getRequestModeration() == null) {
            newEvent.setRequestModeration(true);
        }
        if (newEventDto.getParticipantLimit() == null) {
            newEvent.setParticipantLimit(0);
        }
        newEvent.setViews(0L);
        newEvent.setConfirmedRequests(0L);
        return EventMapper.toFullDto(eventRepository.save(newEvent));
    }

    @Override
    public List<EventFullDto> getAllForAdmin(List<Long> users,
                                             List<State> states,
                                             List<Long> categories,
                                             LocalDateTime rangeStart,
                                             LocalDateTime rangeEnd,
                                             int from,
                                             int size) {
        Pageable pageable = PageRequest.of(from, size);
        if (users != null && users.size() == 1 && users.get(0).equals(0L)) {
            users = null;
        }
        if (categories != null && categories.size() == 1 && categories.get(0).equals(0L)) {
            categories = null;
        }
        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(10);
        }
        Page<Event> page = eventRepository.findAllByAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
        List<String> eventUrls = page.getContent().stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());
        List<ViewStatsDto> viewStatsDtos = statsClient.getStats(rangeStart.format(dateTimeFormatter),
                rangeEnd.format(dateTimeFormatter), eventUrls, true);
        return page.getContent().stream()
                .map(EventMapper::toFullDto)
                .peek(dto -> {
                    Optional<ViewStatsDto> matchingStats = viewStatsDtos.stream()
                            .filter(statsDto -> statsDto.getUri().equals("/events/" + dto.getId()))
                            .findFirst();
                    dto.setViews(matchingStats.map(ViewStatsDto::getHits).orElse(0L));
                })
                .peek(dto -> dto.setConfirmedRequests(participationRequestRepository
                        .countByEventIdAndStatus(dto.getId(), ParticipationRequestStatus.CONFIRMED)))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventShortDto> getAllForInitiator(long userId, int from, int size) {
        checkUserById(userId);
        Pageable page = PageRequest.of(from / size, size);
        return eventRepository.findAllByInitiatorId(userId, page).stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getByIdForInitiator(long userId, long eventId) {
        checkUserById(userId);
        Event event = checkEventById(eventId);
        checkInitiator(userId, eventId, event.getInitiator().getId());
        return EventMapper.toFullDto(event);
    }

    @Override
    public EventFullDto updateByInitiator(long userId, long eventId, UpdateEventUserRequest eventUpdate) {
        checkEventDate(eventUpdate.getEventDate());
        checkUserById(userId);
        Event event = checkEventById(eventId);
        checkInitiator(userId, eventId, event.getInitiator().getId());
        if (!(event.getState() == State.CANCELED || event.getState() == State.PENDING)) {
            throw new DataConflictException("Выбранное событие нельзя изменить");
        }
        if (eventUpdate.getTitle() != null) {
            event.setTitle(eventUpdate.getTitle());
        }
        if (eventUpdate.getDescription() != null) {
            event.setDescription(eventUpdate.getDescription());
        }
        if (eventUpdate.getAnnotation() != null) {
            event.setAnnotation(eventUpdate.getAnnotation());
        }
        if (eventUpdate.getEventDate() != null) {
            event.setEventDate(eventUpdate.getEventDate());
        }
        if (eventUpdate.getPaid() != null) {
            event.setPaid(eventUpdate.getPaid());
        }
        if (eventUpdate.getParticipantLimit() != null) {
            event.setParticipantLimit(eventUpdate.getParticipantLimit());
        }
        if (eventUpdate.getRequestModeration() != null) {
            event.setRequestModeration(eventUpdate.getRequestModeration());
        }
        if (eventUpdate.getStateAction() != null) {
            switch (eventUpdate.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(State.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(State.CANCELED);
                    break;
            }
        }
        if (eventUpdate.getCategory() != null) {
            event.setCategory(checkCategoryById(eventUpdate.getCategory()));
        }
        if (eventUpdate.getLocation() != null) {
            event.setLocation(getOrSaveLocation(eventUpdate.getLocation()));
        }
        return EventMapper.toFullDto(eventRepository.save(event));
    }

    @Override
    public EventRequestStatusUpdate updateRequestsByInitiator(long userId,
                                                              long eventId,
                                                              EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        checkUserById(userId);
        Event event = checkEventById(eventId);
        long confirmLimit = event.getParticipantLimit() - participationRequestRepository
                .countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        if (confirmLimit <= 0) {
            throw new DataConflictException("Достигнут лимит заявок на выбранное событие");
        }
        List<ParticipationRequest> requestList = participationRequestRepository
                .findAllByIdIn(eventRequestStatusUpdateRequest.getRequestIds());
        List<Long> notFoundIds = eventRequestStatusUpdateRequest.getRequestIds().stream()
                .filter(requestId -> requestList.stream().noneMatch(request -> request.getId().equals(requestId)))
                .collect(Collectors.toList());
        if (!notFoundIds.isEmpty()) {
            throw new NotFoundException("Запрос на участие с id: " + notFoundIds + " не найден");
        }
        EventRequestStatusUpdate result = new EventRequestStatusUpdate();
        result.setConfirmedRequests(new ArrayList<>());
        result.setRejectedRequests(new ArrayList<>());
        for (ParticipationRequest req : requestList) {
            if (!req.getEvent().getId().equals(eventId)) {
                throw new NotFoundException("Запрос на участие с id: " + req.getId() + " не найден");
            }
            if (confirmLimit <= 0) {
                req.setStatus(ParticipationRequestStatus.REJECTED);
                result.getRejectedRequests().add(ParticipationRequestMapper.toParticipationRequestDto(req));
                continue;
            }
            switch (eventRequestStatusUpdateRequest.getStatus()) {
                case CONFIRMED:
                    req.setStatus(ParticipationRequestStatus.CONFIRMED);
                    result.getConfirmedRequests().add(ParticipationRequestMapper.toParticipationRequestDto(req));
                    confirmLimit--;
                    break;
                case REJECTED:
                    req.setStatus(ParticipationRequestStatus.REJECTED);
                    result.getRejectedRequests().add(ParticipationRequestMapper.toParticipationRequestDto(req));
                    break;
            }
        }
        participationRequestRepository.saveAll(requestList);
        return result;
    }

    @Override
    public List<ParticipationRequestDto> getParticipationRequestsForInitiator(long userId, long eventId) {
        checkUserById(userId);
        checkEventById(eventId);
        return participationRequestRepository.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventShortDto> getAllForPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, boolean onlyAvailable,
                                               PublicEventController.SortMode sort, int from, int size,
                                               HttpServletRequest request) {
        statsClient.createEndpointHit(EndpointHitDto.builder()
                .app("ewm")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .dtoTimestamp(LocalDateTime.now())
                .build());
        if (categories != null && categories.size() == 1 && categories.get(0).equals(0L)) {
            categories = null;
        }
        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(10);
        }
        List<Event> eventList = eventRepository.getAllByPublic(text, categories, paid, rangeStart, rangeEnd,
                PageRequest.of(0, 10));
        if (onlyAvailable) {
            eventList = eventList.stream()
                    .filter(event -> event.getParticipantLimit().equals(0)
                            || event.getParticipantLimit() < participationRequestRepository
                            .countByEventIdAndStatus(event.getId(), ParticipationRequestStatus.CONFIRMED))
                    .collect(Collectors.toList());
        }
        List<String> eventUrls = eventList.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());
        List<ViewStatsDto> viewStatsDto = statsClient.getStats(rangeStart.format(dateTimeFormatter),
                rangeEnd.format(dateTimeFormatter), eventUrls, true);
        List<EventShortDto> eventShortDtoList = eventList.stream()
                .map(EventMapper::toShortDto)
                .peek(dto -> {
                    Optional<ViewStatsDto> matchingStats = viewStatsDto.stream()
                            .filter(statsDto -> statsDto.getUri().equals("/events/" + dto.getId()))
                            .findFirst();
                    dto.setViews(matchingStats.map(ViewStatsDto::getHits).orElse(0L));
                })
                .peek(dto -> dto.setConfirmedRequests(participationRequestRepository
                        .countByEventIdAndStatus(dto.getId(), ParticipationRequestStatus.CONFIRMED)))
                .collect(Collectors.toList());
        switch (sort) {
            case EVENT_DATE:
                Collections.sort(eventShortDtoList, Comparator.comparing(EventShortDto::getEventDate));
                break;
            case VIEWS:
                Collections.sort(eventShortDtoList, Comparator.comparing(EventShortDto::getViews).reversed());
                break;
        }
        if (from >= eventShortDtoList.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(from + size, eventShortDtoList.size());
        return eventShortDtoList.subList(from, toIndex);
    }

    @Override
    public EventFullDto getByIdForPublic(long eventId, HttpServletRequest request) {
        Event event = checkEventById(eventId);
        if (!event.getState().equals(State.PUBLISHED)) {
            throw new NotFoundException("Событие с id: " + eventId + " не найдено");
        }
        statsClient.createEndpointHit(EndpointHitDto.builder()
                .app("ewm")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .dtoTimestamp(LocalDateTime.now())
                .build());
        List<String> eventUrls = Collections.singletonList("/events/" + event.getId());
        List<ViewStatsDto> viewStatsDto = statsClient.getStats(LocalDateTime.now().minusYears(10)
                        .format(dateTimeFormatter),
                LocalDateTime.now().plusYears(10).format(dateTimeFormatter), eventUrls, true);
        EventFullDto fullDto = EventMapper.toFullDto(event);
        fullDto.setViews(viewStatsDto.isEmpty() ? 0L : viewStatsDto.get(0).getHits());
        fullDto.setConfirmedRequests(participationRequestRepository.countByEventIdAndStatus(fullDto.getId(),
                ParticipationRequestStatus.CONFIRMED));
        return fullDto;
    }

    @Override
    public EventFullDto updateByAdmin(long eventId, UpdateEventAdminRequest eventUpdate) {
        checkEventDate(eventUpdate.getEventDate());
        Event event = checkEventById(eventId);
        if (eventUpdate.getStateAction() != null) {
            if (!State.PENDING.equals(event.getState()))
                throw new DataConflictException("Выбранное событие нельзя изменить");
            switch (eventUpdate.getStateAction()) {
                case PUBLISH_EVENT:
                    event.setState(State.PUBLISHED);
                    event.setPublishedDate(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    event.setState(State.CANCELED);
                    break;
            }
        }
        if (eventUpdate.getTitle() != null) {
            event.setTitle(eventUpdate.getTitle());
        }
        if (eventUpdate.getDescription() != null) {
            event.setDescription(eventUpdate.getDescription());
        }

        if (eventUpdate.getAnnotation() != null) {
            event.setAnnotation(eventUpdate.getAnnotation());
        }
        if (eventUpdate.getEventDate() != null) {
            event.setEventDate(eventUpdate.getEventDate());
        }
        if (eventUpdate.getPaid() != null) {
            event.setPaid(eventUpdate.getPaid());
        }
        if (eventUpdate.getParticipantLimit() != null) {
            event.setParticipantLimit(eventUpdate.getParticipantLimit());
        }
        if (eventUpdate.getRequestModeration() != null) {
            event.setRequestModeration(eventUpdate.getRequestModeration());
        }
        if (eventUpdate.getCategory() != null) {
            event.setCategory(checkCategoryById(eventUpdate.getCategory()));
        }
        if (eventUpdate.getLocation() != null) {
            event.setLocation(getOrSaveLocation(eventUpdate.getLocation()));
        }
        return EventMapper.toFullDto(eventRepository.save(event));
    }

    private void checkEventDate(LocalDateTime dateTime) {
        if (Objects.nonNull(dateTime) && LocalDateTime.now().plusHours(2).isAfter(dateTime)) {
            throw new ValidationException("Между текущим временем и датой проведения мероприятия меньше 2 часов");
        }
    }

    private User checkUserById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id: " + id + " не найден"));
    }

    private void checkInitiator(long userId, Long eventId, long initiatorId) {
        if (userId != initiatorId) {
            throw new NotFoundException("Событие с id: " + eventId + " не найдено");
        }
    }

    private Event checkEventById(long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id: " + id + " не найдено"));
    }

    private Category checkCategoryById(long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id: " + id + " не найдена"));
    }

    private Location getOrSaveLocation(LocationDto locationDto) {
        Location location = locationRepository.findByLatAndLon(locationDto.getLat(), locationDto.getLon());
        return location != null ? location : locationRepository.save(LocationMapper.toLocation(locationDto));
    }
}
