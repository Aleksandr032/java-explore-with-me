package ru.practicum.main.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.dto.NewCommentDto;
import ru.practicum.main.service.CommentService;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/comments")
@RequiredArgsConstructor
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping("/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable Long userId,
                                 @PathVariable Long eventId,
                                 @Valid @RequestBody NewCommentDto newCommentDto) {
        return commentService.addComment(newCommentDto, userId, eventId);
    }

    @GetMapping
    public List<CommentDto> getAllByUserId(@PathVariable Long userId,
                                           @Valid @RequestParam(defaultValue = "0") @Min(0) int from,
                                           @Valid @RequestParam(defaultValue = "10") @Min(10) int size) {
        return commentService.getAllByUserId(userId, from, size);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateByUser(@PathVariable Long userId,
                                   @PathVariable Long commentId,
                                   @Valid @RequestBody NewCommentDto newCommentDto) {
        return commentService.updateByUser(newCommentDto, userId, commentId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByUser(@PathVariable Long userId, @PathVariable Long commentId) {
        commentService.deleteByUser(userId, commentId);
    }
}
