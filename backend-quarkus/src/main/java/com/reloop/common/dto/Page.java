package com.reloop.common.dto;

import java.util.List;

/** Generic pagination envelope for search endpoints. */
public record Page<T>(
    List<T> items,
    long total,
    int page,
    int size
) {}
