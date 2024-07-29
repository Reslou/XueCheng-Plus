package com.xuecheng.learning.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * 接收支付通知业务层
 *
 * @author 张杨
 * @date 2024/07/27
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReceivePayNotifyService {
    private final MyCourseTablesService myCourseTablesService;

    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message) {
        //失败重试等待
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //获取消息
        MqMessage mqMessage = JSON.parseObject(message.getBody(), MqMessage.class);
        log.debug("学习中心服务接收支付结果:{}", mqMessage);
        //判断消息类型和2号业务关键词
        if (PayNotifyConfig.MESSAGE_TYPE.equals(mqMessage.getMessageType())
                && "60201".equals(mqMessage.getBusinessKey2())) {
            String chooseCourseId = mqMessage.getBusinessKey1();
            boolean save = myCourseTablesService.saveChooseCourseSuccess(chooseCourseId);
            if (!save){
                //添加失败,抛出异常,信息重回队列
                XueChengPlusException.cast("收到支付结果，添加选课失败");
            }
        }
    }
}
