package dev.studios_21.linkedin_springbatch;

import dev.studios_21.linkedin_springbatch.DeliverPkgJob.DeliveryDecider;
import dev.studios_21.linkedin_springbatch.DeliverPkgJob.DeliverySatisfaction;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@SpringBootApplication
@EnableBatchProcessing
public class LinkedInSpringBatchApplication {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public JobExecutionDecider receiptDecider() {
        return new ReceiptDecider();
    }

    @Bean
    public StepExecutionListener selectFlowerListener() {
        return new FlowerSelectionStepExecutionListener();
    }

    @Bean
    public Step selectFlowerStep() {
        return this.stepBuilderFactory.get("selectFlowers").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                System.out.println("Gathering flowers for order.");
                return RepeatStatus.FINISHED;
            }
        }).listener(selectFlowerListener()).build();
    }

    @Bean
    public Step removeThornStep() {
        return this.stepBuilderFactory.get("removeThorns").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                System.out.println("Remove thorns from roses.");
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step arrangeFlowerStep() {
        return this.stepBuilderFactory.get("arrangeFlowers").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                System.out.println("Arranging flowers for order.");
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Flow deliveryFlow() {
        return new FlowBuilder<SimpleFlow>("deliveryFlow").start(driveToAddressStep())
                .on("FAILED").to(storePackageStep())
                .from(driveToAddressStep())
                .on("*").to(decider())
                .on("PRESENT").to(givePackageToCustomerStep())
                .next(satisfactory()).on("CORRECT_ITEM").to(thankCustomerStep())
                .from(satisfactory())
                .on("INCORRECT_ITEM").to(refundCustomerStep())
                .from(decider())
                .on("*").to(leavePackageAtTheDoorStep()).build();
    }

    @Bean
    public Job prepareFlowers() {
        return this.jobBuilderFactory.get("prepareFlowersJob")
                .start(selectFlowerStep())
                    .on("TRIM_REQUIRED").to(removeThornStep()).next(arrangeFlowerStep())
                .from(selectFlowerStep())
                    .on("NO_TRIM_REQUIRED").to(arrangeFlowerStep())
                .from(arrangeFlowerStep()).on("*").to(deliveryFlow())
                .end()
                .build();
    }

    @Bean
    public Job deliverPackageJob () {
        return this.jobBuilderFactory.get("deliveryPackageJob")
                .start(packageItemStep())
//                .on("*").to(deliveryFlow())
//                .next(nestedBillingJobStep())
//                Parallel Flow Example...
                .split(new SimpleAsyncTaskExecutor())
                .add(deliveryFlow(), billingFlow())
                .end()
                .build();
    }

    @Bean
    public Flow billingFlow () {
        return new FlowBuilder<SimpleFlow>("billingFLow").start(sendInvoiceStep()).build();
    }

    @Bean
    public Job billingJob () {
        return this.jobBuilderFactory.get("billingJob").start(sendInvoiceStep()).build();
    }

    @Bean
    public Step nestedBillingJobStep() {
        return this.stepBuilderFactory.get("nestedBillingJS").job(billingJob()).build();
    }
    @Bean
    public Step thankCustomerStep() {
        return this.stepBuilderFactory.get("thankCustomer").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
//                String item = chunkContext.getStepContext().getJobParameters().get("item").toString();
//                System.out.printf("Thank you for purchasing %s from us. We appreciate your business.",item);
                System.out.println("Thank you for purchasing from us. We appreciate your business.");
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step refundCustomerStep() {
        return this.stepBuilderFactory.get("refundCustomer").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                String item = chunkContext.getStepContext().getJobParameters().get("item").toString();
                System.out.printf("We are sorry to hear you don't like our %s. Here is your purchase refund." +
                        " We hope you will continue to shop with us.",item);
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step packageItemStep() {
        return this.stepBuilderFactory.get("packageItemStep").tasklet(new Tasklet() {

            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                String item = chunkContext.getStepContext().getJobParameters().get("item").toString();
                String date = chunkContext.getStepContext().getJobParameters().get("run.date").toString();
                System.out.printf("The %s has been packaged on %s.%n", item, date);
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step driveToAddressStep() {
        return this.stepBuilderFactory.get("driveToAddressStep").tasklet(new Tasklet() {
            final boolean gotLost = false;
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                String date = chunkContext.getStepContext().getJobParameters().get("run.date").toString();

                if(gotLost)
                    throw new RuntimeException("Got Lost driving to the address! :( ");

                System.out.printf("Successfully arrived at the address at %s",date);
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step storePackageStep() {
        return this.stepBuilderFactory.get("storePackageStep").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                String item = chunkContext.getStepContext().getJobParameters().get("item").toString();

                System.out.printf("Address not found. Storing the %s package for next trip.", item);
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step givePackageToCustomerStep() {
        return stepBuilderFactory.get("givePackageToCustomer").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
//                String item = chunkContext.getStepContext().getJobParameters().get("item").toString();
//                System.out.printf("%s package has been given to the customer",item);
                System.out.println("Package has been given to the customer.");
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step leavePackageAtTheDoorStep() {
        return stepBuilderFactory.get("leaveAtDoor").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                String item = chunkContext.getStepContext().getJobParameters().get("item").toString();
                System.out.printf("%s package left at the door.",item);
                return RepeatStatus.FINISHED;
            }
        }).build();
    }


    @Bean
    public Step sendInvoiceStep() {
        return this.stepBuilderFactory.get("invoiceStep").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                System.out.println("Invoice is sent to the customer");
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public JobExecutionDecider decider() {
        return new DeliveryDecider();
    }

    @Bean
    public JobExecutionDecider satisfactory() {
        return new DeliverySatisfaction();
    }

    public static void main(String[] args) {
        SpringApplication.run(LinkedInSpringBatchApplication.class, args);
    }

}
