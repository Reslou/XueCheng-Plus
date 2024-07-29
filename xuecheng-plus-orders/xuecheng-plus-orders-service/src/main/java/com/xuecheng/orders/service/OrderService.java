package com.xuecheng.orders.service;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;

/**
 * 订单业务层
 *
 * @author reslou
 * @date 2024/07/26
 */
public interface OrderService {


    /**
     * 新增订单
     *
     * @param userId      用户id
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付记录(包括二维码)
     */
    PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);

    /**
     * 请求支付宝查询支付结果
     *
     * @param payNo 支付记录id
     * @return 支付记录信息
     */
    PayRecordDto queryPayResult(String payNo);

    /**
     * @param payNo 交易记录号
     * @return com.xuecheng.orders.model.po.XcPayRecord
     * @description 查询支付记录
     * @author Mr.M
     * @date 2022/10/20 23:38
     */
    XcPayRecord getPayRecordByPayno(String payNo);

    /**
     * 保存支付宝状态
     *
     * @param payStatusDto 支付结果DTO
     */
    void saveAliPayStatus(PayStatusDto payStatusDto);

    /**
     * 发送通知结果
     *
     * @param mqMessage 消息
     */
    void notifyPayResult(MqMessage mqMessage);
}