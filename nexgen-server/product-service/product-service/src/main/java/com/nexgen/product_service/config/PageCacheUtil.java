package com.nexgen.product_service.config;

import com.nexgen.product_service.dto.RedisPageWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class PageCacheUtil {

    public static <T> RedisPageWrapper<T> wrap(Page<T> page) {
        return new RedisPageWrapper<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    public static <T> Page<T> unwrap(RedisPageWrapper<T> wrapper) {
        return new PageImpl<>(
                wrapper.getContent(),
                PageRequest.of(wrapper.getPageNumber(), wrapper.getPageSize()),
                wrapper.getTotalElements()
        );
    }
}
