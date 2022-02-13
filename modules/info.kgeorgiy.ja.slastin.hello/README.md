# Network

## HelloUDP

### Условие

1. Реализуйте клиент и сервер, взаимодействующие по UDP.

2. Класс `HelloUDPClient` должен отправлять запросы на сервер, принимать результаты и выводить их на консоль.
    * Аргументы командной строки:
        1. имя или ip-адрес компьютера, на котором запущен сервер;

        2. номер порта, на который отсылать запросы;

        3. префикс запросов (строка);

        4. число параллельных потоков запросов;

        5. число запросов в каждом потоке.

    * Запросы должны одновременно отсылаться в указанном числе потоков. Каждый поток должен ожидать обработки своего запроса и выводить сам запрос и результат его обработки на консоль. Если запрос не был обработан, требуется послать его заново.

    * Запросы должны формироваться по схеме `<префикс запросов><номер потока>_<номер запроса в потоке>`.

3. Класс `HelloUDPServer` должен принимать задания, отсылаемые классом `HelloUDPClient` и отвечать на них.

    * Аргументы командной строки:

        1. номер порта, по которому будут приниматься запросы;

        2. число рабочих потоков, которые будут обрабатывать запросы.

    * Ответом на запрос должно быть `Hello, <текст запроса>`.

    * Если сервер не успевает обрабатывать запросы, прием запросов может быть временно приостановлен.


### Реализация

- [HelloUDPClient.java](info.kgeorgiy.ja.slastin.hello/HelloUDPClient.java)
- [HelloUDPServer.java](info.kgeorgiy.ja.slastin.hello/HelloUDPServer.java)
- [Utils](info.kgeorgiy.ja.slastin.hello/Utils.java)


### Тесты

Интерфейсы

* `HelloUDPClient` должен реализовывать интерфейс
  [HelloClient](../info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloClient.java)
* `HelloUDPServer` должен реализовывать интерфейс
  [HelloServer](../info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloServer.java)

Тестирование

* простой вариант ✅:
    * клиент: запустите [ClientTest](test/ClientTest.java) с аргументами `client info.kgeorgiy.ja.slastin.hello.HelloUDPClient`
    * сервер: запустите [ServerTest](test/ServerTest.java) с аргументами `server info.kgeorgiy.ja.slastin.hello.HelloUDPServer`

* сложный вариант ✅:
    * на противоположной стороне находится система, дающая ответы на различных языках
    * клиент: запустите [ClientTest](test/ClientTest.java) с аргументами `client-i18n info.kgeorgiy.ja.slastin.hello.HelloUDPClient`
    * сервер: запустите [ServerTest](test/ServerTest.java) с аргументами `server-i18n info.kgeorgiy.ja.slastin.hello.HelloUDPServer`

* продвинутый вариант ✅:
    * на противоположной стороне находится старая система, не полностью соответствующая последней версии спецификации
    * клиент: запустите [ClientTest](test/ClientTest.java) с аргументами `client-evil info.kgeorgiy.ja.slastin.hello.HelloUDPClient`
    * сервер: запустите [ServerTest](test/ServerTest.java) с аргументами `server-evil info.kgeorgiy.ja.slastin.hello.HelloUDPServer`


## HelloNonblockingUDP

### Условие

1. Реализуйте клиент и сервер, взаимодействующие по UDP, используя только неблокирующий ввод-вывод.

2. Класс `HelloUDPNonblockingClient` должен иметь функциональность аналогичную `HelloUDPClient`, но без создания новых потоков.

3. Класс `HelloUDPNonblockingServer` должен иметь функциональность аналогичную `HelloUDPServer`, но все операции с сокетом должны производиться в одном потоке.

4. В реализации не должно быть активных ожиданий, в том числе через `Selector`.

5. Обратите внимание на выделение общего кода старой и новой реализации.

6. _Бонусный вариант_. Клиент и сервер могут перед началом работы выделить O(число потоков) памяти. Выделять дополнительную память во время работы запрещено.


### Реализация

- [HelloUDPNonblockingClient](info.kgeorgiy.ja.slastin.hello/HelloUDPNonblockingClient.java)
- [HelloUDPNonblockingServer](info.kgeorgiy.ja.slastin.hello/HelloUDPNonblockingServer.java)
- [Utils](info.kgeorgiy.ja.slastin.hello/Utils.java)


### Тесты

Интерфейсы

* `HelloUDPNonblockingClient` должен реализовывать интерфейс
  [HelloClient](../info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloClient.java)
* `HelloUDPNonblockingServer` должен реализовывать интерфейс
  [HelloServer](../info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloServer.java)

Тестирование

* простой вариант ✅:
    * клиент: запустите [ClientTest](test/ClientTest.java) с аргументами `client info.kgeorgiy.ja.slastin.hello.HelloUDPNonblockingClient`
    * сервер: запустите [ServerTest](test/ServerTest.java) с аргументами `server info.kgeorgiy.ja.slastin.hello.HelloUDPNonblockingServer`

* сложный вариант ✅:
    * на противоположной стороне находится система, дающая ответы на различных языках
    * клиент: запустите [ClientTest](test/ClientTest.java) с аргументами `client-i18n info.kgeorgiy.ja.slastin.hello.HelloUDPNonblockingClient`
    * сервер: запустите [ServerTest](test/ServerTest.java) с аргументами `server-i18n info.kgeorgiy.ja.slastin.hello.HelloUDPNonblockingServer`

* продвинутый вариант ✅:
    * на противоположной стороне находится старая система, не полностью соответствующая последней версии спецификации
    * клиент: запустите [ClientTest](test/ClientTest.java) с аргументами `client-evil info.kgeorgiy.ja.slastin.hello.HelloUDPNonblockingClient`
    * сервер: запустите [ServerTest](test/ServerTest.java) с аргументами `server-evil info.kgeorgiy.ja.slastin.hello.HelloUDPNonblockingServer`

