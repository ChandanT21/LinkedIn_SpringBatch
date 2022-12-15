package dev.studios_21.linkedin_springbatch;

import entity.Order;
import entity.TrackedOrder;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;

public class FreeShippingItemProcessor implements ItemProcessor<TrackedOrder, TrackedOrder> {
    @Override
    public TrackedOrder process(TrackedOrder order) throws Exception {
        order.setFreeShipping(order.getCost().compareTo(new BigDecimal("80")) > 0);
        return order.isFreeShipping() ? order : null;
    }
}
