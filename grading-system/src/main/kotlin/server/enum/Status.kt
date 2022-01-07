package server.enum

enum class Status(val code: String) {
    RUNNING("?"),
    OK("+"),
    FAILED("-")
}