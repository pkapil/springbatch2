package com.example.demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


@EnableBatchProcessing
@EnableScheduling
@SpringBootApplication(
        exclude={DataSourceTransactionManagerAutoConfiguration.class,
                BatchAutoConfiguration.class, DataSourceAutoConfiguration.class})
@Import({CustomBatchConfigurer.class})
public class DemoApplication {


    @Autowired
    JobRepository jobRepository;

    public static void main(String[] args) {
                SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public PersonReader itemReader() {
        return new PersonReader();
    }

    @Bean
    public AsyncItemProcessor<Person, Person> itemProcessor() {
        AsyncItemProcessor<Person, Person> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(person -> {

            Thread.sleep(10000);
        	return person;
		});
        return asyncItemProcessor;
    }


    @Bean(name = "asyncPoolTestCaseTaskExecutor")
    public TaskExecutor asyncPoolTestCaseTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(200);
        taskExecutor.setCorePoolSize(100);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setKeepAliveSeconds(100);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setThreadNamePrefix("TEST_");
        taskExecutor.initialize();
        return taskExecutor;
    }



    @Bean
    public JobLauncher asyncJobLauncher() throws Exception {
        SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
        simpleJobLauncher.setJobRepository(jobRepository);
        simpleJobLauncher.setTaskExecutor(asyncPoolTestCaseTaskExecutor());
        simpleJobLauncher.afterPropertiesSet();
        return simpleJobLauncher;
    }


    @Bean
    public AsyncItemWriter<Person> itemWriter() {
        AsyncItemWriter<Person> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(System.out::println);
        return asyncItemWriter;
    }

    @Bean
    public Job job(JobBuilderFactory jobs, StepBuilderFactory steps) {
        return jobs.get("job")
                .start(steps.get("step")
                        .<Person, Future<Person>>chunk(3)
                        .reader(itemReader())
                        .processor(itemProcessor())
                        .writer(itemWriter())
                        .build())
                .build();
    }

    @Bean
    public DataSource dataSource() throws SQLException {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriver(new com.mysql.jdbc.Driver());
        dataSource.setUrl("jdbc:mysql://192.168.0.207:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        return dataSource;
    }

    @Bean
    public BatchProperties batchProperties(){
        return new BatchProperties();
    }

    @Bean
    public BatchDataSourceInitializer batchDataSourceInitializer(@Qualifier("dataSource") DataSource dataSource,
                                                                 ResourceLoader resourceLoader, BatchProperties properties) {
        return new BatchDataSourceInitializer(dataSource, resourceLoader,properties);
    }


    @Bean
    public JobOperator jobOperator(final JobLauncher jobLauncher, final JobRepository jobRepository,
                                   final JobRegistry jobRegistry, final JobExplorer jobExplorer) {
        final SimpleJobOperator jobOperator = new SimpleJobOperator();
        jobOperator.setJobLauncher(jobLauncher);
        jobOperator.setJobRepository(jobRepository);
        jobOperator.setJobRegistry(jobRegistry);
        jobOperator.setJobExplorer(jobExplorer);
        return jobOperator;
    }
}