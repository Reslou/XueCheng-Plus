package com.xuecheng.orders.model.dto;

import com.xuecheng.orders.model.po.XcPayRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 支付记录dto
 *
 * @author Mr.M
 * @date 2022/10/4 11:30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class PayRecordDto extends XcPayRecord {

    //二维码
    private String qrcode;

}
