package nhl.stenden.spoordock.backgroundprocessor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    void testCustomRunnerIsUsed() throws Exception{
        
    }



}
