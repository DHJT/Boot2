-- 订单表 H2
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    customer_name VARCHAR(128),
    amount DECIMAL(10, 2),
    status VARCHAR(32),
    version INT DEFAULT 0,
    deleted INT DEFAULT 0,
    tenant_id BIGINT,
    order_time TIMESTAMP,
    extra_info TEXT,   -- 改为 TEXT 类型（JacksonTypeHandler 处理 JSON 序列化/反序列化）
    third_infos TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 状态机持久化上下文表
CREATE TABLE IF NOT EXISTS state_machine_context (
    machine_id VARCHAR(128) PRIMARY KEY,   -- 业务标识（如订单号）
    state VARCHAR(64) NOT NULL,            -- 当前状态
    extended_state TEXT,                   -- 扩展状态变量（JSON）
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);