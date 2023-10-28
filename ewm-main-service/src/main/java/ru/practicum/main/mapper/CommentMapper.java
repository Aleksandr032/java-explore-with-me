package ru.practicum.main.mapper;

import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.model.Comment;

public class CommentMapper {
    public static CommentDto toCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .author(comment.getAuthor().getName())
                .text(comment.getText())
                .createDate(comment.getCreated())
                .build();
    }
}
