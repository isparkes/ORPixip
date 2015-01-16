/* ====================================================================
 * Limited Evaluation License:
 *
 * This software is open source, but licensed. The license with this package
 * is an evaluation license, which may not be used for productive systems. If
 * you want a full license, please contact us.
 *
 * The exclusive owner of this work is the OpenRate project.
 * This work, including all associated documents and components
 * is Copyright of the OpenRate project 2006-2014.
 *
 * The following restrictions apply unless they are expressly relaxed in a
 * contractual agreement between the license holder or one of its officially
 * assigned agents and you or your organisation:
 *
 * 1) This work may not be disclosed, either in full or in part, in any form
 *    electronic or physical, to any third party. This includes both in the
 *    form of source code and compiled modules.
 * 2) This work contains trade secrets in the form of architecture, algorithms
 *    methods and technologies. These trade secrets may not be disclosed to
 *    third parties in any form, either directly or in summary or paraphrased
 *    form, nor may these trade secrets be used to construct products of a
 *    similar or competing nature either by you or third parties.
 * 3) This work may not be included in full or in part in any application.
 * 4) You may not remove or alter any proprietary legends or notices contained
 *    in or on this work.
 * 5) This software may not be reverse-engineered or otherwise decompiled, if
 *    you received this work in a compiled form.
 * 6) This work is licensed, not sold. Possession of this software does not
 *    imply or grant any right to you.
 * 7) You agree to disclose any changes to this work to the copyright holder
 *    and that the copyright holder may include any such changes at its own
 *    discretion into the work
 * 8) You agree not to derive other works from the trade secrets in this work,
 *    and that any such derivation may make you liable to pay damages to the
 *    copyright holder
 * 9) You agree to use this software exclusively for evaluation purposes, and
 *    that you shall not use this software to derive commercial profit or
 *    support your business or personal activities.
 *
 * This software is provided "as is" and any expressed or impled warranties,
 * including, but not limited to, the impled warranties of merchantability
 * and fitness for a particular purpose are disclaimed. In no event shall
 * The OpenRate Project or its officially assigned agents be liable to any
 * direct, indirect, incidental, special, exemplary, or consequential damages
 * (including but not limited to, procurement of substitute goods or services;
 * Loss of use, data, or profits; or any business interruption) however caused
 * and on theory of liability, whether in contract, strict liability, or tort
 * (including negligence or otherwise) arising in any way out of the use of
 * this software, even if advised of the possibility of such damage.
 * This software contains portions by The Apache Software Foundation, Robert
 * Half International.
 * ====================================================================
 */
package Pixip;

import OpenRate.record.ErrorType;
import OpenRate.record.RatingRecord;
import OpenRate.record.RecordError;
import static Pixip.TeleserviceCode.UNKNOWN;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * A Record corresponds to a unit of work that is being processed by the
 * pipeline. Records are created in the InputAdapter, pass through the Pipeline,
 * and written out in the OutputAdapter. Any stage of the pipeline my update the
 * record in any way, provided that later stages in the processing and the
 * output adapter know how to treat the record they receive.
 *
 * As an alternative, you may define a less flexible record format as you wish
 *
 * and fill in the fields as required, but this costs performance.
 *
 * Generally, the record should know how to handle the following operations by
 * linking the appropriate method:
 *
 * mapOriginalData() [mandatory] ----------------- Transformation from a flat
 * record as read by the input adapter to a formatted record.
 *
 * unmapOriginalData() [mandatory if you wish to write output files]
 * ------------------- Transformation from a formatted record to a flat record
 * ready for output.
 *
 * getDumpInfo() [optional] ------------- Preparation of the dump equivalent of
 * the formatted record, ready for dumping out to a dump file.
 *
 * In this simple example, we require only to read the "B-Number", and write the
 * "destination" as a result of this. Because of the simplicity of the example
 * we do not perform a full mapping, we just handle the fields we want directly,
 * which is one of the advantages of the BBPA model (map as much as you want or
 * as little as you have to).
 *
 */
public class PixipRecord extends RatingRecord {
  // **************************** Ventelo Definition ***************************

