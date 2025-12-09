package nhl.stenden.spoordock.llmService.ToolHandling;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.METHOD)
public @interface ToolFunctionCall {
    public String name();
    public String description();
}
