package trik.testsys.gradingsystem.controllers

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import trik.testsys.gradingsystem.services.SubmissionService
import trik.testsys.security.basicauth.services.TrikUserService


@Component
class StartupCheckController : ApplicationListener<ApplicationReadyEvent> {

    private val logger = LoggerFactory.getLogger(StartupCheckController::class.java)

    @Autowired
    private lateinit var trikUserService: TrikUserService

    @Autowired
    private lateinit var submissionService: SubmissionService

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        runBlocking {
            checkUsers()
            checkUnprocessedSubmissions()
        }
    }

    private fun checkUsers() {
        logger.info("Checking if where are any users able to access the system...")
        trikUserService.countUsers().let {
            if (it == 0L) {
                logger.warn("There are no users in the database. You should add at least one user to the database to have access to the system.")
            } else {
                logger.info("There are $it users in the database.")
            }
        }
    }

    private suspend fun checkUnprocessedSubmissions() {
        logger.info("Checking if there are any unprocessed submissions...")
        submissionService.getAllUnprocessedSubmissionsOrNull().let { submissions ->
            if (submissions == null) {
                logger.info("There are no unprocessed submissions. Ready to go!")
            } else {
                logger.warn("There are ${submissions.size} unprocessed submissions. Started processing them...")

                submissions.forEach {
                    submissionService.testSubmission(it)
                }

                logger.info("All submissions were sent for testing.")
            }
        }
    }
}