package Pixip;

import OpenRate.adapter.jdbc.JDBCOutputAdapter;
import OpenRate.record.ChargePacket;
import OpenRate.record.DBRecord;
import OpenRate.record.IRecord;
import OpenRate.record.TimePacket;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author afzaal
 */
public class PixipDBOutputAdapter extends JDBCOutputAdapter {

  /**
   * We transform the records here so that they are ready to output making any
   * specific changes to the record that are necessary to make it ready for
   * output.
   *
   * As we are using the FlatFileOutput adapter, we should transform the records
   * into FlatRecords, storing the data to be written using the SetData()
   * method. This means that we do not have to know about the internal workings
   * of the output adapter.
   *
   * Note that this is just undoing the transformation that we did in the input
   * adapter.
   *
   * @return
   */
  @Override
  public Collection<DBRecord> procValidRecord(IRecord r) {

    PixipRecord tmpInRecord;
    DBRecord tmpDataRecord;
    Collection<DBRecord> Outbatch;
    Outbatch = new ArrayList<>();
    tmpInRecord = (PixipRecord) r;
    tmpDataRecord = new DBRecord();
    tmpDataRecord.setOutputColumnCount(3);

    // Rate information, add all Charge Packets Info in one string seperated by #
    StringBuilder rateInfo = new StringBuilder("");
    for (ChargePacket cp : tmpInRecord.getChargePackets()) {
      rateInfo.append(cp.ratePlanName).append(",").append(cp.zoneResult).append(",");
      for (TimePacket tp : cp.getTimeZones()) {
        rateInfo.append(tp.timeResult).append(",").append(tp.priceGroup).append(",");
      }
    }
    rateInfo.append("#");

    // ********************* for output without upsert *************************
//    // Price
//    tmpDataRecord.setOutputColumnDouble(0, tmpInRecord.ratedAmount);
//    
//    // Message
//    tmpDataRecord.setOutputColumnString(1, rateInfo.toString());
//
//    // RadacctID (Primary key)
//    tmpDataRecord.setOutputColumnString(2, tmpInRecord.recordId);
    // ********************** for output with upsert ***************************
    // RadacctID (Primary key)
    tmpDataRecord.setOutputColumnString(0, tmpInRecord.recordId);

    // Price
    tmpDataRecord.setOutputColumnDouble(1, tmpInRecord.ratedAmount);

    // Message
    tmpDataRecord.setOutputColumnString(2, rateInfo.toString());

    // *************************************************************************
    Outbatch.add(tmpDataRecord);

    return Outbatch;
  }

  /**
   * Handle any error records here so that they are ready to output making any
   * specific changes to the record that are necessary to make it ready for
   * output.
   *
   * @return
   */
  @Override
  public Collection<DBRecord> procErrorRecord(IRecord r) {
    PixipRecord tmpInRecord;
    DBRecord tmpDataRecord;
    Collection<DBRecord> Outbatch;
    Outbatch = new ArrayList<>();
    tmpInRecord = (PixipRecord) r;
    tmpDataRecord = new DBRecord();
    tmpDataRecord.setOutputColumnCount(3);

    StringBuilder rateInfo = new StringBuilder("");
    if (tmpInRecord.getErrors().get(0).getMessage().equals("ERR_COMPARISON_FAIL")) {
      // Rate information, add all Charge Packets Info in one string seperated by #
      for (ChargePacket cp : tmpInRecord.getChargePackets()) {
        rateInfo.append(cp.ratePlanName).append(",").append(cp.zoneResult).append(",");
        for (TimePacket tp : cp.getTimeZones()) {
          rateInfo.append(tp.timeResult).append(",").append(tp.priceGroup).append(",");
        }
      }
      rateInfo.append("#DIFF");
    } else {
      rateInfo.append(tmpInRecord.getErrors().get(0).getMessage());
    }

    // ********************* for output without upsert *************************
//    // Price
//    tmpDataRecord.setOutputColumnDouble(0, tmpInRecord.ratedAmount);
//
//    // Error Description
//    tmpDataRecord.setOutputColumnString(1, tmpInRecord.getErrors().get(0).getMessage());
//
//    // Record ID (Primary key)
//    tmpDataRecord.setOutputColumnString(2, tmpInRecord.recordId);
    // ********************** for output with upsert ***************************
    // RadacctID (Primary key)
    tmpDataRecord.setOutputColumnString(0, tmpInRecord.recordId);

    // Price
    tmpDataRecord.setOutputColumnDouble(1, tmpInRecord.ratedAmount);

    // Message
    tmpDataRecord.setOutputColumnString(2, rateInfo.toString());

    // *************************************************************************
    Outbatch.add(tmpDataRecord);

    return Outbatch;
  }
}
