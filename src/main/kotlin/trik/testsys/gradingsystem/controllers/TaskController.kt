package trik.testsys.gradingsystem.controllers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import trik.testsys.gradingsystem.enums.Jsons
import trik.testsys.gradingsystem.enums.Paths
import java.io.File
import java.nio.file.Files

@RestController
@RequestMapping("/v3/grading-system/tasks")
class TaskController {

    val logger: Logger = LoggerFactory.getLogger(SubmissionController::class.java)

    @PostMapping("/create")
    fun createTask(@RequestBody files: List<MultipartFile>, @RequestParam taskName: String): ResponseEntity<Any> {
        logger.info("Client requested to create task.")
        val allTasks = File(Paths.TASKS.text).list()!!

        if (allTasks.contains(taskName)) {
            logger.info("Task with name $taskName already exists.")
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Jsons.conflictTaskNameJson)
        }

        File(Paths.TASKS.text + taskName).mkdir()
        File(Paths.TASKS.text + taskName + "/" + Paths.TESTS.text).mkdir()

        for (file in files) {
            val fileBytes = file.bytes
            val fileName = file.originalFilename!!
            val filePath = Paths.TASKS.text + taskName + "/" + Paths.TESTS.text + fileName
            Files.write(File(filePath).toPath(), fileBytes)
        }

        logger.info("Task $taskName created successfully. Count of tests: ${files.size}.")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                mapOf(
                    "status" to "OK",
                    "message" to "Task $taskName created successfully. Count of tests: ${files.size}."
                )
            )
    }
}