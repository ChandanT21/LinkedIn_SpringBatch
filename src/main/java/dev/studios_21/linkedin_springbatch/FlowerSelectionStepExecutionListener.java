package dev.studios_21.linkedin_springbatch;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class FlowerSelectionStepExecutionListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        System.out.println("Executing before step logic.");

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        System.out.println("Executing after step logic.");
        String flowerType = stepExecution.getJobParameters().getString("type");
        assert flowerType != null;
        return flowerType.equalsIgnoreCase("roses") ? new ExitStatus("TRIM_REQUIRED")
                : new ExitStatus("NO_TRIM_REQUIRED");
    }
}
