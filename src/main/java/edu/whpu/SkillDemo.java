package edu.whpu;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.nio.file.Path;
import java.util.List;

public class SkillDemo {
    public static void main(String[] args) throws GraphRunnerException {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .build();
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        SkillRegistry registry = FileSystemSkillRegistry.builder()
                .projectSkillsDirectory(Path.of("skills").toAbsolutePath().toString())
                .build();

        SkillsAgentHook hook = SkillsAgentHook.builder()
                .skillRegistry(registry)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("skills-agent")
                .model(chatModel)
                .saver(new MemorySaver())
                .hooks(List.of(hook))
                .build();

        AssistantMessage message = agent.call("帮我生成一个pdf文件");
        System.out.println(message.getText());
    }
}
