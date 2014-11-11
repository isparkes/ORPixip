--
-- Routines for database 'Tyfon'
--

--
-- Update a balance counter
--
DELIMITER ;;
/*!50003 DROP PROCEDURE IF EXISTS `sp_UpdateCounter` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`openrate`@`localhost`*/ /*!50003 PROCEDURE `sp_UpdateCounter`
(
  IN IN_BAL_GRP BIGINT,
  IN IN_COUNTER INT,
  IN IN_REC_ID INT,
  IN IN_VALID_FROM INT,
  IN IN_VALID_TO INT,
  IN UPDATE_VAL double
)
BEGIN
  DECLARE rowExist int;
  DECLARE currentBal,newBal double;
  
  select count(*) from COUNTER_BALS where BALANCE_GROUP = IN_BAL_GRP and COUNTER_ID=IN_COUNTER and VALID_FROM=IN_VALID_FROM and VALID_TO=IN_VALID_TO into rowExist;
  If rowExist=0 then
    INSERT INTO COUNTER_BALS (BALANCE_GROUP,COUNTER_ID,RECORD_ID,VALID_FROM,VALID_TO,CURRENT_BAL) VALUES (IN_BAL_GRP,IN_COUNTER,IN_REC_ID,IN_VALID_FROM,IN_VALID_TO,UPDATE_VAL);
  else
    select CURRENT_BAL from COUNTER_BALS where BALANCE_GROUP = IN_BAL_GRP and COUNTER_ID=IN_COUNTER and VALID_FROM=IN_VALID_FROM and VALID_TO=IN_VALID_TO into currentBal;
    set newBal = currentBal + UPDATE_VAL;
    update COUNTER_BALS set CURRENT_BAL = newBal where BALANCE_GROUP = IN_BAL_GRP and COUNTER_ID=IN_COUNTER and VALID_FROM=IN_VALID_FROM and VALID_TO=IN_VALID_TO;
  end if;
END */;;



