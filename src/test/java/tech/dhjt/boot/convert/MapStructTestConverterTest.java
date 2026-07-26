package tech.dhjt.boot.convert;

import org.junit.jupiter.api.Test;
import tech.dhjt.boot.convert.bean.MapStructSource;
import tech.dhjt.boot.convert.bean.MapStructTarget;
import tech.dhjt.boot.convert.enums.TestLevelEnum;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MapStructTestConverter 测试类
 * <p>
 * 验证两个核心功能：
 * 1. 使用枚举接口(IEnum)进行 String → 枚举值 的转换
 * 2. 传入 Map 参数，使用源对象的 code 进行获取值并赋值到目标对象的 codeRef 上
 */
class MapStructTestConverterTest {

    @Test
    void testToTarget_WithValidCodeMap_ShouldMapCodeRef() {
        // 准备测试数据
        MapStructSource source = new MapStructSource();
        source.setCode("KEY_001");
        source.setLevelCode("H");
        source.setName("测试对象");

        Map<String, String> codeMap = new HashMap<>();
        codeMap.put("KEY_001", "这是KEY_001对应的描述");
        codeMap.put("KEY_002", "这是KEY_002对应的描述");

        // 执行转换
        MapStructTarget target = MapStructTestConverter.INSTANCE.toTarget(source, codeMap);

        // 验证结果
        assertNotNull(target);
        assertEquals("测试对象", target.getName());
        assertEquals(TestLevelEnum.HIGH, target.getLevel());
        assertEquals("这是KEY_001对应的描述", target.getCodeRef());
    }

    @Test
    void testToTarget_WithNullCode_ShouldMapCodeRefToNull() {
        // 源 code 为 null
        MapStructSource source = new MapStructSource();
        source.setCode(null);
        source.setLevelCode("L");
        source.setName("无Code对象");

        Map<String, String> codeMap = new HashMap<>();
        codeMap.put("KEY_001", "描述信息");

        MapStructTarget target = MapStructTestConverter.INSTANCE.toTarget(source, codeMap);

        assertNotNull(target);
        assertEquals("无Code对象", target.getName());
        assertEquals(TestLevelEnum.LOW, target.getLevel());
        assertNull(target.getCodeRef());
    }

    @Test
    void testToTarget_WithMissingCodeInMap_ShouldMapCodeRefToNull() {
        // code 在 Map 中不存在
        MapStructSource source = new MapStructSource();
        source.setCode("NOT_EXIST");
        source.setLevelCode("M");
        source.setName("缺失映射");

        Map<String, String> codeMap = new HashMap<>();
        codeMap.put("KEY_001", "描述信息");

        MapStructTarget target = MapStructTestConverter.INSTANCE.toTarget(source, codeMap);

        assertNotNull(target);
        assertEquals("缺失映射", target.getName());
        assertEquals(TestLevelEnum.MEDIUM, target.getLevel());
        assertNull(target.getCodeRef());
    }

    @Test
    void testToTarget_WithLevelCodeUnknown_ShouldMapToUnknown() {
        MapStructSource source = new MapStructSource();
        source.setCode("KEY_003");
        source.setLevelCode("X"); // 不存在的 code
        source.setName("未知级别");

        Map<String, String> codeMap = new HashMap<>();
        codeMap.put("KEY_003", "KEY_003的值");

        MapStructTarget target = MapStructTestConverter.INSTANCE.toTarget(source, codeMap);

        assertNotNull(target);
        assertEquals("未知级别", target.getName());
        assertEquals(TestLevelEnum.UNKNOWN, target.getLevel());
        assertEquals("KEY_003的值", target.getCodeRef());
    }

    @Test
    void testToTarget_WithNullLevelCode_ShouldMapToNull() {
        MapStructSource source = new MapStructSource();
        source.setCode("KEY_004");
        source.setLevelCode(null);
        source.setName("Null级别");

        Map<String, String> codeMap = new HashMap<>();
        codeMap.put("KEY_004", "KEY_004的值");

        MapStructTarget target = MapStructTestConverter.INSTANCE.toTarget(source, codeMap);

        assertNotNull(target);
        assertEquals("Null级别", target.getName());
        // levelCode 为 null → level 也为 'null' (对 null 入参 fromCode 返回 UNKNOWN)
        // 但我们的 stringToTestLevelEnum 直接委托给 TestLevelEnum.fromCode(null) → UNKNOWN
        assertEquals(TestLevelEnum.UNKNOWN, target.getLevel());
        assertEquals("KEY_004的值", target.getCodeRef());
    }

    @Test
    void testToTarget_WithEmptyCodeMap_ShouldMapCodeRefToNull() {
        MapStructSource source = new MapStructSource();
        source.setCode("KEY_001");
        source.setLevelCode("H");
        source.setName("空Map");

        // 传入空的 Map
        Map<String, String> codeMap = new HashMap<>();

        MapStructTarget target = MapStructTestConverter.INSTANCE.toTarget(source, codeMap);

        assertNotNull(target);
        assertEquals("空Map", target.getName());
        assertEquals(TestLevelEnum.HIGH, target.getLevel());
        assertNull(target.getCodeRef());
    }

}