package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@Slf4j
public class FrontController {
    @Autowired
    private JobLauncher asyncJobLauncher;
    @Autowired
    private Job job;
    @Autowired
    private JobRegistry jobRegistry;
    @Autowired
    private JobExplorer jobExplorer;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private JobOperator jobOperator;

    @GetMapping("/")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }


    @GetMapping("/invokejob")
    public ResponseEntity<String> getData() {

        try {
            JobParameters jobParameters = new JobParametersBuilder().addString(
                    "jobid", UUID.randomUUID().toString()).toJobParameters();
            asyncJobLauncher.run(job, jobParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok("Test");
    }

    @Scheduled(cron = "*/30 * * * * *")
    public void restartUncompletedJobs() {
        try {

            try {
                jobRegistry.getJob("job");
            } catch (NoSuchJobException e) {
                jobRegistry.register(new ReferenceJobFactory(job));
            }

            log.info("isRestartable: {} ", job.isRestartable());
            List<JobInstance> lastExecutedJobs = jobExplorer.getJobInstances("job", 0, Integer.MAX_VALUE);
//            1)Get the job instances of your job with JobOperator#gList<JobInstance> lastExecutedJobs = jobExplorer.getJobInstances(jobName, 0, Integer.MAX_VALUE);etJobInstances
//            For each instance, check if there is a running execution using JobOperator#getExecutions.
//            2.1 If there is a running execution, move to next instance (in order to let the execution finish either successfully or with a failure)
//
//            2.2 If there is no currently running execution, check the status of the last execution and restart it if failed using JobOperator#restart.
            for (JobInstance jobInstance : lastExecutedJobs) {
                JobExecution lstJob = jobExplorer.getLastJobExecution(jobInstance);

                if (lstJob.getStatus().getBatchStatus().equals(javax.batch.runtime.BatchStatus.FAILED)||
                        lstJob.getStatus().getBatchStatus().equals(javax.batch.runtime.BatchStatus.STOPPED)||
                        lstJob.getStatus().getBatchStatus().equals(javax.batch.runtime.BatchStatus.STARTED)||
                        lstJob.getStatus().getBatchStatus().equals(javax.batch.runtime.BatchStatus.STOPPING)) {
                    log.info("unsceexful {} ", lstJob);

                    Collection<StepExecution> stepExecutions = lstJob.getStepExecutions();
                    for (StepExecution stepExecution : stepExecutions) {
                        BatchStatus status = stepExecution.getStatus();
                        if (status.isRunning() || status == BatchStatus.STOPPING) {
                            stepExecution.setStatus(BatchStatus.STOPPED);
                            stepExecution.setEndTime(new Date());
                            jobRepository.update(stepExecution);
                        }
                    }

                    lstJob.setStatus(BatchStatus.STOPPED);
                    lstJob.setEndTime(new Date());
                    jobRepository.update(lstJob);
                    Long jobExecutionId = lstJob.getId();
                    log.info("jobExecutionId {}",jobExecutionId);
                    final Long restartId = jobOperator.restart(jobExecutionId);
                    log.info("restartId {}",restartId);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }

}

