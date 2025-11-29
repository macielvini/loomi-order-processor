CREATE TABLE products (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  product_type VARCHAR(20) NOT NULL,
  price DECIMAL(10, 2) NOT NULL,
  stock_quantity INTEGER,
  is_active BOOLEAN DEFAULT true,
  metadata JSONB
);

CREATE INDEX idx_products_product_type ON products(product_type);
CREATE INDEX idx_products_is_active ON products(is_active);

