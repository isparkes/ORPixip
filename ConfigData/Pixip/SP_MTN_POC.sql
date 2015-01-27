--
-- Routines for database 'mtn_poc'
--

DELIMITER ;;

/*!50003 DROP PROCEDURE IF EXISTS `sp_UpsertBillingResult` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`root`@`localhost`*/ /*!50003 PROCEDURE `sp_UpsertBillingResult`(
IN IN_MTN_CDR_ID INT,
IN IN_UPDATE_VAL double,
IN IN_OR_MESSAGE varchar(255)
)
BEGIN
DECLARE rowExist int;
select count(*) from or_billing_result where MTN_CDR_ID = IN_MTN_CDR_ID into rowExist;
If rowExist=0 then
  INSERT INTO or_billing_result (MTN_CDR_ID,OR_RATED_AMOUNT,OR_MESSAGE) VALUES (IN_MTN_CDR_ID,IN_UPDATE_VAL,IN_OR_MESSAGE);
else
  UPDATE or_billing_result SET OR_RATED_AMOUNT=IN_UPDATE_VAL,OR_MESSAGE=IN_OR_MESSAGE WHERE MTN_CDR_ID=IN_MTN_CDR_ID;
end if;

UPDATE mtn_billing_cdr SET FIELD3 = '2' where mtn_cdr_id=IN_MTN_CDR_ID;
END */;;

/*!50003 DROP PROCEDURE IF EXISTS `sp_UpsertXMASSCallResult` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`root`@`localhost`*/ /*!50003 PROCEDURE `sp_UpsertXMASSCallResult`(
IN IN_MTN_CDR_ID INT,
IN IN_UPDATE_VAL double,
IN IN_OR_MESSAGE varchar(255)
)
BEGIN
DECLARE rowExist int;
select count(*) from or_xmass_call_result where MTN_CDR_ID = IN_MTN_CDR_ID into rowExist;
If rowExist=0 then
  INSERT INTO or_xmass_call_result (MTN_CDR_ID,OR_RATED_AMOUNT,OR_MESSAGE) VALUES (IN_MTN_CDR_ID,IN_UPDATE_VAL,IN_OR_MESSAGE);
else
  UPDATE or_xmass_call_result SET OR_RATED_AMOUNT=IN_UPDATE_VAL,OR_MESSAGE=IN_OR_MESSAGE WHERE MTN_CDR_ID=IN_MTN_CDR_ID;
end if;

UPDATE xmass_or_cdr_call SET OR_RATE_CONTROL=2 where RESULT_ID=IN_MTN_CDR_ID;
END */;;

DELIMITER ;

