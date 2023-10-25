package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.main.dto.CompilationDto;
import ru.practicum.main.dto.NewCompilationDto;
import ru.practicum.main.dto.UpdateCompilationDto;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.mapper.CompilationMapper;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.CompilationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    public CompilationDto addCompilation(NewCompilationDto newCompilationDtoDto) {
        List<Event> events = new ArrayList<>();
        if (newCompilationDtoDto.getEvents() != null && !newCompilationDtoDto.getEvents().isEmpty()) {
            events = eventRepository.findAllByIdIn(newCompilationDtoDto.getEvents());
        }
        if (newCompilationDtoDto.getPinned() == null) {
            newCompilationDtoDto.setPinned(false);
        }
        Compilation compilation = compilationRepository
                .save(CompilationMapper.toCompilation(newCompilationDtoDto, events));
        return getById(compilation.getId());
    }

    @Override
    public void deleteCompilationById(Long compilationId) {
        if (!compilationRepository.existsById(compilationId)) {
            throw new NotFoundException("Подборка с id: " + compilationId + " не найдена");
        }
        compilationRepository.deleteById(compilationId);
    }

    @Override
    public CompilationDto getById(Long id) {
        return CompilationMapper.toCompilationDto(compilationRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Подборка с id: " + id + " не найдена")));
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from, size);
        return compilationRepository.getAllCompilation(pinned, pageable).stream()
                .map(CompilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto updateCompilation(Long id, UpdateCompilationDto updateCompilationDto) {
        Compilation compilation = compilationRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Подборка с id: " + id + " не найдена"));
        if (updateCompilationDto.getEvents() != null) {
            compilation.setEvents(eventRepository.findAllById(updateCompilationDto.getEvents()));
        }
        Optional.ofNullable(updateCompilationDto.getTitle()).ifPresent(compilation::setTitle);
        Optional.ofNullable(updateCompilationDto.getPinned()).ifPresent(compilation::setPinned);
        return CompilationMapper.toCompilationDto(compilationRepository.save(compilation));
    }
}
