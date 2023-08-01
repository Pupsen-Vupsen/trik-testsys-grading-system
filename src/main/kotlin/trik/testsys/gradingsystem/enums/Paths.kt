package trik.testsys.gradingsystem.enums


enum class Paths(val text: String) {
    GRADER_CONFIG("/grader.conf"),
    SUBMISSIONS("/submissions/"),
    TASKS("/tasks/"),
    TESTS("tests/"),
    PIN("pin.txt"),
    RESULTS("results/")
}