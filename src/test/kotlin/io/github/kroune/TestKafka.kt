package io.github.kroune

import org.testcontainers.kafka.KafkaContainer

object TestKafka {
    var kafka = KafkaContainer("apache/kafka-native:3.8.0")

    init {
        kafka.start()
    }
}
