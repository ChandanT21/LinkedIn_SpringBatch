package dev.studios_21.linkedin_springbatch.DeliverPkgJob;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

public class DeliverySatisfaction implements JobExecutionDecider {
    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        String result = Math.random() > 0.3 ? "CORRECT_ITEM":"INCORRECT_ITEM";
        System.out.println("The delivered item is the: " + result);
        return new FlowExecutionStatus(result);
    }
}
