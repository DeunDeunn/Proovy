package com.deundeun.challenge.service;

import com.deundeun.challenge.dto.response.CategoryResponse;
import com.deundeun.challenge.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return categoryMapper.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