--
-- Insert a CDR into the OSS_Provisioning table. This routine captures
-- records into 4 separate areas:
--   1 record for each line in the file
--   1 record for each unique alias
--   1 record for each unique audit segment
--   1 record for each unique product.
-- 
-- The post processing then builds the structure needed for OpenRate
--
DELIMITER ;;
/*!50003 DROP PROCEDURE IF EXISTS `sp_UpsertProvRecord` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`openrate`@`localhost`*/ /*!50003 PROCEDURE `sp_UpsertProvRecord`
(
   IN IN_CUSTID         int, 
   IN IN_SERVID         int, 
   IN IN_NUMBER         varchar(24), 
   IN IN_PRICE_PLAN     varchar(64),
   IN IN_VALID_FROM     int, 
   IN IN_VALID_TO       int,
   IN IN_SEG_VALID_FROM int, 
   IN IN_SEG_VALID_TO   int
)
BEGIN
  DECLARE rowExist int;
  DECLARE custId int;
  
  -- removed for full loading
  -- select count(*) from VOIP_SERVICES where CUSTOMER_ID = IN_CUSTID and NUMBER = IN_NUMBER and VALID_FROM = IN_VALID_FROM into rowExist;
  -- If rowExist>0 then
  --   -- Move the existing row into the history table
  --   INSERT INTO VOIP_SERVICES_HISTORY (SELECT *,UNIX_TIMESTAMP() FROM VOIP_SERVICES where CUSTOMER_ID = IN_CUSTID and SERVICE_ID = IN_SERVID and NUMBER = IN_NUMBER and VALID_FROM = IN_VALID_FROM and SEGMENT_VALID_FROM = IN_SEG_VALID_FROM);
  --   DELETE FROM VOIP_SERVICES WHERE CUSTOMER_ID = IN_CUSTID and SERVICE_ID = IN_SERVID and NUMBER = IN_NUMBER and VALID_FROM = IN_VALID_FROM and SEGMENT_VALID_FROM = IN_SEG_VALID_FROM;
  -- end if;

  -- Do the insert
  INSERT INTO VOIP_SERVICES
  (
    CUSTOMER_ID        ,
    SERVICE_ID         ,
    NUMBER             ,
    PRICE_PLAN         ,
    VALID_FROM         ,
    VALID_TO           ,
    SEGMENT_VALID_FROM ,
    SEGMENT_VALID_TO   ,
    ModT
  )
  VALUES 
  (
    IN_CUSTID         ,
    IN_SERVID         ,
    IN_NUMBER         ,
    IN_PRICE_PLAN     ,
    IN_VALID_FROM     ,
    IN_VALID_TO       ,
    IN_SEG_VALID_FROM ,
    IN_SEG_VALID_TO   ,
    UNIX_TIMESTAMP()
  );

  -- Manage the ID tables
  select count(*) from VOIP_SERV_ALIAS_LIST where CUSTOMER_ID = IN_CUSTID and NUMBER = IN_NUMBER and VALID_FROM = IN_SEG_VALID_FROM and VALID_TO = IN_SEG_VALID_TO into rowExist;
  If rowExist=0 then
    -- insert the row to get the ID
    INSERT INTO VOIP_SERV_ALIAS_LIST (CUSTOMER_ID,NUMBER,VALID_FROM,VALID_TO) values (IN_CUSTID,IN_NUMBER,IN_SEG_VALID_FROM,IN_SEG_VALID_TO);
  end if;
  
  select count(*) from VOIP_SERV_AUDIT_LIST where CUSTOMER_ID = IN_CUSTID and SEGMENT_VALID_FROM = IN_SEG_VALID_FROM into rowExist;
  If rowExist=0 then
    -- insert the row to get the ID
    INSERT INTO VOIP_SERV_AUDIT_LIST (CUSTOMER_ID,SEGMENT_VALID_FROM,SEGMENT_VALID_TO) values (IN_CUSTID,IN_SEG_VALID_FROM,IN_SEG_VALID_TO);
  end if;
  
  select count(*) from VOIP_SERV_PRODUCT_LIST where CUSTOMER_ID = IN_CUSTID and SERVICE_ID = IN_SERVID and PRICE_PLAN = IN_PRICE_PLAN and VALID_FROM = IN_VALID_FROM into rowExist;
  If rowExist=0 then
    -- insert the row to get the ID
    INSERT INTO VOIP_SERV_PRODUCT_LIST (CUSTOMER_ID,SERVICE_ID,PRICE_PLAN,VALID_FROM) values (IN_CUSTID, IN_SERVID, IN_PRICE_PLAN, IN_VALID_FROM);
  end if;

END */;;

