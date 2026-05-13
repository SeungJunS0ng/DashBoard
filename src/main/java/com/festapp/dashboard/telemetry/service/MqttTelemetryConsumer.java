package com.festapp.dashboard.telemetry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mqtt", name = "enabled", havingValue = "true")
public class MqttTelemetryConsumer {

    private static final String EQUIPMENT_TOPIC_PREFIX = "factory/equipment/";
    private static final String TELEMETRY_SUFFIX = "/telemetry";
    private static final String METADATA_SUFFIX = "/metadata";

    private final ObjectMapper objectMapper;
    private final RealTimeDataService realTimeDataService;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void consume(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = String.valueOf(message.getPayload());
        consume(topic, payload);
    }

    void consume(String topic, String payload) {
        if (topic == null || topic.isBlank()) {
            log.warn("MQTT payload skipped because topic is missing");
            return;
        }
        if (payload == null || payload.isBlank()) {
            log.warn("Blank MQTT payload skipped: topic={}", topic);
            return;
        }

        String equipmentName = extractEquipmentName(topic);
        if (equipmentName == null) {
            log.warn("MQTT payload skipped because topic is unsupported: {}", topic);
            return;
        }

        if (topic.endsWith(METADATA_SUFFIX)) {
            log.info("MQTT equipment metadata received: equipmentId={}, payload={}", equipmentName, payload);
            return;
        }

        if (!topic.endsWith(TELEMETRY_SUFFIX)) {
            log.warn("MQTT payload skipped because topic is not telemetry: {}", topic);
            return;
        }

        try {
            SensorDataPayload sensorPayload = toSensorDataPayload(equipmentName, payload);
            boolean processed = realTimeDataService.processSensorData(sensorPayload);
            if (!processed) {
                log.warn("MQTT telemetry payload skipped by processor: equipmentId={}", sensorPayload.getEquipmentId());
            }
        } catch (JsonProcessingException e) {
            log.warn("Invalid MQTT telemetry payload skipped: topic={}, payload={}", topic, payload, e);
        } catch (Exception e) {
            log.error("MQTT telemetry payload processing failed: topic={}, payload={}", topic, payload, e);
        }
    }

    private SensorDataPayload toSensorDataPayload(String equipmentName, String payload)
            throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(payload);
        SensorDataPayload sensorPayload;

        if (root.has("sensors")) {
            sensorPayload = objectMapper.treeToValue(root, SensorDataPayload.class);
        } else {
            sensorPayload = SensorDataPayload.builder()
                    .timestamp(textOrNull(root, "timestamp"))
                    .status(textOrDefault(root, "status", "RUN"))
                    .sensors(flattenSensorValues(root))
                    .build();
        }

        if (sensorPayload.getEquipmentId() == null || sensorPayload.getEquipmentId().isBlank()) {
            sensorPayload.setEquipmentId(equipmentName);
        }
        if (sensorPayload.getStatus() == null || sensorPayload.getStatus().isBlank()) {
            sensorPayload.setStatus("RUN");
        }
        return sensorPayload;
    }

    private List<SensorDataPayload.SensorDetails> flattenSensorValues(JsonNode root) {
        List<SensorDataPayload.SensorDetails> sensors = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (isMetadataField(field.getKey())) {
                continue;
            }
            JsonNode value = field.getValue();
            if (value == null || value.isNull() || value.isObject() || value.isArray()) {
                continue;
            }
            sensors.add(SensorDataPayload.SensorDetails.builder()
                    .sensorId(field.getKey())
                    .value(toPlainValue(value))
                    .build());
        }
        return sensors;
    }

    private Object toPlainValue(JsonNode value) {
        if (value.isNumber()) {
            return value.doubleValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        return value.asText();
    }

    private boolean isMetadataField(String key) {
        return "equipmentId".equals(key)
                || "equipmentEntityId".equals(key)
                || "equipmentName".equals(key)
                || "timestamp".equals(key)
                || "status".equals(key);
    }

    private String extractEquipmentName(String topic) {
        if (!topic.startsWith(EQUIPMENT_TOPIC_PREFIX)) {
            return null;
        }
        if (!topic.endsWith(TELEMETRY_SUFFIX) && !topic.endsWith(METADATA_SUFFIX)) {
            return null;
        }
        String tail = topic.substring(EQUIPMENT_TOPIC_PREFIX.length());
        int slashIndex = tail.indexOf('/');
        if (slashIndex <= 0) {
            return null;
        }
        return tail.substring(0, slashIndex);
    }

    private String textOrNull(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private String textOrDefault(JsonNode root, String fieldName, String defaultValue) {
        String value = textOrNull(root, fieldName);
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
