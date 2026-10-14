package schedule

import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface ParseResult {
    val number: Long
    data class Success(override val number: Long, val session: Session) : ParseResult
    data class Failure(override val number: Long, val reason: Reason) : ParseResult
}

sealed interface Reason {
    object InvalidSeparator : Reason
    data class WrongFieldsCount(val count: Int) : Reason
    data class EmptyField(val name: String) : Reason
    data class InvalidDate(val value: String) : Reason
    data class UnknownSessionType(val value: String) : Reason
    data class ExamNeedsTwoTeachers(val count: Int) : Reason
    data class DomainViolation(val message: String) : Reason
}

private val FIELDS_PARAMS = listOf<String>("date", "type", "topic", "location", "teachers")
private val DATE_FORMAT = Regex("(?!0000)\\d{4}-\\d{2}-\\d{2}")

fun parseScheduleLine(number: Long, line: String): ParseResult {
    if ('|' !in line) {
        return ParseResult.Failure(number, Reason.InvalidSeparator)
    }
    val fields = line.split('|')
    if (fields.size != 5) {
        return ParseResult.Failure(number, Reason.WrongFieldsCount(fields.size))
    }
    for (i in 0..4){
        if (fields[i].isBlank()) {
            return ParseResult.Failure(number, Reason.EmptyField(FIELDS_PARAMS[i]))
        }
    }
    if (!DATE_FORMAT.matches(fields[0])) {
        return ParseResult.Failure(number, Reason.InvalidDate(fields[0]))
    }

    val dateStr = fields[0]
    val sessionTypeStr = fields[1]
    val topic = fields[2]
    val location = fields[3]
    val teachersStr = fields[4]

    val date = try{
        LocalDate.parse(dateStr)
    }
    catch (e: DateTimeParseException){
        return ParseResult.Failure(number, Reason.InvalidDate(dateStr))
    }

    val type = try {
        SessionType.valueOf(sessionTypeStr)
    }
    catch (e: IllegalArgumentException) {
        return ParseResult.Failure(number, Reason.UnknownSessionType(sessionTypeStr))
    }

    val teachers = fields[4].split(",")

    if (type == SessionType.EXAM && teachers.size < 2) {
        return ParseResult.Failure(number, Reason.ExamNeedsTwoTeachers(teachers.size))
    }

    return try{
        val session = when(type) {
            SessionType.LECTURE -> LectureSession(date, topic, location, teachers)
            SessionType.PRACTICE -> PracticeSession(date, topic, location, teachers)
            SessionType.EXAM -> ExamSession(date, topic, location, teachers)
            SessionType.CANCELLED -> {
                require(location == "-") { "Location for Cancelled Session must be '-'" }
                require(teachersStr == "-") { "Teachers for Cancelled Session must be '-'" }
                CancelSession(date, topic)
            }
        }
        ParseResult.Success(number, session)
    }
    catch (e: IllegalArgumentException){
        ParseResult.Failure(number, Reason.DomainViolation(e.message ?: "invalid session"))
    }
}

fun reasonInfo(failure: ParseResult.Failure): String {
    val num = failure.number
    val reason = failure.reason
    return when (reason) {
        Reason.InvalidSeparator -> "line $num: invalid field separator"
        is Reason.WrongFieldsCount -> "line $num: got ${reason.count} fields, expected 5"
        is Reason.EmptyField -> "line $num: empty required field \"${reason.name}\""
        is Reason.InvalidDate -> "line $num: invalid date \"${reason.value}\""
        is Reason.UnknownSessionType -> "line $num: unknown session type \"${reason.value}\""
        is Reason.ExamNeedsTwoTeachers -> "line $num: type \"EXAM\" requires at least 2 teachers, got ${reason.count}"
        is Reason.DomainViolation -> "line $num: ${reason.message}"
    }
}

fun sessionInfo(session: Session): String {
    val type = when (session) {
        is LectureSession -> SessionType.LECTURE
        is PracticeSession -> SessionType.PRACTICE
        is ExamSession -> SessionType.EXAM
        is CancelSession -> SessionType.CANCELLED
        else -> error("Unknown session type: ${session::class.simpleName}")
    }
    val location = when (session) {
        is LectureSession -> session.location
        is PracticeSession -> session.location
        is ExamSession -> session.location
        is CancelSession -> "-"
        else -> error("Unknown session: ${session::class.simpleName}")
    }
    val teachers = when (session) {
        is LectureSession -> session.teachers.joinToString(", ")
        is PracticeSession -> session.teachers.joinToString(", ")
        is ExamSession -> session.teachers.joinToString(", ")
        is CancelSession -> "-"
        else -> error("Unknown session: ${session::class.simpleName}")
    }
    return "${session.date} | ${type.name} | ${session.topic} | $location | $teachers"
}

