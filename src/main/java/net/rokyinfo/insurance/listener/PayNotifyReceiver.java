package net.rokyinfo.insurance.listener;

import net.rokyinfo.insurance.entity.OrderEntity;
import net.rokyinfo.insurance.entity.PayOrderNotifyMsg;
import net.rokyinfo.insurance.enums.OrderStatus;
import net.rokyinfo.insurance.service.OrderService;
import net.rokyinfo.insurance.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * @author yuanzhijian
 */
@Component
@RabbitListener(queues = "insurance.pay.notify.queue")
public class PayNotifyReceiver {

    private static Logger logger = LoggerFactory.getLogger(PayNotifyReceiver.class);

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void process(String json) {
        PayOrderNotifyMsg payOrderNotifyMsg = JacksonUtil.readValue(json,PayOrderNotifyMsg.class);
        if (payOrderNotifyMsg == null) {
            return;
        }
        logger.info(payOrderNotifyMsg.toString());
        OrderEntity orderEntity = orderService.queryOrderByOrderNo(payOrderNotifyMsg.getMerchantOrderNo());
        if (orderEntity == null) {
            return;
        }
        if (orderEntity.getPrice().compareTo(payOrderNotifyMsg.getOrderAmount()) != 0) {
            logger.warn("完成支付的金额与订单中的订单金额不一致。");
            return;
        }
        OrderEntity updateOrderEntity = new OrderEntity();
        updateOrderEntity.setId(orderEntity.getId());
        //变更订单状态为已支付，待审核
        updateOrderEntity.setStatus(OrderStatus.PAYED_TO_VERIFY.getOrderStatusValue());
        updateOrderEntity.setTrxNo(payOrderNotifyMsg.getTrxNo());
        orderService.update(updateOrderEntity);
    }

}
