-- Example 3: SQL file for creating a table
CREATE TABLE my_dataset.orders (
  order_id INT64 NOT NULL,
  user_id INT64 NOT NULL,
  product_id INT64 NOT NULL,
  quantity INT64,
  total_amount NUMERIC(10, 2),
  order_date DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);

