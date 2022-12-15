package dev.studios_21.linkedin_springbatch;

import entity.Order;
import entity.TrackedOrder;

import java.util.List;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;

public class CustomSkipListener implements SkipListener<Order,TrackedOrder> {

    @Override
    public void onSkipInRead(Throwable throwable) {

    }

    @Override
    public void onSkipInWrite(TrackedOrder trackedOrder, Throwable throwable) {

    }

    @Override
    public void onSkipInProcess(Order order, Throwable throwable) {
        System.out.println("Skipping processing of order with id: "+order.getOrderId());
    }
}
