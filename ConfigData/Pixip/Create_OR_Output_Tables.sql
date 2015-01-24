DROP TABLE OR_XMASS_CALL_RESULT if exists;
CREATE TABLE OR_XMASS_CALL_RESULT
(
   mtn_cdr_id int NOT NULL,
   or_rated_amount double,
   or_message varchar(255)
);

DROP TABLE OR_BILLING_RESULT if exists;
CREATE TABLE OR_BILLING_RESULT
(
   mtn_cdr_id int NOT NULL,
   or_rated_amount double,
   or_message varchar(255)
);
