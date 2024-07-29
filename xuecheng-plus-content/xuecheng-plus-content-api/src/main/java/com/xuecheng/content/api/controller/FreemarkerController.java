package com.xuecheng.content.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * freemarker控制层
 *
 * @author 张杨
 * @date 2024/07/20
 */
@Controller
public class FreemarkerController {
    @GetMapping("/testfreemarker")
    public ModelAndView test() {
        ModelAndView modelAndView = new ModelAndView();
        //设置模型数据
        modelAndView.addObject("name", "reslou");
        //设置模板名称
        modelAndView.setViewName("test");
        return modelAndView;
    }
}
