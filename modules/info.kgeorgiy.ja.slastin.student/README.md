# 3. Студенты

## Условие

1. Разработайте класс `StudentDB`, осуществляющий поиск по базе данных студентов.

    * Класс `StudentDB` должен реализовывать интерфейс `StudentQuery` (простой вариант) или `GroupQuery` (сложный вариант).

    * Каждый метод должен состоять из ровно одного оператора. При этом длинные операторы надо разбивать на несколько строк.

2. При выполнении задания следует обратить внимание на:

    * применение лямбда-выражений и потоков;

    * избавление от повторяющегося кода.

## Реализация

[Код](info/kgeorgiy/ja/slastin/student)

## Тесты

Исходный код

* простой вариант (`StudentQuery`) ✅:
    * запустите [StudentTest](test/StudentTest.java) с аргументами `StudentQuery info.kgeorgiy.ja.slastin.student.StudentDB`
* сложный вариант (`GroupQuery`) ✅:
    * запустите [StudentTest](test/StudentTest.java) с аргументами `GroupQuery info.kgeorgiy.ja.slastin.student.StudentDB`
* продвинутый вариант (`AdvancedQuery`) ✅:
    * запустите [StudentTest](test/StudentTest.java) с аргументами `AdvancedQuery info.kgeorgiy.ja.slastin.student.StudentDB`
