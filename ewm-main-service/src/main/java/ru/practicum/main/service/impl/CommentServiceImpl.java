package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.dto.NewCommentDto;
import ru.practicum.main.exception.model.DataConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.mapper.CommentMapper;
import ru.practicum.main.model.Comment;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.State;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CommentRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.CommentService;

import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public CommentDto addComment(NewCommentDto dto, Long userId, Long eventId) {
        User author = checkUserById(userId);
        Event event = checkEventById(eventId);
        if (State.PUBLISHED != event.getState())
            throw new DataConflictException("Выбранное событие ещё не опубликовано");
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setText(dto.getText());
        comment.setCreated(LocalDateTime.now());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    public CommentDto updateByUser(NewCommentDto dto, Long userId, Long commentId) {
        User user = checkUserById(userId);
        Comment comment = checkCommentById(commentId);
        if (!comment.getAuthor().getId().equals(user.getId()))
            throw new DataConflictException("Ошибка. Только автору сообщения или администратору доступно редактирование");
        comment.setText(dto.getText());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    public CommentDto updateByAdmin(NewCommentDto dto, Long commentId) {
        Comment comment = checkCommentById(commentId);
        comment.setText(dto.getText());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }


    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getAllByUserId(Long userId, int from, int size) {
        User author = checkUserById(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        return commentRepository.findAllByAuthor(author, pageable).stream().map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getAllByEventId(Long eventId, int from, int size) {
        Event event = checkEventById(eventId);
        Pageable pageable = PageRequest.of(from / size, size);
        return commentRepository.findAllByEventId(event.getId(), pageable).stream().map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByUser(Long userId, Long commentId) {
        User author = checkUserById(userId);
        Comment comment = checkCommentById(commentId);
        if (!comment.getAuthor().getId().equals(author.getId()))
            throw new DataConflictException("Ошибка. Только автору сообщения или администратору доступно удаление");
        commentRepository.delete(comment);
    }

    @Override
    public void deleteByAdmin(Long commentId) {
        Comment comment = checkCommentById(commentId);
        commentRepository.delete(comment);
    }

    private Comment checkCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Комментарий с id: " + id + " не найден"));
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
