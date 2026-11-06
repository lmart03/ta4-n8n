CREATE TABLE business_info (
  id uuid PRIMARY KEY,
  business_name varchar(200) NOT NULL,
  description varchar(1000) NOT NULL,
  address varchar(300) NOT NULL,
  district varchar(100) NOT NULL,
  city varchar(100) NOT NULL,
  contact_phone varchar(50),
  appointment_instructions varchar(1000) NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE faq (
  id uuid PRIMARY KEY,
  category varchar(30) NOT NULL CHECK (category IN ('HORARIOS','UBICACION','SERVICIOS','PRECIOS','CONTACTO','CITAS')),
  question varchar(500) NOT NULL,
  answer varchar(1000) NOT NULL,
  keywords varchar(500) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order integer NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_faq_active_order ON faq(active, sort_order);
