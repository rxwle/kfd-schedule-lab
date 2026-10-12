package schedule

import java.time.LocalDate

interface Session {
    val date: LocalDate
    val topic: String
}

private fun checkDate(date: LocalDate){
    require(date.year in 1..9999) {"Year must be between 1 and 9999; current date is $date"}
}

private fun checkTopic(topic: String){
    require(topic.isNotBlank()) { "Topic must not be blank" }
    require(topic == topic.trim()) { "Topic must not have spaces at start or in the end" }
    require(topic.none { it.isISOControl() }) { "Topic must not contain control chars" }
}

private fun checkLocation(location: String) {
    require(location.isNotBlank()) { "Location must not be blank" }
    require(location != "-") { "Location must not be \"-\"" }
}

private fun checkTeachers(teachers: List<String>, minCount: Int) {
    require(teachers.size >= minCount) {
        "Need at least $minCount teacher(s), got ${teachers.size}"
    }
    require(teachers.none { it.isBlank() }) { "Teacher name must not be blank" }
    require(teachers.none { it == "-" }) { "Teacher name must not be \"-\"" }
    require(teachers.toSet().size == teachers.size) { "Duplicate teachers" }
}

private fun snapshot(list: List<String>): List<String> = java.util.List.copyOf(list)

class LectureSession(
    override val date: LocalDate,
    override val topic: String,
    val location: String,
    teachers: List<String>
) : Session{
    val teachers = snapshot(teachers)
    init {
        checkDate(date)
        checkTopic(topic)
        checkLocation(location)
        checkTeachers(this.teachers, 1)
    }
}

class PracticeSession(
    override val date: LocalDate,
    override val topic: String,
    val location: String,
    teachers: List<String>
) : Session{
    val teachers = snapshot(teachers)
    init {
        checkDate(date)
        checkTopic(topic)
        checkLocation(location)
        checkTeachers(this.teachers, 1)
    }
}

class ExamSession(
    override val date: LocalDate,
    override val topic: String,
    val location: String,
    teachers: List<String>
) : Session{
    val teachers = snapshot(teachers)
    init {
        checkDate(date)
        checkTopic(topic)
        checkLocation(location)
        checkTeachers(this.teachers, 2)
    }
}

class CancelSession(
    override val date: LocalDate,
    override val topic: String,
) : Session{
    init {
        checkDate(date)
        checkTopic(topic)
    }
}