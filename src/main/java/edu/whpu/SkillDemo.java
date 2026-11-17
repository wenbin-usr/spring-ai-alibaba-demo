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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SkillDemo {
    public static void main(String[] args) throws GraphRunnerException, IOException {

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

        // 关键改动：补上 L3 执行工具（文件读写 + 命令执行），否则模型只能读技能说明，无法落地产物
        SkillToolbox toolbox = new SkillToolbox();
        Files.createDirectories(Path.of("output"));

        ReactAgent agent = ReactAgent.builder()
                .name("skills-agent")
                .model(chatModel)
                .tools(toolbox.toolCallbacks())
                .saver(new MemorySaver())
                .hooks(List.of(hook))
                .instruction("你是一个严谨的文档处理助手。先调用 read_skill 阅读技能说明，再严格按其中的步骤用工具执行；"
                        + "产物必须落在 output/ 目录，结束前用 run_command 校验文件确实存在。")
                .build();

        AssistantMessage message = agent.call("""
                用 pdf 技能生成一个 PDF 文件：
                1. 先读取 pdf 技能的完整说明；
                2. 按说明用 reportlab 生成一页 PDF（标题 "Hello Skill"，正文一段说明文字）；
                3. 保存到 output/hello-skill.pdf；
                4. 用 python 检查该文件存在且大小大于 0，汇报结果。
                """);
        System.out.println(message.getText());
    }
}
