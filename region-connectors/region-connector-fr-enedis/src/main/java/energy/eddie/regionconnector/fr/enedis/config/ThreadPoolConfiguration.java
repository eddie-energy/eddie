package energy.eddie.regionconnector.fr.enedis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolConfiguration {
    @Value("${region-connector.fr.enedis.threadpool.core.pool.size}")
    private int corePoolSize;
    @Value("${region-connector.fr.enedis.threadpool.max.pool.size}")
    private int maxPoolSize;
    @Value("${region-connector.fr.enedis.threadpool.queue.capacity}")
    private int queueCapacity;

    @Bean
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        return executor;
    }
}
