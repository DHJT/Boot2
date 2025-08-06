package tech.dhjt.boot.config.endpoint;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Configuration;

@Configuration
@Endpoint(id = "vthreads")
public class VirtualThreadEndpoint {

    @ReadOperation
    public VirtualThreadStats virtualThreadStats() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return new VirtualThreadStats(
                Thread.currentThread().isVirtual(),
                Thread.getAllStackTraces().keySet().stream().filter(Thread::isVirtual).count(),
                threadBean.getCurrentThreadCpuTime(),
                threadBean.getCurrentThreadUserTime()
                );
    }

    public record VirtualThreadStats(
            boolean isCurrentThreadVirtual,
            long virtualThreadCount,
            long currentThreadCpuTime,
            long currentThreadUserTime
            ) {}
}
