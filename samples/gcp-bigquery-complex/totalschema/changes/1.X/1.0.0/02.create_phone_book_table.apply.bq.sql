CREATE TABLE IF NOT EXISTS `${gcpProjectId}.${bqDataset}.phone_book` (
  id INT64 NOT NULL,
  name STRING,
  phone_number STRING
);

