package Pixip;

import OpenRate.process.AbstractStubPlugIn;
import OpenRate.record.IRecord;
import OpenRate.utils.ConversionUtils;

/**
 * This module removes VAT content from PostPaid CDRs.
 *
 *
 *
 * @author ian
 */
public class PostPaidVATDropDown extends AbstractStubPlugIn {

  @Override
  public IRecord procValidRecord(IRecord r) {

    PixipRecord CurrentRecord = (PixipRecord) r;

    // We only transform the detail records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {

      switch (CurrentRecord.teleserviceCode) {
        case VOICE:
        case SMS:
        case GPRS:
          // VAT dropdown for Post paid plans
          if (CurrentRecord.usedProduct.matches("CONNECTA.*")) {
            CurrentRecord.ratedAmount = dropDownVAT(CurrentRecord.ratedAmount);
          } else if (CurrentRecord.usedProduct.matches("DISTRIBUTOR.*")) {
            CurrentRecord.ratedAmount = dropDownVAT(CurrentRecord.ratedAmount);
          }
          break;
      }
    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r) {
    // do nothing
    return r;
  }

  /**
   * Drop down VAT for the given input amount.
   * 
   * @param ratedAmount
   * @return 
   */
  public double dropDownVAT(double ratedAmount) {
    double tmpAmount = ratedAmount * 100 / 114;

    // perform rounding, currently fixed VAT and use ConfCode to decide on fixed discount
    return ConversionUtils.getConversionUtilsObject().getRoundedValue(tmpAmount, 2);
  }
}
