package nhl.stenden.spoordock.backgroundprocessor;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundProcessorTests {


    @Test
    void testSubmitTask() throws Exception {
        var backgroundProcessor = new BackgroundProcessor();

        AtomicBoolean ran = new AtomicBoolean(false);
        backgroundProcessor.submitTask(() -> {
            ran.set(true);
        });

        Thread.sleep(50);
        assertTrue(ran.get());
    }

    @Test
    void testCustomExecutorService() throws Exception{

        ExecutorService mocked = Mockito.mock(ExecutorService.class);

        AtomicBoolean used = new AtomicBoolean(false);

        when(mocked.submit(any(Runnable.class)))
            .thenAnswer(invocation -> {
                used.set(true);
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            });

        var backgroundProcessor = new BackgroundProcessor(mocked);
        backgroundProcessor.submitTask(() -> { System.out.println("Ran this task"); });

        assertTrue(used.get());
    }
    @Test 
    void testTaskCounts() throws Exception {
        var backgroundProcessor = new BackgroundProcessor();

        int tasksToSubmit = 5;
        AtomicBoolean ran = new AtomicBoolean(false);
        for (int i = 0; i < tasksToSubmit; i++) {
            backgroundProcessor.submitTask(() -> {
                ran.set(true);
            });
        }

        Thread.sleep(100); // Wait for tasks to complete

        assertEquals(0, backgroundProcessor.getScheduledTaskCount());
        assertEquals(tasksToSubmit, backgroundProcessor.getCompletedTaskCount());
        assertEquals(0, backgroundProcessor.getFailedTaskCount());
    }

     @Test 
    void testTaskCounts_whenExceptionThrown() throws Exception {
        var backgroundProcessor = new BackgroundProcessor();

        int tasksToSubmit = 5;
        AtomicBoolean ran = new AtomicBoolean(false);
        for (int i = 0; i < tasksToSubmit; i++) {
            backgroundProcessor.submitTask(() -> {
                ran.set(true);
                throw new RuntimeException("Test exception");
            });
        }

        Thread.sleep(100); // Wait for tasks to complete

        assertTrue(ran.get());
        assertEquals(0, backgroundProcessor.getScheduledTaskCount());
        assertEquals(0, backgroundProcessor.getCompletedTaskCount());
        assertEquals(5, backgroundProcessor.getFailedTaskCount());
    }



}
