package tech.dhjt.boot3.service.dmn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DMN 决策表启动自动部署 Runner
 * <p>
 * 应用启动时调用 {@link DmnService#deployAll()} 部署全部预定义决策表（内容指纹幂等，
 * 内容未变化自动跳过）。通过 {@code app.dmn.auto-deploy} 配置门控（默认 true），
 * 测试环境可置为 false 以隔离自动部署对测试断言的影响。
 *
 * @author DHJT
 */
@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "app.dmn.auto-deploy", havingValue = "true", matchIfMissing = true)
public class DmnAutoDeployRunner implements CommandLineRunner {

    private final DmnService dmnService;

    @Override
    public void run(String... args) {
        try {
            dmnService.deployAll();
            log.info("DMN 决策表启动自动部署完成");
        } catch (Exception e) {
            // 自动部署失败不阻断应用启动（记录日志，可手动调 /dmn/deploy/all）
            log.error("DMN 决策表启动自动部署失败: {}", e.getMessage(), e);
        }
    }
}
