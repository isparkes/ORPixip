DROP TABLE if exists OR_BILLING_RESULT;
CREATE TABLE OR_BILLING_RESULT
(
   mtn_cdr_id int NOT NULL,
   or_rated_amount double,
   or_message varchar(255)
);

DROP TABLE if exists or_xmass_call_result;
CREATE TABLE or_xmass_call_result
(
   mtn_cdr_id int NOT NULL,
   or_rated_amount double,
   or_message varchar(255)
);

DROP TABLE if exists or_xmass_ftp_result;
CREATE TABLE or_xmass_ftp_result
(
   mtn_cdr_id int NOT NULL,
   or_rated_amount double,
   or_message varchar(255)
);

DROP TABLE if exists or_xmass_http_result;
CREATE TABLE or_xmass_http_result
(
   mtn_cdr_id int NOT NULL,
   or_rated_amount double,
   or_message varchar(255)
);

DROP TABLE if exists or_xmass_sms_result;
CREATE TABLE or_xmass_sms_result
(
   mtn_cdr_id int NOT NULL,
   or_rated_amount double,
   or_message varchar(255)
);
