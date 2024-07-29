package com.xuecheng.orders.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单业务层实现类
 *
 * @author reslou
 * @date 2024/07/26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final XcOrdersMapper ordersMapper;
    private final XcOrdersGoodsMapper ordersGoodsMapper;
    private final XcPayRecordMapper payRecordMapper;
    private final MqMessageService mqMessageService;
    private final RabbitTemplate rabbitTemplate;
    @Autowired
    OrderService currentProxy;
    @Value("${pay.qrcodeurl}")
    String qrcodeurl;
    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;
    @Autowired
    private FanoutExchange paynotify_exchange_fanout;

    /**
     * 新增订单
     *
     * @param userId      用户ID
     * @param addOrderDto 添加订单
     * @return 支付记录dto
     */
    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //保存订单
        XcOrders orders = saveXcOrders(userId, addOrderDto);
        //新增支付记录
        XcPayRecord payRecord = createPayRecord(orders);
        //生成二维码
        String url = String.format(qrcodeurl, payRecord.getPayNo());
        String qrCode = null;
        try {
            qrCode = new QRCodeUtil().createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    /**
     * 查询支付结果
     *
     * @param payNo 支付号
     * @return 支付记录DTO
     */
    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //查询支付记录
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XueChengPlusException.cast("请重新点击支付获取二维码");
        }
        if (payRecord.getStatus().equals("601002")) {//支付成功
            return BeanUtil.copyProperties(payRecord, PayRecordDto.class);
        }
        //从支付宝查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        //保存支付结果
        currentProxy.saveAliPayStatus(payStatusDto);
        //重新查询支付记录
        payRecord = getPayRecordByPayno(payNo);
        return BeanUtil.copyProperties(payRecord, PayRecordDto.class);
    }

    /**
     * 按支付号获取支付记录
     *
     * @param payNo 支付号
     * @return 支付记录
     */
    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        return payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
    }

    /**
     * 保存支付宝状态
     *
     * @param payStatusDto 支付状态DTO
     */
    @Transactional
    @Override
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XueChengPlusException.cast("找不到支付记录");
        }
        log.debug("收到支付结果:{},支付记录:{}", payStatusDto, payRecord);
        String tradeStatus = payStatusDto.getTrade_status();
        if (!tradeStatus.equals("TRADE_SUCCESS")) {
            return;
        }
        Float totalPrice = payRecord.getTotalPrice() * 100;
        Float totalAmount = Float.parseFloat(payStatusDto.getTotal_amount()) * 100;
        if (totalPrice.intValue() != totalAmount.intValue()) {
            log.info("校验支付结果失败,支付记录:{},APP_ID:{},totalPrice:{}", payRecord, APP_ID, totalPrice);
            XueChengPlusException.cast("校验支付结果失败");
        }
        log.debug("更新支付结果,支付交易流水号:{},支付结果:{}", payNo, tradeStatus);
        //更新支付记录
        payRecord.setStatus("601002");
        payRecord.setOutPayChannel("Alipay");
        payRecord.setOutPayNo(payStatusDto.getTrade_no());
        payRecord.setPaySuccessTime(LocalDateTime.now());
        payRecordMapper.updateById(payRecord);
        //更新订单
        XcOrders orders = ordersMapper.selectById(payRecord.getOrderId());
        orders.setStatus("600002");//支付成功
        ordersMapper.updateById(orders);
        MqMessage mqMessage = mqMessageService.addMessage(
                PayNotifyConfig.MESSAGE_TYPE, orders.getOutBusinessId(), orders.getOrderType(), null);
        notifyPayResult(mqMessage);
    }

    /**
     * 通知支付结果
     *
     * @param mqMessage mq消息
     */
    @Override
    public void notifyPayResult(MqMessage mqMessage) {
        //生产者确认机制
        Long mqMsgId = mqMessage.getId();
        CorrelationData correlationData = new CorrelationData(mqMsgId.toString());
        String msgId = correlationData.getId();
        correlationData.getFuture().addCallback(
                result -> {
                    if (result.isAck()){//发送成功,删除信息表数据
                        log.debug("通知支付结果消息发送成功，ID:{}", msgId);
                        mqMessageService.completed(mqMsgId);
                    }else {
                        log.error("通知支付结果消息发送失败，ID:{}，原因：{}", msgId,result.getReason());
                    }
                },
                ex->log.error("消息发送异常，ID:{}，原因{}", msgId,ex.getMessage())
        );
        String msgStr = JSON.toJSONString(mqMessage);
        Message message = MessageBuilder.withBody(msgStr.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT,"",message,correlationData);
    }

    /**
     * 查询支付宝支付结果
     *
     * @param payNo 支付号
     * @return 支付结果DTO
     */
    private PayStatusDto queryPayResultFromAlipay(String payNo) {
        //========请求支付宝查询支付结果=============
        AlipayClient alipayClient = new DefaultAlipayClient(
                AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET,
                ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                XueChengPlusException.cast("请求支付查询查询失败");
            }
        } catch (AlipayApiException e) {
            log.error("请求支付宝查询支付结果异常:{}", e.toString(), e);
            XueChengPlusException.cast("请求支付查询查询失败");
        }

        //获取支付结果
        String resultJson = response.getBody();
        //转map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        //支付结果
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");
        //保存支付结果
        return PayStatusDto.builder()
                .out_trade_no(payNo)
                .trade_status(trade_status)
                .app_id(APP_ID)
                .trade_no(trade_no)
                .total_amount(total_amount).build();
    }

    /**
     * 保存订单
     *
     * @param userId      用户ID
     * @param addOrderDto 添加订单DTO
     * @return 订单
     */
    private XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        //幂等性处理
        XcOrders orders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (orders != null) {
            return orders;
        }
        //插入订单和订单商品
        orders = new XcOrders();
        BeanUtils.copyProperties(addOrderDto, orders);
        long orderId = IdWorkerUtils.getInstance().nextId();
        orders.setId(orderId);
        orders.setCreateDate(LocalDateTime.now());
        orders.setStatus("600001");//未支付
        orders.setUserId(userId);
        ordersMapper.insert(orders);
        List<XcOrdersGoods> ordersGoodsList = JSON.parseArray(addOrderDto.getOrderDetail(), XcOrdersGoods.class);
        ordersGoodsList.forEach(goods -> {
            XcOrdersGoods ordersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods, ordersGoods);
            ordersGoods.setOrderId(orderId);
            ordersGoodsMapper.insert(ordersGoods);
        });
        return orders;
    }

    /**
     * 新增支付记录
     *
     * @param orders 订单
     * @return 支付记录
     */
    private XcPayRecord createPayRecord(XcOrders orders) {
        if (orders == null) {
            XueChengPlusException.cast("订单不存在");
        }
        if (orders.getStatus().equals("600002")) {
            XueChengPlusException.cast("订单已支付");
        }
        XcPayRecord payRecord = BeanUtil.copyProperties(orders, XcPayRecord.class);
        payRecord.setOrderId(orders.getId());
        payRecord.setPayNo(IdWorkerUtils.getInstance().nextId());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecordMapper.insert(payRecord);
        return payRecord;
    }

    /**
     * 通过业务ID获取订单
     *
     * @param businessId 业务ID
     * @return 订单
     */
    private XcOrders getOrderByBusinessId(String businessId) {
        return ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>()
                .eq(XcOrders::getOutBusinessId, businessId));
    }
}