  // Character used as a field splitter
  private static final String FILE_FIELD_SPLITTER = ",";

  /**
   * Internal identifier for the header record
   */
  public final static int FILE_HEADER_RECORD = 10;

  /**
   * Internal identifier for the detail record
   */
  public final static int FILE_DETAIL_RECORD = 20;

  /**
   * Internal identifier for the trailer record
   */
  public final static int FILE_TRAILER_RECORD = 90;

  // Detail Records
  private final static int FIELD_V_COUNT = 80;
  private final static int IDX_mtn_cdr_id = 0; //  0
  private final static int IDX_ANUMBER = 1; //  1
  private final static int IDX_BNUMBER = 2; //  2
  private final static int IDX_CALL_DATE = 3; //  3
  private final static int IDX_CALL_DURATION = 4; //  4
  private final static int IDX_IMEI = -1; //  5
  private final static int IDX_IMSI = -1; //  6
  private final static int IDX_CHARGE_MAIN_ACCT = 5; //  7
  private final static int IDX_ACCT_VALUE_BEFORE_CALL = -1; //  8
  private final static int IDX_ACCT_VALUE_AFTER_CALL = -1; //  9
  private final static int IDX_CHARGE_DA1 = -1; // 10
  private final static int IDX_DA1_ACCT_BAL_BEFORE_CALL = -1; // 11
  private final static int IDX_DA1_ACCT_BAL_AFTER_CALL = -1; // 12
  private final static int IDX_CHARGE_DA2 = -1; // 13
  private final static int IDX_DA2_ACCT_BAL_BEFORE_CALL = -1; // 14
  private final static int IDX_DA2_ACCT_BAL_AFTER_CALL = -1; // 15
  private final static int IDX_CHARGE_DA3 = -1; // 16
  private final static int IDX_DA3_ACCT_BAL_BEFORE_CALL = -1; // 17
  private final static int IDX_DA3_ACCT_BAL_AFTER_CALL = -1; // 18
  private final static int IDX_CHARGE_DA4 = -1; // 19
  private final static int IDX_DA4_ACCT_BAL_BEFORE_CALL = -1; // 20
  private final static int IDX_DA4_ACCT_BAL_AFTER_CALL = -1; // 21
  private final static int IDX_CHARGE_DA5 = -1; // 22
  private final static int IDX_DA5_ACCT_BAL_BEFORE_CALL = -1; // 23
  private final static int IDX_DA5_ACCT_BAL_AFTER_CALL = -1; // 24
  private final static int IDX_CHARGE_DA6 = -1; // 25
  private final static int IDX_DA6_ACCT_BAL_BEFORE_CALL = -1; // 26
  private final static int IDX_DA6_ACCT_BAL_AFTER_CALL = -1; // 27
  private final static int IDX_CHARGE_DA7 = -1; // 28
  private final static int IDX_DA7_ACCT_BAL_BEFORE_CALL = -1; // 29
  private final static int IDX_DA7_ACCT_BAL_AFTER_CALL = -1; // 30
  private final static int IDX_CHARGE_DA8 = -1; // 31
  private final static int IDX_DA8_ACCT_BAL_BEFORE_CALL = -1; // 32
  private final static int IDX_DA8_ACCT_BAL_AFTER_CALL = -1; // 33
  private final static int IDX_CHARGE_DA9 = -1; // 34
  private final static int IDX_DA9_ACCT_BAL_BEFORE_CALL = -1; // 35
  private final static int IDX_DA9_ACCT_BAL_AFTER_CALL = -1; // 36
  private final static int IDX_CHARGE_DA10 = -1; // 37
  private final static int IDX_DA10_ACCT_BAL_BEFORE_CALL = -1; // 38
  private final static int IDX_DA10_ACCT_BAL_AFTER_CALL = -1; // 39
  private final static int IDX_CHARGE_DA11 = -1; // 40
  private final static int IDX_DA11_ACCT_BAL_BEFORE_CALL = -1; // 41
  private final static int IDX_DA11_ACCT_BAL_AFTER_CALL = -1; // 42
  private final static int IDX_CHARGE_DA12 = -1; // 43
  private final static int IDX_DA12_ACCT_BAL_BEFORE_CALL = -1; // 44
  private final static int IDX_DA12_ACCT_BAL_AFTER_CALL = -1; // 45
  private final static int IDX_CHARGE_DA13 = -1; // 46
  private final static int IDX_DA13_ACCT_BAL_BEFORE_CALL = -1; // 47
  private final static int IDX_DA13_ACCT_BAL_AFTER_CALL = -1; // 48
  private final static int IDX_CHARGE_DA14 = -1; // 49
  private final static int IDX_DA14_ACCT_BAL_BEFORE_CALL = -1; // 50
  private final static int IDX_DA14_ACCT_BAL_AFTER_CALL = -1; // 51
  private final static int IDX_CHARGE_DA15 = -1; // 52
  private final static int IDX_DA15_ACCT_BAL_BEFORE_CALL = -1; // 53
  private final static int IDX_DA15_ACCT_BAL_AFTER_CALL = -1; // 54
  private final static int IDX_CALL_TYPE = 6; // 55
  private final static int IDX_PARTNER_OPTR = 7; // 56
  private final static int IDX_FNF_IND = 8; // 57
  private final static int IDX_SERVICE_CLASS = 9; // 58
  private final static int IDX_TELESERVICE_CODE = 10; // 59
  private final static int IDX_TRAFFIC_TYPE = 11; // 60
  private final static int IDX_CFW_IND = -1; // 61
  private final static int IDX_ORIGINATING_LOC_INFO = -1; // 62
  private final static int IDX_ACCUMULATOR_ID = -1; // 63
  private final static int IDX_ACCUMULATOR_VALUE_BEFORE = -1; // 64
  private final static int IDX_ACCUMULATOR_VALUE_AFTER = -1; // 65
  private final static int IDX_CELL_SITE_ID = -1; // 66
  private final static int IDX_DDS = -1; // 67
  private final static int IDX_SERVICE_OFFERINGS = -1; // 68
  private final static int IDX_PRODUCT_ID = -1; // 69
  private final static int IDX_COMMUNITY_IND = -1; // 70
  private final static int IDX_CHARGED_PARTY_NUMBER = -1; // 71
  private final static int IDX_GSM_CALL_REF = -1; // 72
  private final static int IDX_FILE_ID = -1; // 73
  private final static int IDX_SESSIONID = -1; // 74
  private final static int IDX_CLIENTIP = -1; // 75
  private final static int IDX_FIELD1 = -1; // 76
  private final static int IDX_FIELD2 = -1; // 77
  private final static int IDX_FIELD3 = -1; // 78
  private final static int IDX_FIELD4 = -1; // 79
  private final static int IDX_FIELD5 = -1; // 80

