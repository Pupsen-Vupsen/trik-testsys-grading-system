package server.enum

enum class Requests(val text: String) {

    SUBMISSION_NOT_FOUND("Submission with this id does not exist."),
    NO_SUBMISSIONS("There is no submissions."),
    EMPTY_FILE("Uploading file is empty or not .qrs file.")
}