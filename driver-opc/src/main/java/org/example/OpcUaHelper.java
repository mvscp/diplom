package org.example;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class OpcUaHelper {

    private final OpcUaClient client;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public OpcUaHelper(OpcUaClient client) {
        this.client = client;
    }

    public OpcUaHelper(String host, int port) {
        String serverUrl = "opc.tcp://" + host + ":" + port;

        OpcUaClient client = null;
        try {
            client = OpcUaClient.create(serverUrl);
        } catch (UaException e) {
            throw new RuntimeException(e);
        }

        try {
            client.connect().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        this.client = client;
    }

    /**
     * Чтение значения переменной.
     * @param variableName Имя переменной (в формате строки).
     */
    public Object readVariable(String variableName) {
        NodeId nodeId = NodeId.parse("ns=2;s=" + variableName);

        CompletableFuture<DataValue> future = client.readValue(0, TimestampsToReturn.Both, nodeId);
        DataValue dataValue;
        try {
            dataValue = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return dataValue.getValue().getValue();
    }

    /**
     * Чтение значения string переменной.
     * @param variableName Имя переменной (в формате строки).
     */
    public String readString(String variableName) {
        return String.valueOf(readVariable(variableName));
    }

    /**
     * Чтение значения short переменной.
     * @param variableName Имя переменной (в формате строки).
     */
    public int readShort(String variableName) {
        return Short.parseShort(readString(variableName));
    }

    /**
     * Чтение значения int переменной.
     * @param variableName Имя переменной (в формате строки).
     */
    public int readInt(String variableName) {
        return Integer.parseInt(readString(variableName));
    }

    /**
     * Чтение значения double переменной.
     * @param variableName Имя переменной (в формате строки).
     */
    public double readDouble(String variableName) {
        return Double.parseDouble(readString(variableName));
    }

    /**
     * Чтение значения float переменной.
     * @param variableName Имя переменной (в формате строки).
     */
    public float readFloat(String variableName) {
        return Float.parseFloat(readString(variableName));
    }

    /**
     * Чтение значения bool переменной.
     * @param variableName Имя переменной (в формате строки).
     */
    public boolean readBool(String variableName) {
        return Boolean.parseBoolean(readString(variableName));
    }

    /**
     * Запись значения переменной.
     * @param variableName Имя переменной (в формате строки).
     * @param value Значение для записи (любого поддерживаемого типа).
     */
    public void writeVariable(String variableName, Object value) {
        NodeId nodeId = NodeId.parse("ns=2;s=" + variableName);
        Variant variant = new Variant(value);
        DataValue dataValue = new DataValue(variant, null, null);

        try {
            client.writeValue(nodeId, dataValue).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Завершение работы и отмена подписок.
     */
    public void shutdown() {
        try {
            client.disconnect().get();
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Ошибка при отключении клиента: " + e.getMessage());
        }

        System.out.println("Клиент OPC UA отключен.");
    }

    // jar -cvfm out/artifacts/driver_opc_jar/driver-opc.jar .\src\main\resources\META-INF\MANIFEST.MF -C target/classes .
}
