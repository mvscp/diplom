package org.example;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Библиотека-обертка для работы с OPC UA сервером через Eclipse Milo
 * Предоставляет упрощенный API для базовых операций чтения/записи
 */
public class OpcUaHelper {

    // Экземпляр OPC UA клиента из библиотеки Eclipse Milo
    private final OpcUaClient client;

    /**
     * Конструктор клиента OPC UA
     * @param host IP-адрес или доменное имя сервера
     * @param port Порт OPC UA сервера (обычно 4840)
     * @throws RuntimeException При ошибках подключения
     */
    public OpcUaHelper(String host, int port) {
        String serverUrl = "opc.tcp://" + host + ":" + port;

        // Инициализация клиента
        OpcUaClient client = null;
        try {
            client = OpcUaClient.create(serverUrl);
        } catch (UaException e) {
            throw new RuntimeException("Ошибка создания клиента", e);
        }

        // Установка соединения
        try {
            client.connect().get(); // Блокирующее подключение
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Ошибка подключения к серверу", e);
        }

        this.client = client;
    }

    /**
     * Базовый метод чтения переменной (возвращает Object)
     * @param variableName Имя переменной в формате OPC UA (пример: "MyVariable")
     * @return Значение переменной или null при ошибке
     */
    public Object readVariable(String variableName) {
        // Формирование NodeId с namespace index=2 и строковым идентификатором
        NodeId nodeId = NodeId.parse("ns=2;s=" + variableName);

        // Асинхронное чтение значения с получением меток времени
        CompletableFuture<DataValue> future = client.readValue(
                0, // Максимальный возраст данных (0 - всегда актуальные)
                TimestampsToReturn.Both, // Запросить метки источника и сервера
                nodeId
        );

        try {
            DataValue dataValue = future.get(); // Блокирующее получение результата
            return dataValue.getValue().getValue();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Ошибка чтения переменной " + variableName, e);
        }
    }

    // Группа методов для типизированного чтения значений ----------------------

    /**
     * Чтение строкового значения
     * @param variableName Имя переменной
     * @return Значение в виде String или пустая строка при ошибке
     */
    public String readString(String variableName) {
        Object value = readVariable(variableName);
        return value != null ? value.toString() : "";
    }

    /**
     * Чтение целого числа (16 бит)
     * @param variableName Имя переменной
     * @return Значение short или 0 при ошибке
     */
    public short readShort(String variableName) {
        try {
            return Short.parseShort(readString(variableName));
        } catch (NumberFormatException e) {
            System.err.println("Ошибка преобразования short: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Чтение целого числа (32 бита)
     * @param variableName Имя переменной
     * @return Значение int или 0 при ошибке
     */
    public int readInt(String variableName) {
        try {
            return Integer.parseInt(readString(variableName));
        } catch (NumberFormatException e) {
            System.err.println("Ошибка преобразования int: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Чтение числа с плавающей точкой двойной точности
     * @param variableName Имя переменной
     * @return Значение double или 0.0 при ошибке
     */
    public double readDouble(String variableName) {
        try {
            return Double.parseDouble(readString(variableName));
        } catch (NumberFormatException e) {
            System.err.println("Ошибка преобразования double: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Чтение числа с плавающей точкой одинарной точности
     * @param variableName Имя переменной
     * @return Значение float или 0.0f при ошибке
     */
    public float readFloat(String variableName) {
        try {
            return Float.parseFloat(readString(variableName));
        } catch (NumberFormatException e) {
            System.err.println("Ошибка преобразования float: " + e.getMessage());
            return 0.0f;
        }
    }

    /**
     * Чтение логического значения
     * @param variableName Имя переменной
     * @return Значение boolean или false при ошибке
     */
    public boolean readBool(String variableName) {
        String value = readString(variableName).toLowerCase();
        return value.equals("true") || value.equals("1");
    }

    /**
     * Запись значения в переменную OPC UA
     * @param variableName Имя целевой переменной
     * @param value Значение (поддерживаемые типы: Boolean, Number, String)
     * @throws RuntimeException При ошибках записи
     */
    public void writeVariable(String variableName, Object value) {
        // Формирование NodeId аналогично методу чтения
        NodeId nodeId = NodeId.parse("ns=2;s=" + variableName);

        // Создание Variant с автоматическим определением типа
        Variant variant = new Variant(value);

        // DataValue без меток времени и статуса
        DataValue dataValue = new DataValue(variant, null, null);

        try {
            // Асинхронная запись с блокировкой до завершения
            client.writeValue(nodeId, dataValue).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Ошибка записи в переменную " + variableName, e);
        }
    }

    /**
     * Безопасное отключение от сервера
     * Должен вызываться перед завершением работы приложения
     */
    public void shutdown() {
        try {
            // Асинхронное отключение с ожиданием завершения
            client.disconnect().get();
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Ошибка при отключении: " + e.getMessage());
        }
        System.out.println("Соединение с OPC UA сервером закрыто");
    }

    // Пример команды для сборки JAR-файла
    // jar -cvfm out/artifacts/driver_opc_jar/driver-opc.jar .\src\main\resources\META-INF\MANIFEST.MF -C target/classes .
}
