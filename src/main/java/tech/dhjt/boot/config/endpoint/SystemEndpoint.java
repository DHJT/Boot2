package tech.dhjt.boot.config.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Configuration;

@Configuration
@Endpoint(id = "system")
public class SystemEndpoint {

    // GET请求
    @ReadOperation
    public SystemInfo systemInfo() {
        Runtime runtime = Runtime.getRuntime();
        return new SystemInfo(
                System.getProperty("os.name"),
                runtime.availableProcessors(),
                runtime.freeMemory(),
                runtime.maxMemory(),
                runtime.totalMemory()
                );
    }

    //  (POST请求)
    //    @WriteOperation
    //    public void put(String key, String value) {
    //        // TODO do something...
    //    }

    // DELETE请求
    @DeleteOperation
    public String reset() {
        // TODO do something...
        return "Reset completed";
    }

    public record SystemInfo(
            String osName,
            int processors,
            long freeMemory,
            long maxMemory,
            long totalMemory
            ) {}

}
