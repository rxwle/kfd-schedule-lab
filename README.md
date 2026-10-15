# KFD Schedule Lab

## Сборка и запуск

```
gradlew.bat build
gradlew.bat test
gradlew.bat installDist
```

Запуск после `installDist`:

```
app\build\install\schedule\bin\schedule.bat errors --file samples/clean.txt
```

Или через Gradle:

```
gradlew.bat run --args="errors --file samples/clean.txt"
```

## Почему class, а не data class

Классы `LectureSession`, `PracticeSession`, `ExamSession`,
`CancelSession` - обычные `class`, не `data class`. У `data class`
автоматически генерируется `copy()`, через который можно создать
объект в обход проверок в конструкторе. У обычного класса `copy()`
нет, объект создаётся только через конструктор, где `init` всё
проверяет. Значит обойти проверки через `copy()` нельзя.

## Почему только add недостаточно для учёта addAll

`AuditedSessionRepository` реализован через `by delegate`, поэтому
`all()` уходит в делегат напрямую. Если переопределить только `add`,
то `addAll` тоже уйдёт в делегат напрямую, и журнал не зафиксирует
пакетное добавление. Поэтому `addAll` переопределяется отдельно:
один вызов `addAll(list)` пишет одну запись со значением `list.size`,
а `addAll(emptyList())` - одну запись со значением `0`.

## Что защищает модель в тестах «сломай модель»

- **Пустая тема** - проверка при создании: `require(topic.isNotBlank())`
  в `checkTopic`.
- **Год 0 или 10000** - проверка при создании:
  `require(date.year in 1..9999)` в `checkDate`.
- **Действующее занятие без преподавателей** - проверка при создании:
  `checkTeachers(..., minCount = 1)` в `LectureSession`/`PracticeSession`.
- **Сдача с одним преподавателем** - проверка при создании:
  `checkTeachers(..., minCount = 2)` в `ExamSession`.
- **Сдача с двумя одинаковыми преподавателями** - проверка при
  создании: `require(teachers.toSet().size == teachers.size)`.
- **Изменение исходного списка после создания занятия** - защита
  коллекции: `List.copyOf` в конструкторе.
- **Изменение состояния занятия через выданный список** - защита
  коллекции: `teachers` возвращает неизменяемый список, `add`/`clear`
  на нём кидают `UnsupportedOperationException`.
- **Обход через `copy`** - `copy()` в модели
  нет, обходить нечего.

## Сообщения об ошибках

Для части причин был дан точный текст, для остальных - придуманы
свои.

Заданные:

- `line N: invalid field separator`
- `line N: got N fields, expected 5`
- `line N: empty required field "..."` (`date`, `type`, `topic`,
  `location` или `teachers`)
- `line N: invalid date "..."`
- `line N: unknown session type "..."`
- `line N: type "EXAM" requires at least 2 teachers, got N`

Придуманные:

- `line N: <сообщение из модели>` - все нарушения домена
  (год не в 1..9999, тема с пробелами по краям, тема с управляющим
  символом, место `"-"`, преподаватель `""` или `"-"`, дубли
  преподавателей) отдают сообщение из `IllegalArgumentException`,
  которое бросает конструктор соответствующего класса.
