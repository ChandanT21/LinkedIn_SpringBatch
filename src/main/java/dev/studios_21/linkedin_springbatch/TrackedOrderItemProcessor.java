package dev.studios_21.linkedin_springbatch;

import entity.Order;
import entity.TrackedOrder;
import org.springframework.batch.item.ItemProcessor;

import java.util.UUID;

public class TrackedOrderItemProcessor implements ItemProcessor<Order, TrackedOrder> {

    @Override
    public TrackedOrder process(Order order) throws Exception {
        TrackedOrder trackedOrder = new TrackedOrder(order);
        trackedOrder.setTrackingNumber(UUID.randomUUID().toString());
        return trackedOrder;
    }
}
