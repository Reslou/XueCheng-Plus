package com.xuecheng.content.mapper;

import com.xuecheng.content.model.dto.TeachplanDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TeachplanMapperTest {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Test
    void selectTreeNodes() {
        List<TeachplanDto> list = teachplanMapper.selectTreeNodes(117L);
        System.out.println("list = " + list);
    }
}