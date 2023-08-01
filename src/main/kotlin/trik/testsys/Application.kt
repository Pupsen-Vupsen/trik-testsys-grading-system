package trik.testsys

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.MultipartConfigFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import javax.servlet.MultipartConfigElement


@SpringBootApplication
@Configuration
@EnableScheduling
class Application {

    @Bean
    fun taskScheduler(): TaskScheduler? {
        val threadPoolTaskScheduler = ThreadPoolTaskScheduler()
        threadPoolTaskScheduler.poolSize = 2
        threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler")
        return threadPoolTaskScheduler
    }

    @Bean
    fun multipartConfigElement(): MultipartConfigElement {
        val factory = MultipartConfigFactory()
        factory.setMaxFileSize(DataSize.of(4, DataUnit.MEGABYTES))
        factory.setMaxRequestSize(DataSize.of(4, DataUnit.MEGABYTES))
        return factory.createMultipartConfig()
    }

    companion object {

//        fun kek(submissionId: Long, status: Status) {
//            println("Id: $submissionId Status: $status")
//        }

        @JvmStatic
        fun main(args: Array<String>) {
//            val nodePool = GradingNodePool("/home/viktor/Documents/code/pet/TestGradingSystem/appdata/grader.conf", ::kek)
//            val task1 = "/home/viktor/Desktop/tasks/task1"
//            val task1solution = "/home/viktor/Downloads/Telegram Desktop/Task_2.1_reshenie.qrs"
//            nodePool.grade(GradingNode.SubmissionInfo(
//                submissionId = 1,
//                submissionFilePath = task1solution,
//                fieldsDirectoryPath = task1,
//                resultsDirectoryPath = "/home/viktor/Desktop"
//            ))
//            nodePool.grade(GradingNode.SubmissionInfo(
//                submissionId = 2,
//                submissionFilePath = "$task1/Task_2.1.xml",
//                fieldsDirectoryPath = task1,
//                resultsDirectoryPath = "/home/viktor/Desktop"
//            ))
//            while(true){
//                Thread.sleep(15_000)
//                nodePool.poll()
//            }

            SpringApplication.run(Application::class.java, *args)
        }
    }
}
