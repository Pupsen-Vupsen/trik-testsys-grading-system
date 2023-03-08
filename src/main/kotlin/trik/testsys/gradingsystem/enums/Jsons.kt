package trik.testsys.gradingsystem.enums

import com.beust.klaxon.JsonObject


object Jsons {

    val thereIsNoSubmissionJson = JsonObject(
        mapOf(
            "code" to 404,
            "error_type" to "client",
            "message" to "There is no submission with this id."
        )
    )

    val serverErrorJson = JsonObject(
        mapOf(
            "code" to 500,
            "error_type" to "trik/testsys/gradingsystem",
            "message" to "Something on server went wrong."
        )
    )

    val uploadingFileEmptyOrNotQrsJson = JsonObject(
        mapOf(
            "code" to 422,
            "error_type" to "client",
            "message" to "Uploading file is empty or not .qrs."
        )
    )

    val submissionHasErrorJson = JsonObject(
        mapOf(
            "code" to 422,
            "error_type" to "client",
            "message" to "Submission has error."
        )
    )

    val submissionIsNotTestedYetJson = JsonObject(
        mapOf(
            "code" to 405,
            "error_type" to "client",
            "message" to "Submission is not tested yet."
        )
    )
}