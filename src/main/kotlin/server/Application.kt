package server

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.MultipartConfigFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import java.util.concurrent.Executor
import javax.servlet.MultipartConfigElement


@SpringBootApplication
@Configuration
class Application {

    @Bean
    fun multipartConfigElement(): MultipartConfigElement {
        val factory = MultipartConfigFactory()
        factory.setMaxFileSize(DataSize.of(4, DataUnit.MEGABYTES))
        factory.setMaxRequestSize(DataSize.of(4, DataUnit.MEGABYTES))
        return factory.createMultipartConfig()
    }

    @Bean("prepareExecutor")
    fun createPrepareExecutor(): Executor? {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 1
        executor.maxPoolSize = 1
        executor.setQueueCapacity(1000)
        executor.initialize()
        return executor
    }

    @Bean("testExecutor")
    fun createTestExecutor(): Executor? {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 6
        executor.maxPoolSize = 6
        executor.setQueueCapacity(1000)
        executor.initialize()
        return executor
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}
