package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.ParticipationRequestDto;
import ru.practicum.main.exception.model.DataConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.mapper.ParticipationRequestMapper;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ParticipationRequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final UserRepository userRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User user = checkUserById(userId);
        Event event = checkEventById(eventId);
        if (event.getInitiator().getId().equals(userId)) {
            throw new DataConflictException("Ошибка. Инициатор мероприятия не может подать заявку на участие в " +
                    "мероприятии");
        }
        if (!event.getState().equals(State.PUBLISHED)) {
            throw new DataConflictException("Ошибка. Данное мероприятие не опубликовано");
        }
        if (event.getParticipantLimit() > 0) {
            if (event.getParticipantLimit() <= requestRepository.countByEventIdAndStatus(eventId,
                    ParticipationRequestStatus.CONFIRMED)) {
                throw new DataConflictException("Для данного мероприятия достигнут лимит заявок на участие");
            }
        }
        ParticipationRequest participationRequest = new ParticipationRequest();
        participationRequest.setRequester(user);
        participationRequest.setEvent(event);
        participationRequest.setCreated(LocalDateTime.now());
        participationRequest.setStatus(event.getRequestModeration() && !event.getParticipantLimit().equals(0) ?
                ParticipationRequestStatus.PENDING : ParticipationRequestStatus.CONFIRMED);
        return ParticipationRequestMapper.toParticipationRequestDto(requestRepository.save(participationRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getAllRequests(Long userId) {
        checkUserById(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        checkUserById(userId);
        ParticipationRequest participationRequest = checkParticipationRequestById(requestId);
        if (!participationRequest.getRequester().getId().equals(userId)) {
            throw new NotFoundException("События для редактирования не найдены");
        }
        participationRequest.setStatus(ParticipationRequestStatus.CANCELED);
        return ParticipationRequestMapper.toParticipationRequestDto(requestRepository.save(participationRequest));
    }

    private ParticipationRequest checkParticipationRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заявка на участие с id: " + id + " не найдена"));
    }

    private User checkUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id: " + id + " не найден"));
    }

    private Event checkEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id: " + id + " не найдено"));
    }
}
