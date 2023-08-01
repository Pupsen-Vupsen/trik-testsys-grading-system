package trik.testsys.grading

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import trik.testsys.gradingsystem.enums.Status
import java.io.File


internal class GradingNode(
    private val processesCount: Int,
    private val timeout: Int,
    private val client: HttpClient,
    ipPort: String,
    private val trikStudioImage: String,
    private val onGradingFinished: (Long, Status) -> Unit
) {

    //region SubmissionInfo
    data class SubmissionInfo(
        val submissionId: Long,
        val submissionFilePath: String,
        val fieldsDirectoryPath: String,
        val resultsDirectoryPath: String,
    )
    //endregion

    private val urlPrefix = "http://${ipPort}/"
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val gradedSubmissions = mutableSetOf<Long>()
    private val logPrefix = "[$ipPort]"

    init {
        runBlocking { setupNode() }
    }

    private fun setupNode(): HttpStatusCode = runBlocking {
        logger.error("$logPrefix Sent setup node request")
        client.submitFormWithBinaryData(
            url = "${urlPrefix}setup",
            formData = formData {
                append("trik_studio_image", trikStudioImage)
                append("timeout", timeout)
            }
        )
    }

    private fun uploadSubmission(
        submissionId: Long,
        fields: Array<File>,
        submissionFilePath: String
    ): Unit = runBlocking {
        logger.debug("$logPrefix Sent upload submission[$submissionId] request")
        client.submitFormWithBinaryData(
            url = "${urlPrefix}submission/$submissionId",
            formData = formData {
                append("submission_file", File(submissionFilePath).readBytes(), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=submission_file")
                })
                append("fields_count", fields.count())
                for (i in fields.indices){
                    val fieldFile = fields[i]
                    append("field_$i", fieldFile.readBytes(), Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=field_$i")
                    })
                }
            }
        )
    }

    private fun getResults(submissionId: Long): String = runBlocking {
        logger.debug("$logPrefix polling id[$submissionId]")
        val response = client.get<HttpResponse> {
            url("${urlPrefix}submission/$submissionId")
        }
        response.readText()
    }

    private fun statusFromString(str: String): Status {
        return when(str){
            "ACCEPTED" -> Status.ACCEPTED
            "FAILED" -> Status.FAILED
            "ERROR" -> Status.ERROR
            "RUNNING" -> Status.RUNNING
            "QUEUED" -> Status.QUEUED
            else -> throw Exception("Unexpected status $str")
        }
    }

    fun poll() {
        val finished = mutableSetOf<Long>()
        for (submissionId in gradedSubmissions) {
            val result = getResults(submissionId)
            val status = statusFromString(result)
            if (status == Status.QUEUED || status == Status.RUNNING)
                continue
            onGradingFinished(submissionId, status)
            finished.add(submissionId)
        }
        gradedSubmissions -= finished
    }

    fun availableProcesses(): Int {
        return processesCount - gradedSubmissions.size
    }

    fun gradeSubmission(info: SubmissionInfo) {
        val fields = File(info.fieldsDirectoryPath).listFiles()!!
        uploadSubmission(info.submissionId, fields, info.submissionFilePath)
        gradedSubmissions.add(info.submissionId)
    }
}