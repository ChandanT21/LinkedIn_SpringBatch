package dev.studios_21.linkedin_springbatch;

import entity.Order;
import entity.TrackedOrder;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.*;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;

@SpringBootApplication
@EnableBatchProcessing
public class LinkedInSpringBatchApplication {

    @Autowired
    public DataSource dataSource;
    public static String ORDER_SQL = "SELECT * from SHIPPED_ORDER ORDER BY order_id";
    public static String[] header = new String[] {"order_id", "first_name","last_name","email","cost","item_id","item_name","ship_date"};
    public static String[] names = new String[] {"orderId", "firstName","lastName","email","cost","itemId","itemName","shipDate"};
    public static String INSERT_ORDER_SQL = "INSERT into SHIPPED_ORDER_OUTPUT(" +
            "order_id, first_name, last_name, email, item_id, item_name, cost, ship_date) " +
            "values(?,?,?,?,?,?,?,?)";
    public static String INSERT_NAMED_ORDER_SQL = "INSERT into SHIPPED_ORDER_OUTPUT(" +
            "order_id, first_name, last_name, email, cost, item_id, item_name, ship_date) " +
            "values(:orderId, :firstName, :lastName, :email, :cost, :itemId, :itemName, :shipDate)";

    public static String INSERT_TRACKED_ORDER_SQL = "INSERT into TRACKED_ORDER(" +
            "order_id, first_name, last_name, email, cost, item_id, item_name, ship_date, tracking_number, free_shipping) " +
            "values(:orderId, :firstName, :lastName, :email, :cost, :itemId, :itemName, :shipDate, :trackingNumber, :freeShipping)";

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public PagingQueryProvider queryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setSelectClause("SELECT order_id, first_name, last_name, email, cost, item_id, item_name, ship_date");
        factoryBean.setFromClause("FROM SHIPPED_ORDER");
        factoryBean.setSortKey("order_id");
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Bean
    public ItemReader<Order> itemReader () throws Exception {
        JdbcPagingItemReader<Order> itemReader = new JdbcPagingItemReader<Order>();
        itemReader.setDataSource(dataSource);
        itemReader.setName("jdbcPagingItemReader");
        itemReader.setQueryProvider(queryProvider());
        itemReader.setRowMapper(new OrderRowMapper());
        itemReader.setPageSize(10); //sync with chunk size
        itemReader.setSaveState(false); //set to False for multi-threading
        return itemReader;
    }

    @Bean
    public ItemReader<Order> itemReader_JdbcCursor_forSingleThread () {
        JdbcCursorItemReader<Order> itemReader = new JdbcCursorItemReader<Order>();
        itemReader.setDataSource(dataSource);
        itemReader.setName("jdbcCursorItemReader");
        itemReader.setSql(ORDER_SQL);
        itemReader.setRowMapper(new OrderRowMapper());
        return itemReader;
    }

    @Bean
    public ItemReader<Order> itemReader_FlatFile() {
        FlatFileItemReader<Order> itemReader = new FlatFileItemReader<>();
        itemReader.setLinesToSkip(1); //skip header
        itemReader.setResource(new ClassPathResource("/data/shipped_orders.csv"));

        DefaultLineMapper<Order> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(header);
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(new OrderFieldSetMapper());
        itemReader.setLineMapper(lineMapper);
        return itemReader;
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
                .start(chunkBasedStep())
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor () {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(11);
        return executor;
    }
    @Bean
    public Step chunkBasedStep() {
        try {
            return this.stepBuilderFactory.get("chunkBasedStep")
                    .<Order,TrackedOrder>chunk(10)
                    .reader(itemReader())
                    .processor(compositeItemProcessor())
                    .faultTolerant()
//                    .retry(OrderProcessingException.class)
//                    .retryLimit(3)
//                    .listener(new CustomRetryListener())
                    .skip(OrderProcessingException.class)
                    .skipLimit(10)
                    .listener(new CustomSkipListener())
                    .writer(itemWriter())
                    .taskExecutor(taskExecutor()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public ItemProcessor<Order,TrackedOrder> compositeItemProcessor() {
        return new CompositeItemProcessorBuilder<Order,TrackedOrder>()
                .delegates(trackedOrderItemProcessor(), freeShippingItemProcessor())
                .build();
    }

    @Bean
    public ItemProcessor<TrackedOrder, TrackedOrder> freeShippingItemProcessor() {
        return new FreeShippingItemProcessor();
    }
    @Bean
    public ItemProcessor<Order, TrackedOrder> trackedOrderItemProcessor() {
        return new TrackedOrderItemProcessor();
    }

//    @Bean
//    public ItemProcessor<Order,Order> orderValidatingItemProcessor() {
//        BeanValidatingItemProcessor<Order> processor = new BeanValidatingItemProcessor<Order>();
//        processor.setFilter(true);
//        return processor;
//    }

    @Bean
    public ItemWriter<Order> itemWriter () {
        return new JdbcBatchItemWriterBuilder<Order>()
                .dataSource(dataSource)
//                .sql(INSERT_NAMED_ORDER_SQL)
                .sql(INSERT_TRACKED_ORDER_SQL)
                .beanMapped()
                .build();
    }
    @Bean
    public ItemWriter<Order> itemWriter_PreparedStatementSetter() {
        return new JdbcBatchItemWriterBuilder<Order>()
                .dataSource(dataSource)
                .sql(INSERT_ORDER_SQL)
                .itemPreparedStatementSetter(new OrderItemPreparedStatementSetter())
                .build();
    }

    @Bean
    public ItemWriter<TrackedOrder> itemWriter_JSONItemFile () {
        return new JsonFileItemWriterBuilder<TrackedOrder>()
                .jsonObjectMarshaller(new JacksonJsonObjectMarshaller<TrackedOrder>())
                .resource(new FileSystemResource("src/main/resources/data/JSON_Output_Order.json"))
                .name("jsonItemWriter")
                .build();
    }
    @Bean
    public ItemWriter<Order> itemWriter_FlatFile() {
        BeanWrapperFieldExtractor<Order> fieldExtractor = new BeanWrapperFieldExtractor<Order>();
        fieldExtractor.setNames(names); // needs to match the POJO field names else it will throw an error

        DelimitedLineAggregator<Order> aggregator = new DelimitedLineAggregator<Order>();
        aggregator.setDelimiter(",");
        aggregator.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<Order> itemWriter = new FlatFileItemWriter<Order>();
        itemWriter.setLineAggregator(aggregator);
        itemWriter.setResource(new FileSystemResource("src/main/resources/data/Output_Order.csv"));
        itemWriter.setAppendAllowed(true);
        itemWriter.setShouldDeleteIfEmpty(true);
        itemWriter.setShouldDeleteIfExists(true);
        return itemWriter;
    }

    public static void main(String[] args) {
        SpringApplication.run(LinkedInSpringBatchApplication.class, args);
    }

}
