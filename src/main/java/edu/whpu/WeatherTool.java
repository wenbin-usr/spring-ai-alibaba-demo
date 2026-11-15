package edu.whpu;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.function.BiFunction;

public class WeatherTool implements BiFunction<WeatherTool.WeatherRequest, ToolContext, String> {

    /**
     * 工具的入参必须是对象（JSON Schema 中 type=object），
     * 不能直接用 String 之类的标量类型，否则 DashScope 会在第二轮
     * 校验 function.arguments 时返回 400 InvalidParameter。
     */
    public record WeatherRequest(
            @ToolParam(description = "城市名称") String city) {
    }

    @Override
    public String apply(WeatherRequest request, ToolContext toolContext) {
        return "It's always sunny in " + request.city() + "!";
    }
}