  //  The record type is what allows us to determine what the records to handle
  //	are, and what to ignore. Generally you will need something of this type
  public static final String RECYCLE_TAG = "ORRECYCLE";
  private static final long serialVersionUID = 1L;

  // CDR related variables
  public String eventDate = null; // Date of the call
  public int callDuration;     // Duration of the call
  public String ANumber;         // Raw A Number
  public String BNumber;         // Raw B Number
  public String BNumberNorm;     // Normalised B number

  // Rating variables
  public String destination;      // The zoning destination for the B Number
  public String destCategory;     // The category for the B Number
  public String timeZone;         // The time zone

  // Output rated amount values
  public double origAmount = 0;
  public double outputTotalCost = 0;
  public double outputConnCost = 0;
  public double ratedAmount = 0;

  // Internal Management Fields
  public Integer custIDA = null;    // The identifier of the A customer
  public String usedProduct;       // The identifier of the product
  public String baseProduct;       // The base price plan
  public ArrayList<String> overlay; // Overlay price plan(s)
  public String marginFlag;        // Used for reporting incorrect margins

  // The number of recycles for this record
  public int recycleCount = 0;

  // Unique ID for this record
  public String recordId;

  // Call scenario fields
  public String callType;
  public String partnerOperator;
  public boolean fnf = false;
  public String serviceClass;
  public TeleserviceCode teleserviceCode;
  public String trafficType;

