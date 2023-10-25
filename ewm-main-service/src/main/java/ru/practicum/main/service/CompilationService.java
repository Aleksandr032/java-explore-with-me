package ru.practicum.main.service;

import ru.practicum.main.dto.CompilationDto;
import ru.practicum.main.dto.NewCompilationDto;
import ru.practicum.main.dto.UpdateCompilationDto;

import java.util.List;

public interface CompilationService {
    CompilationDto addCompilation(NewCompilationDto newCompilationDtoDto);

    void deleteCompilationById(Long compilationId);

    CompilationDto getById(Long id);

    List<CompilationDto> getAll(Boolean pinned, int from, int size);

    CompilationDto updateCompilation(Long id, UpdateCompilationDto updateCompilationDto);
}
