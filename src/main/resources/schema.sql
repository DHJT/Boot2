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
    -- 下单人信息
    order_person_name VARCHAR(128),
    order_person_phone VARCHAR(32),
    order_person_address VARCHAR(256),
    order_person_bank VARCHAR(128),
    order_person_bank_branch VARCHAR(128),
    extra_info TEXT,   -- 改为 TEXT 类型（JacksonTypeHandler 处理 JSON 序列化/反序列化）
    third_infos TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);