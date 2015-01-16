/* ====================================================================
 * Limited Evaluation License:
 *
 * The exclusive owner of this work is Tiger Shore Management Ltd.
 * This work, including all associated documents and components
 * is Copyright Tiger Shore Management Limited 2006-2012.
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
 * and fitness for a particular purpose are discplaimed. In no event shall
 * Tiger Shore Management or its officially assigned agents be liable to any
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

import OpenRate.adapter.jdbc.JDBCOutputAdapter;
import OpenRate.record.ChargePacket;
import OpenRate.record.DBRecord;
import OpenRate.record.IRecord;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author afzaal
 */
public class PixipDBOutputAdapter extends JDBCOutputAdapter {

  /**
   * Creates a new instance of DBOutput
   */
  public PixipDBOutputAdapter() {
    super();
  }

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
  public Collection<IRecord> procValidRecord(IRecord r) {

    PixipRecord tmpInRecord;
    DBRecord tmpDataRecord;
    Collection<IRecord> Outbatch;
    Outbatch = new ArrayList<>();
    tmpInRecord = (PixipRecord) r;
    tmpDataRecord = new DBRecord();
    tmpDataRecord.setOutputColumnCount(3);

    // Rate information, add all Charge Packets Info in one string seperated by #
    StringBuilder rateInfo = new StringBuilder("");
    ChargePacket cp;
    for (int i = 0; i < tmpInRecord.getChargePacketCount(); i++) {
      cp = tmpInRecord.getChargePacket(i);
      rateInfo.append(cp.ratePlanName).append(",").append(cp.service).append(",").append(cp.timeResult).append(",").append(cp.priceGroup).append("#");
    }
    tmpDataRecord.setOutputColumnString(0, rateInfo.toString());

    // Price
    tmpDataRecord.setOutputColumnDouble(1, tmpInRecord.ratedAmount);

    // RadacctID (Primary key)
    tmpDataRecord.setOutputColumnString(2, tmpInRecord.recordID);
    Outbatch.add((IRecord) tmpDataRecord);

    return Outbatch;
  }

  /**
   * Handle any error records here so that they are ready to output making any
   * specific changes to the record that are necessary to make it ready for
   * output.
   */
  @Override
  public Collection<IRecord> procErrorRecord(IRecord r) {
    PixipRecord tmpInRecord;
    DBRecord tmpDataRecord;
    Collection<IRecord> Outbatch;
    Outbatch = new ArrayList<>();
    tmpInRecord = (PixipRecord) r;
    tmpDataRecord = new DBRecord();
    tmpDataRecord.setOutputColumnCount(3);

    // Rate
    tmpDataRecord.setOutputColumnString(0, tmpInRecord.getErrors().get(0).getMessage());

    // Price
    tmpDataRecord.setOutputColumnDouble(1, new Double("0"));

    // RadacctID (Primary key)
    tmpDataRecord.setOutputColumnString(2, tmpInRecord.recordID);
    Outbatch.add((IRecord) tmpDataRecord);

    return Outbatch;
  }
}
