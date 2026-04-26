package ru.uncledrema.statistics.dto;

import java.util.List;

public record PageDto<T>(
        int page,
        int size,
        long totalElements,
        List<T> items
) {
}
