package ru.uncledrema.statistics.dto;

import java.util.List;

public record PageDto<T>(
        int page,
        int size,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        List<T> items
) {
}
