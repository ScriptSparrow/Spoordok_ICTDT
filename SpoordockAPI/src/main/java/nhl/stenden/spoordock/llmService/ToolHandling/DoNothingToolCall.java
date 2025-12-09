package nhl.stenden.spoordock.llmService.ToolHandling;

import org.springframework.stereotype.Component;

@Component
public class DoNothingToolCall implements ToolService{

    @ToolFunctionCall(
        name = "do_nothing",
        description = "A tool that does nothing and returns an empty string. Use this if you don't want to use a tool, but are required to call one."
    )
    public String doNothing(){
        return "";
    }

}
