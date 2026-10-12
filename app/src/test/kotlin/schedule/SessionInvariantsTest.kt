package schedule


import java.time.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class SessionInvariantsTest{
    private val date = LocalDate.of(2026, 9, 28)

    @Test
    fun addBlankTopicSession(){
        assertFailsWith<IllegalArgumentException> {
            PracticeSession(date, " ", "A-207", listOf("Telyakovsky"))
        }
    }

    @Test
    fun addTopicWithSpacesAtStartAndEndSession() {
        assertFailsWith<IllegalArgumentException> {
            PracticeSession(date, " Integrals ", "A-207", listOf("Telyakovsky"))
        }
    }

    @Test
    fun addTopicWithControlChar() {
        assertFailsWith<IllegalArgumentException> {
            PracticeSession(date, "Integrals\u0000", "A-207", listOf("Telyakovsky"))
        }
    }

    @Test
    fun addWrongYearDateSession(){
        assertFailsWith<IllegalArgumentException> {
            PracticeSession(LocalDate.of(10000, 9, 28), "Integrals", "A-207", listOf("Telyakovsky"))
        }
    }

    @Test
    fun addWrongYearDateSession2(){
        assertFailsWith<IllegalArgumentException> {
            PracticeSession(LocalDate.of(0, 9, 28), "Integrals", "A-207", listOf("Telyakovsky"))
        }
    }

    @Test
    fun addSessionWithoutTeacher(){
        assertFailsWith<IllegalArgumentException> {
            LectureSession(date, "Koshi", "A-207", listOf())
        }
    }

    @Test
    fun addSessionWithDashLocation() {
        assertFailsWith<IllegalArgumentException> {
            PracticeSession(date, "Integrals", "-", listOf("Telyakovsky"))
        }
    }

    @Test
    fun addSessionWithBlankTeacherName() {
        assertFailsWith<IllegalArgumentException> {
            LectureSession(date, "Physics", "G-402", listOf(""))
        }
    }

    @Test
    fun addSessionWithTireTeacherName() {
        assertFailsWith<IllegalArgumentException> {
            LectureSession(date, "Physics", "G-402", listOf("-"))
        }
    }

    @Test
    fun addExamWithOneTeacher(){
        assertFailsWith<IllegalArgumentException> {
            ExamSession(date, "Matan", "B-100", listOf("Telyakovsky"))
        }
    }

    @Test
    fun addExamWithTwoSimilarTeachers(){
        assertFailsWith<IllegalArgumentException> {
            ExamSession(date, "Matan", "B-100", listOf("Telyakovsky", "Telyakovsky"))
        }
    }

    @Test
    fun tryToChangeTeachers(){
        val source = mutableListOf("Xram")
        val lecture = LectureSession(date, "Physics", "G-402", source)
        source.add("Kaminin")
        assert(lecture.teachers == listOf("Xram"))
    }

    @Test
    fun tryToChangeTeachers2() {
        val lecture = LectureSession(date, "Physics", "G-402", listOf("Xram"))
        assertFailsWith<UnsupportedOperationException> {
            (lecture.teachers as MutableList).add("Kaminin")
        }
    }

    @Test
    fun repositoryAllReturnsSnapshot() {
        val repository = InMemorySessionRepository()
        val lecture = LectureSession(date, "Linal", "K-1220", listOf("Baikova"))
        repository.add(lecture)

        val snapshot = repository.all()
        repository.add(PracticeSession(date, "Linal", "K-1220", listOf("Baikova")))

        assertEquals(1, snapshot.size)
        assertEquals(2, repository.all().size)
    }

    @Test
    fun repositoryAllReturnsImmutableList() {
        val repository = InMemorySessionRepository()
        repository.add(LectureSession(date, "Linal", "K-1220", listOf("Baikova")))

        assertFailsWith<UnsupportedOperationException> {
            (repository.all() as MutableList).clear()
        }
    }

    @Test
    fun auditRecordsAddAsOneEvent() {
        val audit = AuditedSessionRepository(InMemorySessionRepository())
        audit.add(PracticeSession(date, "OOP", "A-210a", listOf("Ovchar")))

        assertEquals(listOf(1), audit.journal)
        assertEquals(1, audit.total)
    }

    @Test
    fun auditRecordsAddAllAsSizeEvent() {
        val audit = AuditedSessionRepository(InMemorySessionRepository())
        audit.addAll(
            listOf(
                PracticeSession(date, "OOP", "A-210a", listOf("Ovchar")),
                PracticeSession(date, "OOP", "A-210b", listOf("Firer")),
            )
        )

        assertEquals(listOf(2), audit.journal)
        assertEquals(2, audit.total)
    }

    @Test
    fun auditRecordsEmptyAddAllAsZero() {
        val audit = AuditedSessionRepository(InMemorySessionRepository())
        audit.addAll(emptyList())

        assertEquals(listOf(0), audit.journal)
        assertEquals(0, audit.total)
    }

    @Test
    fun auditAllDoesNotWriteToJournal() {
        val audit = AuditedSessionRepository(InMemorySessionRepository())
        audit.add(LectureSession(date, "OOP", "A-210a", listOf("Ovchar")))
        audit.all()
        audit.all()

        assertEquals(listOf(1), audit.journal)
        assertEquals(1, audit.total)
    }
}
