package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.mapper.EndpointHitMapper;
import ru.practicum.stats.server.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final EndpointHitRepository endpointHitRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto endpointHitDto) {
        endpointHitRepository.save(EndpointHitMapper.toEndpointHit(endpointHitDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (end != null && start != null) {
            if (!end.isAfter(start)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверная дата");
            }
        }

        List<ViewStatsDto> list;
        if (unique) {
            list = endpointHitRepository.getUniqueStats(start, end, uris);
        } else {
            list = endpointHitRepository.getStats(start, end, uris);
        }
        return list.stream()
                .map(statsDto -> ViewStatsDto.builder()
                        .app(statsDto.getApp())
                        .uri(statsDto.getUri())
                        .hits(statsDto.getHits())
                        .build())
                .collect(Collectors.toList());
    }
}