/*!50003 DROP PROCEDURE IF EXISTS `sp_CreatePriceModels` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`openrate`@`localhost`*/ /*!50003 PROCEDURE `sp_CreatePriceModels`(
)
BEGIN
delete from RATE_PRICE_PIVOT;

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,1,1,1,SETUP_PRICE,1 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Event'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,0,999999999,1,RATE_PRICE,1024 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'MBBeat1kB'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,0,999999,1,RATE_PRICE,60 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Beat1'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,0,0,1,SETUP_PRICE,1 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Setup,Beat1'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,2,0,999999,1,RATE_PRICE,60 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Setup,Beat1'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,0,0,1,SETUP_PRICE,1 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Setup,Beat60'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,2,0,999999,60,RATE_PRICE,60 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Setup,Beat60'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,1,1,1,SETUP_PRICE,1 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Setup,Beat60,Beat1'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,2,0,60,60,RATE_PRICE,60 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Setup,Beat60,Beat1'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,3,60,999999,1,RATE_PRICE,60 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Setup,Beat60,Beat1'
);


insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,0,30,30,RATE_PRICE,60 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Beat30,Beat1'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,2,30,999999,1,RATE_PRICE,60 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Beat30,Beat1'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,0,60,60,RATE_PRICE,60 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Beat60,Beat15'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,2,60,999999,15,RATE_PRICE,60 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Beat60,Beat15'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,1,1,1,SETUP_PRICE,1 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Setup,Markup'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,2,0,999999,0.01,RATE_PRICE,1 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Setup,Markup'
);

insert into RATE_PRICE_PIVOT (PRICE_MODEL,STEP,TIER_FROM,TIER_TO,BEAT,FACTOR,CHARGE_BASE)
(
  select distinct PRICE_MODEL,1,0,999999,0.01,RATE_PRICE,1 from PRICE_MODEL_CALCULATION where RATING_TYPE = 'Markup'
);

delete from PRICE_MODEL;
insert into PRICE_MODEL
(
  select * from RATE_PRICE_PIVOT
);

DELETE FROM RUM_MAP;
INSERT INTO RUM_MAP (PRICE_GROUP,PRICE_MODEL,RUM,RESOURCE,RESOURCE_ID,RUM_TYPE,CONSUME_FLAG,STEP)
(
  select distinct PRICE_MODEL,PRICE_MODEL,'DUR','SEK',888,'TIERED',0,1 from PRICE_MODEL_CALCULATION where RATING_TYPE in ('Setup,Beat60,Beat1','Setup,Beat1','Beat1','Beat30,Beat1','Beat60,Beat15','Setup,Beat60')
);

INSERT INTO RUM_MAP (PRICE_GROUP,PRICE_MODEL,RUM,RESOURCE,RESOURCE_ID,RUM_TYPE,CONSUME_FLAG,STEP)
(
  select distinct PRICE_MODEL,PRICE_MODEL,'VOL','SEK',888,'TIERED',0,1 from PRICE_MODEL_CALCULATION where RATING_TYPE in ('MBBeat1kB')
);

INSERT INTO RUM_MAP (PRICE_GROUP,PRICE_MODEL,RUM,RESOURCE,RESOURCE_ID,RUM_TYPE,CONSUME_FLAG,STEP)
(
  select distinct PRICE_MODEL,PRICE_MODEL,'EVT','SEK',888,'EVENT',0,1 from PRICE_MODEL_CALCULATION where RATING_TYPE in ('Event')
);

INSERT INTO RUM_MAP (PRICE_GROUP,PRICE_MODEL,RUM,RESOURCE,RESOURCE_ID,RUM_TYPE,CONSUME_FLAG,STEP)
(
  select distinct PRICE_MODEL,PRICE_MODEL,'SEK','SEK',888,'TIERED',0,1 from PRICE_MODEL_CALCULATION where RATING_TYPE in ('Markup','Setup,Markup')
);

select concat(count(*),' price models created') from RUM_MAP;

END */;;

