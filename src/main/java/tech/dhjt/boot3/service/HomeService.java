package tech.dhjt.boot3.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    private static final Logger logger = LoggerFactory.getLogger(HomeService.class);

    // This method will add delay in execution and return name of thread
    public String getResponse() {
        //Adding sleep
        int sleepTime = 250 ; //new Random().nextInt(1000); -- Uncomment the line if you want to add random delay

        try {
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
            logger.error( e.getMessage());
        }
        return "Current Thread Name: " + Thread.currentThread().toString();
    }

    @Async
    public CompletableFuture<String> getResponse1() {
        int sleepTime = 250 ; //new Random().nextInt(1000); -- Uncomment the line if you want to add random delay

        try {
            logger.info("Task1 started.");
            long start = System.currentTimeMillis();
            TimeUnit.MILLISECONDS.sleep(sleepTime);
            //            Thread.sleep(5000);
            long end = System.currentTimeMillis();

            logger.info("Task1 finished, time elapsed: {} ms.", end-start);
        } catch (InterruptedException e) {
            logger.error( e.getMessage());
        }
        //        return new AsyncResult<>("Current Thread Name: " + Thread.currentThread().toString());
        return CompletableFuture.completedFuture("Current Thread Name1: " + Thread.currentThread().toString());
    }
}
