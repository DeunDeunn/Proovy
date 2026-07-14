package com.deundeun.challenge.mapper;

import com.deundeun.challenge.domain.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {
    List<Category> findAll();
}
