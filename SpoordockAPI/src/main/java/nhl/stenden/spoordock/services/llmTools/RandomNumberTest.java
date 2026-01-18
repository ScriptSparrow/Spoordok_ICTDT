// package nhl.stenden.spoordock.services.llmTools;

// import org.springframework.stereotype.Component;

// import nhl.stenden.spoordock.llmService.ToolHandling.ToolFunctionCall;
// import nhl.stenden.spoordock.llmService.ToolHandling.ToolParameter;
// import nhl.stenden.spoordock.llmService.ToolHandling.ToolService;

// @Component
// public class RandomNumberTest implements ToolService  {

//     @ToolFunctionCall(
//             name = "generate_random_number",
//             description = "Generates a random number between the specified min and max values."
//     )
//     public int generateRandomNumber(
//             @ToolParameter(description = "The minimum value (inclusive).") int min,
//             @ToolParameter(description = "The maximum value (inclusive).") int max
//     )
//     {
//         return (int) (Math.random() * (max - min + 1)) + min;
//     }

// }

