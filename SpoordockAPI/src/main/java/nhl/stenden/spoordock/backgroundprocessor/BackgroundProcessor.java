package nhl.stenden.spoordock.backgroundprocessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BackgroundProcessor {


    private final ExecutorService executorService = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

    private AtomicInteger scheduledTasks = new AtomicInteger(0);
    private AtomicInteger completedTasks = new AtomicInteger(0);
    private AtomicInteger failedTasks = new AtomicInteger(0);

    public void submitTask(Runnable task) {
        var wrappedTask = new WrapperTask(this, task);
        executorService.submit(wrappedTask);
        scheduledTasks.incrementAndGet();
    }

    public boolean isShutdown()
    {
        return executorService.isShutdown();
    }

    public void shutdown()
    {
        executorService.shutdown();
    }

    private void markTaskComplete(boolean success){
        scheduledTasks.decrementAndGet();
        if (success) {
            completedTasks.incrementAndGet();
        } else {
            failedTasks.incrementAndGet();
        }
    }

    private class WrapperTask implements Runnable
    {
        private final Runnable task;

        public WrapperTask(BackgroundProcessor parent, Runnable task)
        {
            this.task = task;
        }

        @Override
        public void run()
        {
            try
            {
                task.run();
                markTaskComplete(true);
            }
            catch (Exception e)
            {
                log.error("Background task failed", e);
                markTaskComplete(false);
            }
        }
    }
}