--
-- Post process the records we captured during the loading phase to create
-- the image needed by OpenRate. This happens in several short steps:
-- 1) drop and create a master TMP table which serves as a cross reference for
--    the information.
-- 2) Add the identity columns to the temp table and populate them
-- 3) Clear and re-populate the ALIAS,AUDIT and PRODUCT tables
-- 4) Clean out the LIST tables (to avoid that we keep old versions around)
-- 5) Move the loading table into the history area.
-- 
/*!50003 DROP PROCEDURE IF EXISTS `sp_MungeTables` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`openrate`@`localhost`*/ /*!50003 PROCEDURE `sp_MungeTables`(
)
BEGIN
  drop table if exists VOIP_SERV_TMP;
  create table VOIP_SERV_TMP as select * from VOIP_SERVICES;
  alter table VOIP_SERV_TMP add column SEGMENT_ID integer;
  alter table VOIP_SERV_TMP add column ALIAS_ID integer;
  alter table VOIP_SERV_TMP add column PRODUCT_ID integer;

  update VOIP_SERV_TMP vs, 
         VOIP_SERV_AUDIT_LIST vsal 
  set    vs.SEGMENT_ID = vsal.ID
  where vs.CUSTOMER_ID=vsal.CUSTOMER_ID
  and vs.SEGMENT_VALID_FROM=vsal.SEGMENT_VALID_FROM;

  update VOIP_SERV_TMP vs, 
         VOIP_SERV_ALIAS_LIST vsal 
  set    vs.ALIAS_ID = vsal.ID 
  where  vs.CUSTOMER_ID=vsal.CUSTOMER_ID
  and    vs.SEGMENT_VALID_FROM=vsal.VALID_FROM
  and    vs.NUMBER = vsal.NUMBER;

  update VOIP_SERV_TMP vs,
         VOIP_SERV_PRODUCT_LIST vsal
  set    vs.PRODUCT_ID = vsal.ID
  where  vs.CUSTOMER_ID=vsal.CUSTOMER_ID
  and    vs.SERVICE_ID=vsal.SERVICE_ID
  and    vs.VALID_FROM=vsal.VALID_FROM
  and    vs.PRICE_PLAN = vsal.PRICE_PLAN;

  delete from VOIP_SERV_ALIAS;
  insert into VOIP_SERV_ALIAS
  select vsal.ID as ALIAS_ID,
         vsal.NUMBER,
         vst.SERVICE_ID as SUBSCRIPTION_ID,
         vsal.CUSTOMER_ID,
         vsal.VALID_FROM as SEGMENT_VALID_FROM,
         vsal.VALID_TO as SEGMENT_VALID_TO,
         UNIX_TIMESTAMP() as ModT
  from   VOIP_SERV_ALIAS_LIST vsal, 
         VOIP_SERV_TMP vst 
  where  vsal.CUSTOMER_ID=vst.CUSTOMER_ID
  and    vsal.NUMBER=vst.NUMBER
  and    vsal.VALID_FROM=vst.SEGMENT_VALID_FROM;

  delete from VOIP_SERV_AUDIT;
  insert into VOIP_SERV_AUDIT 
  select ID as AUDIT_SEGMENT_ID,
         CUSTOMER_ID,CUSTOMER_ID as CUSTOMER_EXT_ID,
         SEGMENT_VALID_FROM as AUDIT_SEGMENT_VALID_FROM,
         SEGMENT_VALID_FROM,
         SEGMENT_VALID_TO,
         UNIX_TIMESTAMP() as ModT
  from VOIP_SERV_AUDIT_LIST;

  delete from VOIP_SERV_PRODUCT;
  insert into VOIP_SERV_PRODUCT
  select vspl.ID as PRODUCT_ID,
         vst.SEGMENT_ID as AUDIT_SEGMENT_ID,
         vspl.PRICE_PLAN as PRODUCT_NAME,
         vst.SERVICE_ID as SUBSCRIPTION_ID,
         'TEL' as SERVICE,
         vspl.VALID_FROM,
         vst.VALID_TO,
         vst.ModT
  from   VOIP_SERV_PRODUCT_LIST vspl, 
         VOIP_SERV_TMP vst 
  where  vspl.CUSTOMER_ID=vst.CUSTOMER_ID
  and    vspl.SERVICE_ID=vst.SERVICE_ID
  and    vspl.PRICE_PLAN=vst.PRICE_PLAN
  and vspl.VALID_FROM=vst.VALID_FROM;

  -- Clean up the temp tables
  delete from VOIP_SERV_ALIAS_LIST;
  delete from VOIP_SERV_PRODUCT_LIST;
  delete from VOIP_SERV_AUDIT_LIST;

  -- move over the history from the previous run
  INSERT INTO VOIP_SERVICES_HISTORY (SELECT *,UNIX_TIMESTAMP() FROM VOIP_SERVICES);
  drop table if exists VOIP_SERVICES_LAST_LOAD;
  create table VOIP_SERVICES_LAST_LOAD as select * from VOIP_SERVICES;
  DELETE FROM VOIP_SERVICES;

END */;;

DELIMITER ;

