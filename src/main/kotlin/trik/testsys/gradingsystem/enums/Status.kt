package trik.testsys.gradingsystem.enums



enum class Status(val code: Int) {

    QUEUED(100),
    RUNNING(102),
    ACCEPTED(202),
    FAILED(406),
    ERROR(500);
}