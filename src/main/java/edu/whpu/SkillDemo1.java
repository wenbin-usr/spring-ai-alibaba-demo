package edu.whpu;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.FileSystemTools;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SkillDemo1 {
    public static void main(String[] args) throws GraphRunnerException, IOException {

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .build();
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        String projectRoot = Path.of("").toAbsolutePath().normalize().toString();

        SkillRegistry registry = FileSystemSkillRegistry.builder()
                .projectSkillsDirectory(Path.of("skills").toAbsolutePath().toString())
                .build();

        SkillsAgentHook skillsHook = SkillsAgentHook.builder()
                .skillRegistry(registry)
                .build();

        // L3 执行层：全部使用框架内置工具
        // 1) 文件：read_file / write_file / edit_file / list_files（要求绝对路径）
        FileSystemTools fileTools = FileSystemTools.builder()
                .rootDir(projectRoot)
                .maxFileSizeMb(10)
                .build();
        ToolCallback[] fileToolCallbacks = ToolCallbacks.from(fileTools);

        // 2) Shell：持久会话，由 ShellToolAgentHook 在 beforeAgent 里初始化
        //    注意 commandTimeout 单位是毫秒；Windows 默认 shell 是 PowerShell，这里显式用 cmd.exe 更稳
        List<String> shellCommand = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? List.of("cmd.exe")
                : List.of("/bin/sh");
        ShellTool2 shellTool = ShellTool2.builder(projectRoot)
                .withShellCommand(shellCommand)
                .withCommandTimeout(120_000)
                .withMaxOutputLines(500)
                .build();
        ShellToolAgentHook shellHook = ShellToolAgentHook.builder()
                .shellTool2(shellTool)
                .build();

        Files.createDirectories(Path.of("output"));

        ReactAgent agent = ReactAgent.builder()
                .name("skills-agent")
                .model(chatModel)
                .tools(fileToolCallbacks)
                .saver(new MemorySaver())
                .hooks(List.of(skillsHook, shellHook))
                .instruction("你是一个严谨的文档处理助手。先调用 read_skill 阅读技能说明，再按其中的步骤执行。"
                        + "文件操作用 read_file / write_file / edit_file / list_files（必须传绝对路径，"
                        + "write_file 不能覆盖已存在文件）；命令用 shell 工具执行。"
                        + "项目根目录是 " + projectRoot + "，产物统一放到 " + projectRoot + "/output，"
                        + "结束前用 shell 校验文件确实存在。")
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
