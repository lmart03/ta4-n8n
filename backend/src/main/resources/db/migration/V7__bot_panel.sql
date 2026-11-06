-- Panel del bot: chats y mensajes (con retención), métricas sin contenido y ajustes.
CREATE TABLE bot_chat (
  id uuid PRIMARY KEY,
  telegram_chat_id bigint NOT NULL UNIQUE,
  display_name varchar(200) NOT NULL,
  username varchar(100),
  mode varchar(10) NOT NULL DEFAULT 'BOT' CHECK (mode IN ('BOT','HUMANO')),
  test boolean NOT NULL DEFAULT false,
  last_message_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE bot_message (
  id uuid PRIMARY KEY,
  chat_id uuid NOT NULL REFERENCES bot_chat(id) ON DELETE CASCADE,
  author varchar(10) NOT NULL CHECK (author IN ('USER','BOT','HUMANO')),
  text varchar(4100) NOT NULL,
  source varchar(20),
  latency_ms integer,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_bot_message_chat ON bot_message(chat_id, created_at);

CREATE TABLE bot_metric_event (
  id uuid PRIMARY KEY,
  at timestamptz NOT NULL DEFAULT now(),
  source varchar(20) NOT NULL,
  latency_ms integer,
  test boolean NOT NULL DEFAULT false
);
CREATE INDEX idx_bot_metric_at ON bot_metric_event(at);

CREATE TABLE bot_settings (
  id integer PRIMARY KEY CHECK (id = 1),
  store_conversations boolean NOT NULL DEFAULT true,
  retention_hours integer NOT NULL DEFAULT 24 CHECK (retention_hours BETWEEN 1 AND 720),
  auto_register_webhook boolean NOT NULL DEFAULT true
);
INSERT INTO bot_settings (id) VALUES (1);
