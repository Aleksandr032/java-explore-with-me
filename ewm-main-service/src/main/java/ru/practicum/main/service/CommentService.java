package ru.practicum.main.service;

import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.dto.NewCommentDto;

import java.util.List;

public interface CommentService {
    CommentDto addComment(NewCommentDto dto, Long userId, Long eventId);

    CommentDto updateByUser(NewCommentDto dto, Long userId, Long commentId);

    CommentDto updateByAdmin(NewCommentDto dto, Long commentId);

    List<CommentDto> getAllByUserId(Long userId, int from, int size);

    List<CommentDto> getAllByEventId(Long eventId, int from, int size);

    void deleteByUser(Long userId, Long commentId);

    void deleteByAdmin(Long commentId);
}
