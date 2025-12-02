CREATE TABLE order_events (
  id BIGSERIAL PRIMARY KEY,
  event_id UUID NOT NULL,
  order_id UUID NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  order_status VARCHAR(30) NOT NULL,
  payload JSONB NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_order_events_event_id ON order_events(event_id);

CREATE INDEX idx_order_events_order_id ON order_events(order_id);


