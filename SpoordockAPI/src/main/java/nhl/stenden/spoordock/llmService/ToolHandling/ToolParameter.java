package nhl.stenden.spoordock.llmService.ToolHandling;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.PARAMETER)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ToolParameter {
    public String description();
}
