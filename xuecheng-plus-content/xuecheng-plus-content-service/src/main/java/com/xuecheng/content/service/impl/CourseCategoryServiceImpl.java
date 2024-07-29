package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 课程类别服务实现类
 *
 * @author zhangyang
 * @date 2024/07/12
 */
@Service
@RequiredArgsConstructor
public class CourseCategoryServiceImpl implements CourseCategoryService {
    private final CourseCategoryMapper courseCategoryMapper;

    /**
     * 查询树节点
     *
     * @return 列表<课程分类树>
     */
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes() {
        String id = "1";
        List<CourseCategoryTreeDto> list = courseCategoryMapper.selectTreeNodes(id);
        //转为map
        Map<String, CourseCategoryTreeDto> map = list
                .stream()
                .filter(item->!id.equals(item.getId()))
                .collect(Collectors.toMap(CourseCategory::getId, value -> value, (key1, key2) -> key2));
        //返回的list
        List<CourseCategoryTreeDto> resultList = new ArrayList<>();
        list.forEach(
                item -> {
                    //TODO:优化,注释和测试
                    if (!item.getId().equals(id)) {//过滤根节点
                        if (item.getParentid().equals(id)) {//添加二级节点
                            resultList.add(item);
                        }
                        //根据项的父节点id从map中取出父节点的数据
                        CourseCategoryTreeDto parent = map.get(item.getParentid());
                        if (parent != null) {
                            //第一次遍历二级节点的子节点,创建二级节点的子节点树
                            if (parent.getChildrenTreeNodes() == null){
                                parent.setChildrenTreeNodes(new ArrayList<>());
                            }
                            //添加项到子节点树
                            parent.getChildrenTreeNodes().add(item);
                        }
                    }
                }
        );
        return resultList;
    }
}