  /**
   * Utility function to map a file header record
   *
   * @param inputData The input data to map
   */
  public void mapVenteloHeaderRecord(String inputData) {
    RECORD_TYPE = PixipRecord.FILE_HEADER_RECORD;
    OriginalData = inputData;
  }

  /**
   * Utility function to map a file trailer record
   *
   * @param inputData The input data to map
   */
  public void mapVenteloTrailerRecord(String inputData) {
    RECORD_TYPE = PixipRecord.FILE_TRAILER_RECORD;
    OriginalData = inputData;
  }

  /**
   * Map a detail record from the Ventelo input source. We split up the record
   * at the tabs, and put the information into fieldsso that we can manipulate
   * it as we want.
   *
   * @param inputData The input data to map
   */
  public void mapFileDetailRecord(String inputData) {
    // Set the record type
    RECORD_TYPE = PixipRecord.FILE_DETAIL_RECORD;

    // Set the original data
    OriginalData = inputData;

    // Detect recycle case
    if (OriginalData.startsWith(RECYCLE_TAG)) {
      // RECYCLE_COUNT
      StringBuffer record = new StringBuffer(OriginalData);

      // remove RecycleTag from record
      record = record.delete(0, record.indexOf(FILE_FIELD_SPLITTER) + 1);

      // remove ErrorCode from record
      record = record.delete(0, record.indexOf(FILE_FIELD_SPLITTER) + 1);

      // Get the previous recycle count
      String Recycle_CountStr = record.substring(0, record.indexOf(FILE_FIELD_SPLITTER));
      recycleCount = Integer.parseInt(Recycle_CountStr);

      // remove RecycleCount from record
      record = record.delete(0, record.indexOf(";") + 1);

      // reset the original data
      OriginalData = record.toString();
    }

    // Split the fields up
    fields = OriginalData.split(FILE_FIELD_SPLITTER);

    // Validate the number of fields
    if (fields.length == FIELD_V_COUNT) {
      eventDate = getField(IDX_CALL_DATE);
      ANumber = getField(IDX_ANUMBER);
      BNumber = getField(IDX_BNUMBER);

      try {
        callDuration = Integer.parseInt(getField(IDX_CALL_DURATION));
      } catch (NumberFormatException nfe) {
        addError(new RecordError("ERR_DURATION_INVALID", ErrorType.DATA_VALIDATION));
      }

      try {
        origAmount = Double.parseDouble(getField(IDX_CHARGE_MAIN_ACCT));
      } catch (NumberFormatException ex) {
        addError(new RecordError("ERR_ORIG_PRICE_INVALID", ErrorType.DATA_VALIDATION));
      }

      //Repair full prefix for National calls
      if (BNumber.length() > 5 && !BNumber.substring(1, 2).matches("0") && BNumber.substring(0, 1).matches("0")) {
        BNumberNorm = "0046" + BNumber.substring(1, BNumber.length());
      } //Repair 118118 and others
      else if (BNumber.length() < 7 && !BNumber.substring(1, 2).matches("0") && BNumber.substring(0, 2).matches("11")) {
        BNumberNorm = "0046" + BNumber;
      } // International
      else if (BNumber.startsWith("00")) {
        // Do nothing it is already right
        BNumberNorm = BNumber;
      } // Default error case
      else {
        addError(new RecordError("ERR_NORM_FAILED", ErrorType.DATA_VALIDATION));
      }

      // Get the CDR date
      try {
        SimpleDateFormat sdfInput = new SimpleDateFormat("yyyyMMddHHmmss");
        EventStartDate = sdfInput.parse(eventDate);
        UTCEventDate = EventStartDate.getTime() / 1000;
      } catch (ParseException ex) {
        addError(new RecordError("ERR_DATE_INVALID", ErrorType.DATA_VALIDATION));
      }

      // Set the RUMS duration and original rated amount (for markup)
      setRUMValue("DUR", callDuration);
      setRUMValue("SEK", origAmount);

      // Set the default service
      Service = "TEL";
    } else {
      addError(new RecordError("ERR_FIELD_COUNT", ErrorType.DATA_VALIDATION));
    }
  }

