package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 密码身份验证业务层实现类
 *
 * @author reslou
 * @date 2024/07/24
 */
@Service("password_authservice")
@RequiredArgsConstructor
public class PasswordAuthServiceImpl implements AuthService {

    private final XcUserMapper xcUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final CheckCodeClient checkCodeClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //校验验证码
        String checkcodekey = authParamsDto.getCheckcodekey();
        String checkcode = authParamsDto.getCheckcode();
        if (StringUtils.isBlank(checkcodekey) || StringUtils.isBlank(checkcode)) {
            throw new RuntimeException("验证码为空");
        }
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if (!verify) {
            throw new RuntimeException("验证码输入错误");
        }
        //查询用户数据
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>()
                .eq(XcUser::getUsername, authParamsDto.getUsername()));
        if (xcUser == null) {
            throw new RuntimeException("账号不存在");
        }
        //校验密码
        boolean matches = passwordEncoder.matches(authParamsDto.getPassword(), xcUser.getPassword());
        if (!matches) {
            throw new RuntimeException("账号或密码错误");
        }
        //封装数据
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }
}
