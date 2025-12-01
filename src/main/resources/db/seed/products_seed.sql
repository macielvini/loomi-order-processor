-- Dev/local seed for initial products.
-- This is optimized for speed; for long-term catalog management prefer Flyway or a dedicated import pipeline.

-- Produtos Físicos
INSERT INTO products (id, name, product_type, price, stock_quantity, is_active, metadata) VALUES
  (gen_random_uuid(), 'Clean Code',       'PHYSICAL',   89.90,   150, true, '{"productId": "BOOK-CC-001"}'),
  (gen_random_uuid(), 'Laptop Pro',       'PHYSICAL', 5499.00,     8, true, '{"productId": "LAPTOP-PRO-2024"}'),
  (gen_random_uuid(), 'MacBook Pro M3',   'PHYSICAL',12999.00,    25, true, '{"productId": "LAPTOP-MBP-M3-001"}');

-- Assinaturas
INSERT INTO products (id, name, product_type, price, stock_quantity, is_active, metadata) VALUES
  (gen_random_uuid(), 'Netflix Basic',           'SUBSCRIPTION',  19.90, NULL, true, '{"GROUP_ID": "NETFLIX"}'),
  (gen_random_uuid(), 'Netflix Premium',         'SUBSCRIPTION',  49.90, NULL, true, '{"GROUP_ID": "NETFLIX",}'),
  (gen_random_uuid(), 'Adobe Cloud Pro',         'SUBSCRIPTION', 59.00, NULL, true, '{"GROUP_ID": "ADOBE"}'),
  (gen_random_uuid(), 'Adobe Cloud Enterprise',  'SUBSCRIPTION', 399.00, NULL, true, '{"GROUP_ID": "ADOBE"}');

-- Digitais
INSERT INTO products (id, name, product_type, price, stock_quantity, is_active, metadata) VALUES
  (gen_random_uuid(), 'Effective Java',         'DIGITAL',  39.90, NULL, true, '{"productId": "EBOOK-JAVA-001", "licenses": 1000}'),
  (gen_random_uuid(), 'Domain-Driven Design',   'DIGITAL',  59.90, NULL, true, '{"productId": "EBOOK-DDD-001", "licenses": 500}'),
  (gen_random_uuid(), 'Swift Programming',      'DIGITAL',  49.90, NULL, true, '{"productId": "EBOOK-SWIFT-001", "licenses": 800}'),
  (gen_random_uuid(), 'Kafka Mastery',          'DIGITAL', 299.00, NULL, true, '{"productId": "COURSE-KAFKA-001", "licenses": 500}');

-- Pré-venda
INSERT INTO products (id, name, product_type, price, stock_quantity, is_active, metadata) VALUES
  (gen_random_uuid(), 'Epic Game 2025',  'PRE_ORDER',  249.90, NULL, true,
   '{"productId": "GAME-2025-001", "releaseDate": "2025-06-01", "preOrderSlots": 1000}'),
  (gen_random_uuid(), 'PlayStation 6',   'PRE_ORDER', 4999.00, NULL, true,
   '{"productId": "PRE-PS6-001", "releaseDate": "2025-11-15", "preOrderSlots": 500}'),
  (gen_random_uuid(), 'iPhone 16 Pro',   'PRE_ORDER', 7999.00, NULL, true,
   '{"productId": "PRE-IPHONE16-001", "releaseDate": "2025-09-20", "preOrderSlots": 2000}');

-- Corporativo
INSERT INTO products (id, name, product_type, price, stock_quantity, is_active, metadata) VALUES
  (gen_random_uuid(), 'Enterprise License',   'CORPORATE', 15000.00, NULL, true,
   '{"productId": "CORP-LICENSE-ENT"}'),
  (gen_random_uuid(), 'Ergonomic Chair Bulk', 'CORPORATE',   899.00, 500, true,
   '{"productId": "CORP-CHAIR-ERG-001"}');


