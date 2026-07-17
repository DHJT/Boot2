package tech.dhjt.boot.handler;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// 抽象基类，处理任意枚举的 List
public abstract class BaseEnumListTypeHandler<E extends Enum<E>> extends BaseTypeHandler<List<E>> {

    private final Class<E> enumType;

    public BaseEnumListTypeHandler(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<E> parameter, JdbcType jdbcType) throws SQLException {
        String joined = parameter.stream().map(Enum::name).collect(Collectors.joining(","));
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
        if (StringUtils.isBlank(value)) return Collections.emptyList();
        return Arrays.stream(value.split(","))
                .map(name -> Enum.valueOf(enumType, name))
                .collect(Collectors.toList());
    }
}
