package server.enum

enum class Status(val code: Int) {
    RUNNING(0),
    OK(1),
    FAILED(-1)
}