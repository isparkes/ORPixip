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

import OpenRate.process.AbstractStubPlugIn;
import OpenRate.record.ChargePacket;
import OpenRate.record.IRecord;
import OpenRate.record.RatingBreakdown;
import OpenRate.utils.ConversionUtils;
import java.util.Iterator;

/**
 * This module performs rounding and collection of output values.
 *
 * @author ian
 */
public class RateRounding extends AbstractStubPlugIn {
  // -----------------------------------------------------------------------------
  // ------------------ Start of inherited Plug In functions ---------------------
  // -----------------------------------------------------------------------------

  /**
   * For all good records, this module enriches the CDR record with the
   * information from the Accounting Info and Time Info records.
   *
   * @param r The record we are working on
   * @return The processed record
   */
  @Override
  public IRecord procValidRecord(IRecord r) {
    double defaultTotalCost = 0;
    double overlayTotalCost = 0;
    boolean overlayUsed = false;

    PixipRecord CurrentRecord = (PixipRecord) r;

    // We only transform the detail records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      // No custom rate so use the standard one
      // pick out the connect cost part and create the steps serialisation
      Iterator<ChargePacket> cpIter = CurrentRecord.getChargePackets().iterator();
      while (cpIter.hasNext()) {
        ChargePacket tmpCP = cpIter.next();

        // Gather from valid CPs with rating info
        if ((tmpCP.breakDown != null) && (tmpCP.Valid)) {
          // Standard Rating
          if (tmpCP.packetType.equals("R")) {
            Iterator<RatingBreakdown> rbIter = tmpCP.breakDown.iterator();
            while (rbIter.hasNext()) {
              RatingBreakdown rb = rbIter.next();

              // Gather the steps
              defaultTotalCost += rb.ratedAmount;
            }
          }

          // overlay rating
          if (tmpCP.packetType.equals("O")) {
            // mark that we should use the overlay price
            overlayUsed = true;

            Iterator<RatingBreakdown> rbIter = tmpCP.breakDown.iterator();
            while (rbIter.hasNext()) {
              RatingBreakdown rb = rbIter.next();

              // Gather the steps and total cost
              overlayTotalCost += rb.ratedAmount;
            }
          }
        }
      }

      // now pick the right one
      if (overlayUsed) {
        CurrentRecord.ratedAmount = overlayTotalCost;
      } else {
        CurrentRecord.ratedAmount = defaultTotalCost;
      }

      // perform rounding, currently fixed VAT and use ConfCode to decide on fixed discount
      double tmpAmount;
      tmpAmount = CurrentRecord.ratedAmount;
      CurrentRecord.ratedAmount = ConversionUtils.getConversionUtilsObject().getRoundedValue(tmpAmount, 4);
    }

    return r;
  }

  /**
   * This is called when a data record with errors is encountered. You should do
   * any processing here that you have to do for error records, e.g. statistics,
   * special handling, even error correction!
   *
   * @param r The record we are working on
   * @return The processed record
   */
  @Override
  public IRecord procErrorRecord(IRecord r) {
    // do nothing
    return r;
  }
}
