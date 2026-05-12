package com.festapp.dashboard.telemetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("Telemetry Consumer 단위 테스트")
class TelemetryConsumerTest {

    @Test
    @DisplayName("RabbitMQ consumer는 payload를 RealTimeDataService로 전달한다")
    void rabbitConsumerDelegatesPayload() {
        RealTimeDataService realTimeDataService = mock(RealTimeDataService.class);
        RabbitTelemetryConsumer consumer = new RabbitTelemetryConsumer(realTimeDataService);
        SensorDataPayload payload = SensorDataPayload.builder()
                .equipmentEntityId(1L)
                .equipmentId("CVD-01")
                .build();

        consumer.consume(payload);

        verify(realTimeDataService).processSensorData(payload);
    }

    @Test
    @DisplayName("Kafka consumer는 JSON payload를 파싱해 RealTimeDataService로 전달한다")
    void kafkaConsumerParsesJsonAndDelegatesPayload() {
        RealTimeDataService realTimeDataService = mock(RealTimeDataService.class);
        KafkaTelemetryConsumer consumer = new KafkaTelemetryConsumer(new ObjectMapper(), realTimeDataService);

        consumer.consume("""
                {
                  "equipmentEntityId": 1,
                  "equipmentId": "CVD-01",
                  "status": "RUN",
                  "sensors": [
                    {"sensorId": "Temp_0", "value": 971.5, "unit": "C"}
                  ]
                }
                """);

        verify(realTimeDataService).processSensorData(argThat(payload ->
                payload.getEquipmentEntityId().equals(1L)
                        && payload.getEquipmentId().equals("CVD-01")
                        && payload.getSensors().size() == 1
        ));
    }

    @Test
    @DisplayName("Kafka consumer는 잘못된 JSON payload를 건너뛴다")
    void kafkaConsumerSkipsInvalidJson() {
        RealTimeDataService realTimeDataService = mock(RealTimeDataService.class);
        KafkaTelemetryConsumer consumer = new KafkaTelemetryConsumer(new ObjectMapper(), realTimeDataService);

        consumer.consume("{bad-json");

        verify(realTimeDataService, never()).processSensorData(any());
    }
}
