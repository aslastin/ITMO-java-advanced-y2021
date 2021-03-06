# Обход файлов

## Условие

1. Разработайте класс `Walk`, осуществляющий подсчет хеш-сумм файлов.
    1. Формат запуска

       `java Walk <входной файл> <выходной файл>`

    2. Входной файл содержит список файлов, которые требуется обойти.

    3. Выходной файл должен содержать по одной строке для каждого файла. Формат строки:

       `<шестнадцатеричная хеш-сумма> <путь к файлу>`

    4. Для подсчета хеш-суммы используйте 64-битную версию алгоритма [PJW](https://en.wikipedia.org/wiki/PJW\_hash\_function).

    5. Если при чтении файла возникают ошибки, укажите в качестве его хеш-суммы `0000000000000000`.

    6. Кодировка входного и выходного файлов — UTF-8.

    7. Если родительская директория выходного файла не существует, то соответствующий путь надо создать.

    8. Размеры файлов могут превышать размер оперативной памяти.

    9. Пример

       Входной файл

        ```sh
        samples/1
        samples/12
        samples/123
        samples/1234
        samples/1
        samples/binary
        samples/no-such-file
        ```               

       Выходной файл

        ```sh
        0000000000000031 samples/1
        0000000000003132 samples/12
        0000000000313233 samples/123
        0000000031323334 samples/1234
        0000000000000031 samples/1
        005501015554abff samples/binary
        0000000000000000 samples/no-such-file
        ```

2. Сложный вариант ✅:
    1. Разработайте класс `RecursiveWalk`, осуществляющий подсчет хеш-сумм файлов в директориях

    2. Входной файл содержит список файлов и директорий, которые требуется обойти. Обход директорий осуществляется рекурсивно.

    3. Пример

       Входной файл

        ```sh
        samples/binary
        samples
        samples/no-such-file
        ```

       Выходной файл

        ```sh
        005501015554abff samples/binary
        0000000000000031 samples/1    
        0000000000003132 samples/12
        0000000000313233 samples/123
        0000000031323334 samples/1234
        005501015554abff samples/binary
        0000000000000000 samples/no-such-file
        ```

3. При выполнении задания следует обратить внимание на:
    * Дизайн и обработку исключений, диагностику ошибок.

    * Программа должна корректно завершаться даже в случае ошибки.

    * Корректная работа с вводом-выводом.

    * Отсутствие утечки ресурсов.


## Реализация

- [RecursiveWalk](info/kgeorgiy/ja/slastin/walk/RecursiveWalk.java)
- [Pjw64WriterVisitor](info/kgeorgiy/ja/slastin/walk/Pjw64WriterVisitor.java)
- [Walk](info/kgeorgiy/ja/slastin/walk/Walk.java)
- [WalkException](info/kgeorgiy/ja/slastin/walk/WalkException.java)

## Тесты

* простой вариант (`Walk`) ✅
  * запустите [WalkerTest](test/WalkerTest.java) с аргументами `Walk info.kgeorgiy.ja.slastin.walk.RecursiveWalk`
* сложный вариант (`RecursiveWalk`) ✅
  * запустите [WalkerTest](test/WalkerTest.java) с аргументами `RecursiveWalk info.kgeorgiy.ja.slastin.walk.RecursiveWalk`
* продвинутый вариант (`AdvancedWalk`) ✅
  * запустите [WalkerTest](test/WalkerTest.java) с аргументами `AdvancedWalk info.kgeorgiy.ja.slastin.walk.RecursiveWalk`
