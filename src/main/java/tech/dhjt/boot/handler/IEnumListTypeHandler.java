package tech.dhjt.boot.handler;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

// 抽象基类，处理任意枚举的 List
public abstract class IEnumListTypeHandler<E extends Enum<E> & IEnum<?>> extends BaseTypeHandler<List<E>> {

    private final Map<String, E> valueEnumMap;

    public <K> IEnumListTypeHandler(Class<E

            > enumType) {
        E[] constants = enumType.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException("No enum constants for " + enumType);
        }
        this.valueEnumMap = new HashMap<>(constants.length);
        for (E e : constants) {
            // 用 getValue() 的字符串形式作为 key，便于数据库值匹配
            String key = String.valueOf(e.getValue());
            if (valueEnumMap.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate IEnum value [" + key + "] in " + enumType);
            }
            valueEnumMap.put(key, e);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<E> parameter, JdbcType jdbcType) throws SQLException {
        String joined = parameter.stream()
                .map(e -> String.valueOf(e.getValue()))
                .collect(Collectors.joining(","));
        ps.setString(i, joined);
    }

    @Override
    public List<E> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return parse(value);
    }

    @Override
    public List<E> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return parse(value);
    }

    @Override
    public List<E> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return parse(value);
    }

    private List<E> parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(k -> {
                    E e = valueEnumMap.get(k);
                    if (e == null) {
                        // 根据业务决定是抛异常还是跳过
                        throw ExceptionUtils.mpe("Cannot convert %s to enum, value: %s",
                                valueEnumMap.values().iterator().next().getClass().getSimpleName(), k);
                    }
                    return e;
                })
                .collect(Collectors.toList());
    }
}
