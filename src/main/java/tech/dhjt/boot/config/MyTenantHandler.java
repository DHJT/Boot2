package tech.dhjt.boot.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

@Component
public class MyTenantHandler implements TenantLineHandler {

    /**
     * 获取当前租户ID（示例：从ThreadLocal或SecurityContext中获取）
     */
    @Override
    public Expression getTenantId() {
        // 模拟从上下文中获取租户ID，实际可改为从JWT、Session等获取
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            tenantId = 0L;  // 默认租户
        }
        return new LongValue(tenantId);
    }

    /**
     * 租户字段的数据库列名
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    /**
     * 是否忽略当前表（某些全局表不需要租户过滤）
     * 例如系统配置表、字典表等
     */
    @Override
    public boolean ignoreTable(String tableName) {
        // 示例：忽略 sys_config 表
        return "sys_config".equalsIgnoreCase(tableName);
    }

}