package server.enum

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
enum class Status(val code: Int) {
    QUEUED(100),
    RUNNING(102),
    ACCEPTED(202),
    FAILED(406),
    ERROR(500);

    @JsonValue
    fun toValue() = code
}