package schedule

interface CommandHandlers {
    fun period(command: Command.Period): Int
    fun byType(command: Command.ByType): Int
    fun errors(command: Command.Errors): Int
}

object StudentCommandHandlers : CommandHandlers {
    override fun period(command: Command.Period): Int = runPeriod(command)
    override fun byType(command: Command.ByType): Int = runType(command)
    override fun errors(command: Command.Errors): Int = runErrors(command)
}

fun runPeriod(command: Command.Period): Int {
    var counter = 0
    var rejected = false

    forEachScheduleLine(command.file) { number, line ->
        val res = parseScheduleLine(number, line)
        if (res is ParseResult.Success) {
            val session = res.session
            if (session.date >= command.from && session.date <= command.to) {
                println(SessionInfo(session))
                counter++
            }
        }
        else {
            rejected = true
        }
    }
    println("Total: $counter")
    return if (rejected) 1 else 0
}

fun runType(command: Command.ByType): Int {
    var counter = 0
    var rejected = false

    forEachScheduleLine(command.file) { number, line ->
        val res = parseScheduleLine(number, line)
        if (res is ParseResult.Success) {
            val session = res.session
            val sessiontype = when (command.type) {
                SessionType.LECTURE -> session is LectureSession
                SessionType.PRACTICE -> session is PracticeSession
                SessionType.EXAM -> session is ExamSession
                SessionType.CANCELLED -> session is CancelSession
            }
            if (sessiontype) {
                println(SessionInfo(session))
                counter++
            }
        }
        else {
            rejected = true
        }
    }
    println("Total: $counter")
    return if (rejected) 1 else 0
}

fun runErrors(command: Command.Errors): Int {
    var total = 0
    var rejected = 0
    forEachScheduleLine(command.file) { number, line ->
        total++
        val res = parseScheduleLine(number, line)
        if (res is ParseResult.Failure) {
            println(ReasonInfo(res))
            rejected++
        }
    }
    println("Rejected: $rejected of $total")
    return if (rejected > 0) 1 else 0
}
