package schedule

class AuditedSessionRepository (
    private val delegate: SessionRepository
) : SessionRepository by delegate {
    private val events = mutableListOf<Int>()
    private var count = 0

    val journal: List<Int> get() = java.util.List.copyOf(events)
    val total: Int get() = count

    override fun add(session: Session) {
        delegate.add(session)
        events.add(1)
        count += 1
    }

    override fun addAll(sessions: List<Session>) {
        delegate.addAll(sessions)
        events.add(sessions.size)
        count += sessions.size
    }
}