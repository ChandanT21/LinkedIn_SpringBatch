package entity;

import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class TrackedOrder extends Order {
    private String trackingNumber;
    private boolean freeShipping;

    public TrackedOrder (Order order) {
        BeanUtils.copyProperties(order, this);
    }
}
