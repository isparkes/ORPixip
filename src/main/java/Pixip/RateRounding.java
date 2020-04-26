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
      switch (CurrentRecord.teleserviceCode) {
        case VOICE:
        case SMS:
          tmpAmount = CurrentRecord.ratedAmount;
          CurrentRecord.ratedAmount = ConversionUtils.getConversionUtilsObject().getRoundedValue(tmpAmount, 2);
          break;
        case GPRS:
          // Data is rounded up
          tmpAmount = CurrentRecord.ratedAmount;
          CurrentRecord.ratedAmount = ConversionUtils.getConversionUtilsObject().getRoundedValueRoundUp(tmpAmount, 2);
          
          // Minimum charge rule
          if (CurrentRecord.ratedAmount < 0.01) {
            CurrentRecord.ratedAmount = 0.01;
          }
          break;
      }
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
