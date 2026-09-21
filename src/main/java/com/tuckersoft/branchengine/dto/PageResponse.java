package com.tuckersoft.branchengine.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Respuesta paginada con la forma que suelen pedir los enunciados:
 * { content, totalElements, totalPages, currentPage, size }.
 *
 * Uso en el service (mapeando la entidad al DTO ANTES de devolver):
 *   Page<Tropel> page = repository.findAll(spec, PageRequest.of(page, size));
 *   return PageResponse.from(page.map(this::toDto));
 *
 * Ojo: "currentPage" es el numero de pagina base 0 (el mismo que se envia en ?page=).
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
