package com.vesanrebackend.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Paging {
    private static final int MAX_SIZE = 100;

    private Paging() {
    }

    // Admin queues: oldest first; the id tiebreak keeps pages stable when createdAt collides.
    public static Pageable of(int page, int size, String idProperty) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size >= 1");
        }
        return PageRequest.of(page, Math.min(size, MAX_SIZE), Sort.by("createdAt", idProperty));
    }
}
