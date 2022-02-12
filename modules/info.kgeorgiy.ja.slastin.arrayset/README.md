# 2. Множество на массиве

## Условие

1. Разработайте класс `ArraySet`, реализующие неизменяемое упорядоченное множество.

    * Класс `ArraySet` должен реализовывать интерфейс `SortedSet` (простой вариант) или `NavigableSet` (сложный вариант).

    * Все операции над множествами должны производиться с максимально возможной асимптотической эффективностью.

2. При выполнении задания следует обратить внимание на:

    * Применение стандартных коллекций.

    * Избавление от повторяющегося кода.

## Реализация

[Код](info/kgeorgiy/ja/slastin/arrayset)

## Тесты

Исходный код

* простой вариант (`SortedSet`) ✅:
    * запустите [ArraySetTest](test/ArraySetTest.java) с аргументами `SortedSet info.kgeorgiy.ja.slastin.arrayset.ArraySet`
* сложный вариант (`NavigableSet`) ✅:
    * запустите [ArraySetTest](test/ArraySetTest.java) с аргументами `NavigableSet info.kgeorgiy.ja.slastin.arrayset.ArraySet`
* продвинутый вариант (`AdvancedSet`) ✅:
    * запустите [AdvancedSet](test/ArraySetTest.java) с аргументами `AdvancedSet info.kgeorgiy.ja.slastin.arrayset.ArraySet`
