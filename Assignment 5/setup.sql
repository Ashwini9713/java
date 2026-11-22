-- Run once:  mysql -u root -p < setup.sql      (test database used ONLY for this assignment)
CREATE DATABASE IF NOT EXISTS bus_pass_test;
USE bus_pass_test;
CREATE TABLE IF NOT EXISTS bus_pass (
  pass_id  VARCHAR(10)  PRIMARY KEY,
  reg_no   VARCHAR(12)  NOT NULL,
  name     VARCHAR(60)  NOT NULL,
  route_no INT          NOT NULL,
  amount   DOUBLE       NOT NULL,
  INDEX idx_route (route_no)
);
-- tables student, wallet, users are created by the Java programs themselves.
