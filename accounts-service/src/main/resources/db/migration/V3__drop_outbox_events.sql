SET search_path TO accounts;

-- уведомления теперь шлём прямо в Kafka, outbox больше не нужен
DROP TABLE IF EXISTS outbox_events;
