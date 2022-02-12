# Параллельное программирование

## 5. Итеративный параллелизм

### Условие

1. Реализуйте класс `IterativeParallelism`, который будет обрабатывать списки в несколько потоков.

2. В простом варианте должны быть реализованы следующие методы:

    * `minimum(threads, list, comparator)` — первый минимум;

    * `maximum(threads, list, comparator)` — первый максимум;

    * `all(threads, list, predicate)` — проверка, что все элементы списка удовлетворяют предикату;

    * `any(threads, list, predicate)` — проверка, что существует элемент списка, удовлетворяющий предикату.

3. В сложном варианте должны быть дополнительно реализованы следующие методы:

    * `filter(threads, list, predicate)` — вернуть список, содержащий элементы удовлетворяющие предикату;

    * `map(threads, list, function)` — вернуть список, содержащий результаты применения функции;

    * `join(threads, list)` — конкатенация строковых представлений элементов списка.

4. Во все функции передается параметр `threads` — сколько потоков надо использовать при вычислении. Вы можете рассчитывать, что число потоков не велико.

5. Не следует рассчитывать на то, что переданные компараторы, предикаты и функции работают быстро.

6. При выполнении задания **нельзя** использовать _Concurrency Utilities_.

7. Рекомендуется подумать, какое отношение к заданию имеют [моноиды](https://en.wikipedia.org/wiki/Monoid).

### Реализация

[Код](info/kgeorgiy/ja/slastin/concurrent/IterativeParallelism.java)

### Тесты

* простой вариант ✅:
    * запустите [ConcurrentTest](test/ConcurrentTest.java) с аргументами `scalar info.kgeorgiy.ja.slastin.concurrent.IterativeParallelism`

* сложный вариант: ✅
    * запустите [ConcurrentTest](test/ConcurrentTest.java) с аргументами `list info.kgeorgiy.ja.slastin.concurrent.IterativeParallelism`

* продвинутый вариант ✅:
    * запустите [ConcurrentTest](test/ConcurrentTest.java) с аргументами `advanced info.kgeorgiy.ja.slastin.concurrent.IterativeParallelism`


## 6. Параллельный запуск

### Условие

1. Напишите класс `ParallelMapperImpl`, реализующий интерфейс `ParallelMapper`.

```java
public interface ParallelMapper extends AutoCloseable {
        List map(
            Function f,
            List args
        ) throws InterruptedException;

        @Override
        void close() throws InterruptedException;
    } 
```

* Метод `run` должен параллельно вычислять функцию `f` на каждом из указанных аргументов (`args`).

* Метод `close` должен останавливать все рабочие потоки.

* Конструктор `ParallelMapperImpl(int threads)` создает `threads` рабочих потоков, которые могут быть использованы для распараллеливания.

* К одному `ParallelMapperImpl` могут одновременно обращаться несколько клиентов.

* Задания на исполнение должны накапливаться в очереди и обрабатываться в порядке поступления.

* В реализации не должно быть активных ожиданий.

2. Доработайте класс `IterativeParallelism` так, чтобы он мог использовать `ParallelMapper`.

    * Добавьте конструктор `IterativeParallelism(ParallelMapper)`

    * Методы класса должны делить работу на `threads` фрагментов и исполнять их при помощи `ParallelMapper`.

    * При наличии `ParallelMapper` сам `IterativeParallelism` новые потоки создавать не должен.

    * Должна быть возможность одновременного запуска и работы нескольких клиентов, использующих один `ParallelMapper`.

### Реализация

[Код](info/kgeorgiy/ja/slastin/concurrent/ParallelMapperImpl.java)

### Тесты

* простой вариант ✅:
    * запустите [MapperTest](test/MapperTest.java) с аргументами `scalar info.kgeorgiy.ja.slastin.concurrent.IterativeParallelism`

* сложный вариант ✅:
    * запустите [MapperTest](test/MapperTest.java) с аргументами `list info.kgeorgiy.ja.slastin.concurrent.IterativeParallelism`

* продвинутый вариант ✅:
    * запустите [MapperTest](test/MapperTest.java) с аргументами `advanced info.kgeorgiy.ja.slastin.concurrent.IterativeParallelism`
