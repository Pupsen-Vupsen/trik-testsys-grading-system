package server.enum

enum class Status(symbol: Char) {

    RUNNING('?'),
    OK('+'),
    FAILED('-')
}