  /**
   * Utility function to map an unknown type of record
   *
   * @param inputData The input data to map
   */
  public void mapUnknownRecord(String inputData) {
    // Just mark the record as unknown and store the input
    OriginalData = inputData;
    addError(new RecordError("ERR_UNKNOWN_RECORD", ErrorType.DATA_VALIDATION));
  }

  /**
   * Reconstruct the record from the field values, replacing the original
   * structure of tab separated records
   *
   * @return The unmapped original data
   */
  public String unmapOriginalData() {
    int NumberOfFields;
    int i;
    StringBuffer tmpReassemble;

    if (RECORD_TYPE == PixipRecord.FILE_DETAIL_RECORD) {
      // We use the string buffer for the reassembly of the record. Avoid
      // just catenating strings, as it is a LOT slower because of the
      // java internal string handling (it has to allocate/deallocate many
      // times to rebuild the string).
      tmpReassemble = new StringBuffer(1024);

      // write the destination information back
      // setField(DESTINATION_IDX, destination);
      NumberOfFields = fields.length;

      for (i = 0; i < NumberOfFields; i++) {

        if (i == 0) {
          tmpReassemble.append(fields[i]);
        } else {
          tmpReassemble.append(FILE_FIELD_SPLITTER);
          tmpReassemble.append(fields[i]);
        }
      }

      return tmpReassemble.toString();
    } else {
      // just return the untampered with original
      return OriginalData;
    }
  }

  /**
   * Reconstruct the record from the field values, replacing the original
   * structure of tab separated records
   *
   * @return The unmapped original data
   */
  public String unmapVenteloSuspenseData() {
    StringBuffer tmpReassemble;

    if (RECORD_TYPE == PixipRecord.FILE_DETAIL_RECORD) {
      // We use the string buffer for the reassembly of the record. Avoid
      // just catenating strings, as it is a LOT slower because of the
      // java internal string handling (it has to allocate/deallocate many
      // times to rebuild the string).
      tmpReassemble = new StringBuffer(1024);

      // Write the error information back, including the recycle header
      String errorCode = this.getErrors().get(0).getMessage();

      // Increment the recycle count
      recycleCount++;

      // Put the header on
      tmpReassemble.append(RECYCLE_TAG);
      tmpReassemble.append(FILE_FIELD_SPLITTER);
      tmpReassemble.append(errorCode);
      tmpReassemble.append(FILE_FIELD_SPLITTER);
      tmpReassemble.append(recycleCount);
      tmpReassemble.append(FILE_FIELD_SPLITTER);

      // Now the original record
      tmpReassemble.append(OriginalData);

      return tmpReassemble.toString();
    } else {
      // just return the untampered with original
      return OriginalData;
    }
  }

