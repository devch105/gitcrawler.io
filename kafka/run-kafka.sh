
#!/bin/bash

set -e

CONTAINER="notification-kafka"
TOPIC="notification-events"
COMPOSE_FILE="docker-compose.yml"

echo "=================================="
echo " Starting Kafka Infrastructure"
echo "=================================="

# 1. Start Kafka
docker compose -f "$COMPOSE_FILE" up -d

echo "Waiting for Kafka to become healthy..."

# 2. Wait for Kafka health
for i in {1..60}; do
    STATUS=$(docker inspect \
        --format='{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo "starting")

    if [ "$STATUS" = "healthy" ]; then
        echo "Kafka is healthy!"
        break
    fi

    if [ "$i" -eq 60 ]; then
        echo "Kafka failed to become healthy."
        docker logs "$CONTAINER"
        exit 1
    fi

    sleep 2
done

# 3. Create topic if it doesn't exist
echo "Creating Kafka topic..."

docker exec "$CONTAINER" \
    /opt/kafka/bin/kafka-topics.sh \
    --create \
    --if-not-exists \
    --topic "$TOPIC" \
    --bootstrap-server localhost:9092 \
    --partitions 3 \
    --replication-factor 1

echo "Topic ready: $TOPIC"

# 4. Open Consumer terminal first
echo "Opening Kafka Consumer..."

gnome-terminal --title="Kafka Consumer" -- bash -lc "
docker exec -it $CONTAINER \
/opt/kafka/bin/kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic $TOPIC \
--from-beginning \
--property print.key=true;
exec bash
"

sleep 2
#
## 5. Open Producer terminal
#echo "Opening Kafka Producer..."
#
#gnome-terminal --title="Kafka Producer" -- bash -lc "
#docker exec -it $CONTAINER \
#/opt/kafka/bin/kafka-console-producer.sh \
#--bootstrap-server localhost:9092 \
#--topic $TOPIC \
#--property parse.key=true \
#--property key.separator=:;
#exec bash
#"

echo "=================================="
echo " Kafka is ready!"
#echo "  Consumer opened."
echo "=================================="
