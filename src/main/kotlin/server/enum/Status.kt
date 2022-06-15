package server.enum

enum class Status(val symbol: Char) {

    RUNNING('?'),
    OK('+'),
    FAILED('-')
}