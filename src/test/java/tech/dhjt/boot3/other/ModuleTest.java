package tech.dhjt.boot3.other;

public class ModuleTest {
    // 验证模块加载：
    public static void main(String[] args) {
        System.out.println("Module loaded: " + ModuleLayer.boot().findModule("jdk.incubator.concurrent").isPresent());
    }

}
