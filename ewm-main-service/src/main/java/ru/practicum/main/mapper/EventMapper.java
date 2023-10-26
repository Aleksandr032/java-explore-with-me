package ru.practicum.main.mapper;

import ru.practicum.main.dto.EventFullDto;
import ru.practicum.main.dto.EventShortDto;
import ru.practicum.main.dto.NewEventDto;
import ru.practicum.main.model.*;

import java.time.LocalDateTime;

public class EventMapper {
    public static Event toEvent(NewEventDto newEventDto,
                                Category category,
                                User user,
                                Location location, State state) {
        return Event.builder()
                .title(newEventDto.getTitle())
                .description(newEventDto.getDescription())
                .annotation(newEventDto.getAnnotation())
                .createDate(LocalDateTime.now())
                .eventDate(newEventDto.getEventDate())
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .publishedDate(LocalDateTime.now())
                .requestModeration(newEventDto.getRequestModeration())
                .state(state)
                .initiator(user)
                .category(category)
                .location(location)
                .build();
    }

    public static EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .initiator(event.getInitiator())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .views(event.getViews())
                .confirmedRequests(event.getConfirmedRequests())
                .build();
    }

    public static EventFullDto toFullDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .annotation(event.getAnnotation())
                .createdOn(event.getCreateDate())
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedDate())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .initiator(event.getInitiator())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .location(event.getLocation())
                .views(event.getViews())
                .confirmedRequests(event.getConfirmedRequests())
                .build();
    }
}
