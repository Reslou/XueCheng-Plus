package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义UserDetailsService
 *
 * @author reslou
 * @date 2024/07/23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserDetailsService {
    private final ApplicationContext applicationContext;
    private final XcMenuMapper xcMenuMapper;

    /**
     * 按用户名加载用户
     *
     * @param s 用户名
     * @return 用户详细信息
     * @throws UsernameNotFoundException 用户名未发现异常
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        //JSON 转 AuthParamsDto
        AuthParamsDto authParamsDto;
        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            log.info("认证请求不符合项目要求：{}", s);
            throw new RuntimeException("认证请求数据格式不对");
        }
        //策略模式
        //根据 AuthType 选择不同的方法进行身份验证
        String serviceName = authParamsDto.getAuthType() + "_authservice";
        AuthService authService = applicationContext.getBean(serviceName, AuthService.class);
        XcUserExt xcUserExt = authService.execute(authParamsDto);
        return getUserPrincipal(xcUserExt);
    }

    //将XcUserExt封装成UserDetails
    private UserDetails getUserPrincipal(XcUserExt xcUserExt) {
        //查询用户权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(xcUserExt.getId());
        ArrayList<String> permissions = new ArrayList<>();
        if (xcMenus.isEmpty()){
            permissions.add("p1");
        }else {
            xcMenus.forEach(m->permissions.add(m.getCode()));
        }
        xcUserExt.setPermissions(permissions);
        String password = xcUserExt.getPassword();
        xcUserExt.setPassword(null);
        return User.withUsername(JSON.toJSONString(xcUserExt))
                .password(password)
                .authorities(permissions.toArray(new String[0])).build();
    }
}
