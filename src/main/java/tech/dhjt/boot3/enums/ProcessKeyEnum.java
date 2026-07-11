package tech.dhjt.boot3.enums;

/**
 * 流程Key枚举 — 统一管理所有流程定义的Key、显示名称和资源路径
 */
public enum ProcessKeyEnum {

    LEAVE("leaveProcess", "请假审批流程", "processes/leave.bpmn20.xml"),
    MULTI_LEVEL_APPROVAL("multiLevelApprovalProcess", "多级复杂审批流程", "processes/multiLevelApprovalProcess.bpmn20.xml"),
    ;

    private final String key;
    private final String displayName;
    private final String resourcePath;

    ProcessKeyEnum(String key, String displayName, String resourcePath) {
        this.key = key;
        this.displayName = displayName;
        this.resourcePath = resourcePath;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * 根据key值查找枚举，忽略大小写
     */
    public static ProcessKeyEnum fromKey(String key) {
        if (key == null) throw new IllegalArgumentException("流程Key不能为空");
        for (ProcessKeyEnum e : values()) {
            if (e.key.equalsIgnoreCase(key)) return e;
        }
        throw new IllegalArgumentException("未知的流程Key: " + key);
    }

    /**
     * 校验key是否合法
     */
    public static boolean isValid(String key) {
        try {
            fromKey(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}