  /**
   * Return the dump-ready data
   *
   * @return The dump info strings
   */
  @Override
  public ArrayList<String> getDumpInfo() {

    ArrayList<String> tmpDumpList;
    tmpDumpList = new ArrayList<>();

    // Format the fields
    // We only transform the detail records, and leave the others alone
    if (RECORD_TYPE == PixipRecord.FILE_DETAIL_RECORD) {
      tmpDumpList.add("============ BEGIN RECORD ============");
      tmpDumpList.add("  Record Number         = <" + RecordNumber + ">");
      tmpDumpList.add("  Outputs               = <" + outputs + ">");
      tmpDumpList.add("--------------------------------------");
      tmpDumpList.add("  Call Date             = <" + eventDate + ">");
      tmpDumpList.add("  Call Duration         = <" + callDuration + ">");
      tmpDumpList.add("  ANumber               = <" + ANumber + ">");
      tmpDumpList.add("  BNumber               = <" + BNumber + ">");
      tmpDumpList.add("  OrigRatedAmount       = <" + origAmount + ">");
      tmpDumpList.add("  CallType              = <" + callType + ">");
      tmpDumpList.add("  PartnerOperator       = <" + partnerOperator + ">");
      tmpDumpList.add("  FnF                   = <" + fnf + ">");
      tmpDumpList.add("  ServiceClass          = <" + serviceClass + ">");
      tmpDumpList.add("  TeleServiceCode       = <" + teleserviceCode + ">");
      tmpDumpList.add("  TrafficType           = <" + trafficType + ">");
      tmpDumpList.add("--------------------------------------");
      tmpDumpList.add("  BNumberNormalised     = <" + BNumberNorm + ">");
      tmpDumpList.add("  ORRatedAmount         = <" + ratedAmount + ">");
      tmpDumpList.add("--------------------------------------");
      tmpDumpList.add("  CustIDA               = <" + custIDA + ">");
      tmpDumpList.add("  CDRDate               = <" + EventStartDate + ">");
      tmpDumpList.add("  BNumber Norm          = <" + BNumberNorm + ">");
      tmpDumpList.add("  Destination           = <" + destination + ">");
      tmpDumpList.add("  Destination Category  = <" + destCategory + ">");
      tmpDumpList.add("  TimeZone              = <" + timeZone + ">");
      tmpDumpList.add("--------------------------------------");
      tmpDumpList.add("  UsedProduct           = <" + usedProduct + ">");
      tmpDumpList.add("  Base Product          = <" + baseProduct + ">");
      tmpDumpList.add("  Overlay Product       = <" + overlay + ">");
      tmpDumpList.add("  RatedAmount           = <" + outputTotalCost + ">");
      tmpDumpList.add("  RatedAmountConnFee    = <" + outputConnCost + ">");

      // Charge Packets
      tmpDumpList.addAll(getChargePacketsDump());

      // Errors
      tmpDumpList.addAll(getErrorDump());
    }

    return tmpDumpList;
  }

  public Object getSourceKey() {
    return null;
  }

  /**
   * Map a record from the DB input adapter into the internal format.
   *
   * @param originalColumns The columns we got from the DB
   */
  void mapDBDetailRecord(String[] originalColumns) {
    // Set the record type
    RECORD_TYPE = PixipRecord.FILE_DETAIL_RECORD;

    recordId = originalColumns[IDX_mtn_cdr_id];
    ANumber = originalColumns[IDX_ANUMBER];
    BNumber = originalColumns[IDX_BNUMBER];
    eventDate = originalColumns[IDX_CALL_DATE];

    try {
      // Convert date from string: 2014-12-02 16:42:21
      SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      EventStartDate = sdfInput.parse(eventDate);
      UTCEventDate = EventStartDate.getTime() / 1000;
    } catch (ParseException ex) {
      addError(new RecordError("ERR_DATE_INVALID", ErrorType.DATA_VALIDATION));
    }

    try {
      callDuration = Integer.parseInt(originalColumns[IDX_CALL_DURATION]);
    } catch (NumberFormatException ex) {
      addError(new RecordError("ERR_DURATION_INVALID", ErrorType.DATA_VALIDATION));
    }

    try {
      origAmount = Double.parseDouble(originalColumns[IDX_CHARGE_MAIN_ACCT]);
    } catch (NumberFormatException ex) {
      addError(new RecordError("ERR_ORIG_PRICE_INVALID", ErrorType.DATA_VALIDATION));
    }

    // Call scenario fields
    callType = originalColumns[IDX_CALL_TYPE];
    partnerOperator = originalColumns[IDX_PARTNER_OPTR];

    String tmpFnf = originalColumns[IDX_FNF_IND];
    if (tmpFnf != null && tmpFnf.equals("1")) {
      fnf = true;
    }

    serviceClass = originalColumns[IDX_SERVICE_CLASS];
    
    // Handle teleservice code
    // TODO: Find out what null TeleserviceCode means
    try {
      teleserviceCode = TeleserviceCode.fromValue(originalColumns[IDX_TELESERVICE_CODE]);
    } catch (NullPointerException ex) {
      teleserviceCode = UNKNOWN;
    } catch (IllegalArgumentException ex) {
      addError(new RecordError("ERR_TS_CODE_INVALID", ErrorType.DATA_VALIDATION));
    }

    trafficType = originalColumns[IDX_TRAFFIC_TYPE];

    // Set the RUMS duration and original rated amount (for markup)
    setRUMValue("DUR", callDuration);
    setRUMValue("MONEY", origAmount);

    // Set the default service
    Service = "TEL";
  }
}
