package nhl.stenden.spoordock.llmService.ToolHandling;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.catalina.startup.Tool;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.deser.impl.CreatorCandidate.Param;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nhl.stenden.spoordock.llmService.dtos.parameters.ObjectParameter;
import nhl.stenden.spoordock.llmService.dtos.parameters.Parameter;
import nhl.stenden.spoordock.llmService.dtos.parameters.ParameterType;
import nhl.stenden.spoordock.llmService.dtos.parameters.PrimitiveParameter;
import nhl.stenden.spoordock.llmService.dtos.parameters.ToolRequest.Function;
import nhl.stenden.spoordock.llmService.dtos.parameters.ToolRequest.ToolRequest;
import nhl.stenden.spoordock.llmService.dtos.parameters.toolCall.FunctionCall;

@Slf4j
@Component
public class ToolHandlingManager {
    public Map<String, ToolFunctionData> availableToolMethods = new HashMap<>();

    @Getter @Setter
    @AllArgsConstructor
    private class ToolFunctionData {
        public ToolRequest toolRequest;
        public Method method;
        public ToolService serviceInstance;
    }

    public ToolHandlingManager(List<ToolService> toolServices) {
        
        for (ToolService service : toolServices) {
        
            var methods = service.getClass().getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(ToolFunctionCall.class)) {
                    ToolFunctionCall annotation = method.getAnnotation(ToolFunctionCall.class);
                    
                    String name = service.getClass().getSimpleName() + " | " + annotation.name();
                    String description = annotation.description();
                    ObjectParameter rootParam = buildObjectParameter(method.getParameters());

                    Function function = new Function(name, description, rootParam);
                    ToolRequest toolRequest = new ToolRequest(function);

                    ToolFunctionData functionData = new ToolFunctionData(toolRequest, method, service);
                    availableToolMethods.put(name, functionData);
                }
            }
        }
    }

    public List<ToolRequest> getAvailableTools() {
        return availableToolMethods.values().stream()
                .map(ToolFunctionData::getToolRequest)
                .toList();
    }


    public String handleToolInvocation(FunctionCall function) {

        String name = function.getName();
        if (!availableToolMethods.containsKey(name)) {
            log.error("Tool method not found: " + name);
            return "Error: Tool method not found.";
        }

        ToolFunctionData functionData = availableToolMethods.get(name);
        Method method = functionData.getMethod();
        ToolService serviceInstance = functionData.getServiceInstance();
        
        try {
            // Currently assuming all parameters are of type String for simplicity
            Object result = method.invoke(serviceInstance, function.getParameters().values().toArray());
            return result.toString();
        } catch (Exception e) {
            log.error("Error invoking tool method: " + name, e);
            return "Error: Failed to invoke tool method.";
        }

    }
    

    private ObjectParameter buildObjectParameter(java.lang.reflect.Parameter[] params){
        Map<String, Parameter> parameters = new java.util.HashMap<>();
        for (java.lang.reflect.Parameter param : params) {
            if (param.isAnnotationPresent(ToolParameter.class)){
                ToolParameter paramAnnotation = param.getAnnotation(ToolParameter.class);
                String paramDescription = paramAnnotation.description();
                ParameterType paramType = mapType(param.getType());

                if (paramType == ParameterType.OBJECT) {
                    //Skip For now TODO: Handle nested object parameters, probably recursion
                } else {
                    parameters.put(param.getName(), new PrimitiveParameter(paramType, paramDescription));
                }
            }
        }

        return new ObjectParameter(parameters);
    }

    private ParameterType mapType(Class<?> clazz) {
        if (clazz.equals(String.class)) {
            return ParameterType.STRING;
        } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            return ParameterType.INTEGER;
        } else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            return ParameterType.BOOLEAN;
        } else if (clazz.equals(Double.class) || clazz.equals(double.class) || clazz.equals(Float.class) || clazz.equals(float.class)) {
            return ParameterType.NUMBER;
        } else {
            return ParameterType.OBJECT; // Default to OBJECT for complex types
        }
    }

}
