package ru.practicum.main.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.service.CommentService;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping(path = "/comments")
@RequiredArgsConstructor
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping("/{eventId}")
    public List<CommentDto> getAllByEventId(@PathVariable Long eventId,
                                            @Valid @RequestParam(defaultValue = "0") @Min(0) int from,
                                            @Valid @RequestParam(defaultValue = "10") @Min(10) int size) {
        return commentService.getAllByEventId(eventId, from, size);
    }
}
