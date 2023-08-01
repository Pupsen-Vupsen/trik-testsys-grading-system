package trik.testsys.grading

import org.slf4j.LoggerFactory
import trik.testsys.gradingsystem.entities.Submission
import trik.testsys.gradingsystem.enums.FilePostfixes
import trik.testsys.gradingsystem.enums.Paths
import trik.testsys.gradingsystem.enums.Status

class Grader(graderConfigPath: String) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val submissions = mutableMapOf<Long, Submission>()
    private val gradingNodePool = GradingNodePool(graderConfigPath, ::onGradingFinished)
    val gradingFinishedEvent = Event<Submission>()

    //region Custom exceptions
    class IncorrectConfigException(msg: String):
        Exception(msg)
    //endregion


    //region Paths
    private fun getResultsDirectory(submission: Submission): String {
        val submissionDir = "${Paths.SUBMISSIONS.text}${submission.id}/"
        return "${submissionDir}${Paths.RESULTS.text}${FilePostfixes.RESULT.text}"
    }
    //endregion


    private fun onGradingFinished(submissionId: Long, status: Status) {
        val submission = submissions[submissionId]!!
        submission.changeStatus(status)
        gradingFinishedEvent(submission)
    }

    //region SubmissionInfo creating
    private fun submissionInfoFromSubmission(submission: Submission): GradingNode.SubmissionInfo{
        val submissionDir = "${Paths.SUBMISSIONS.text}${submission.id}/"
        val submissionFilePath = "${submissionDir}${submission}${FilePostfixes.QRS.text}"
        val fieldsDirectoryPath = "${submissionDir}${Paths.TESTS.text}"
        return GradingNode.SubmissionInfo(
            submissionId = submission.id!!,
            submissionFilePath = submissionFilePath,
            fieldsDirectoryPath = fieldsDirectoryPath,
            resultsDirectoryPath = getResultsDirectory(submission)
        )
    }
    //endregion

    fun grade(submission: Submission) {
        val submissionInfo = submissionInfoFromSubmission(submission)
        gradingNodePool.grade(submissionInfo)
    }

    fun poll() {
        gradingNodePool.poll()
    }
}