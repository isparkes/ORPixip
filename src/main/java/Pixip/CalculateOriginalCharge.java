package Pixip;

import OpenRate.process.AbstractStubPlugIn;
import OpenRate.record.IRecord;
import OpenRate.utils.ConversionUtils;
import static Pixip.model.TeleserviceCode.VOICE;

/**
 * Calculate what the original charge would have been, taking into account any
 * bundles.
 *
 * @author ian
 */
public class CalculateOriginalCharge extends AbstractStubPlugIn {

  @Override
  public IRecord procValidRecord(IRecord r) {

    PixipRecord CurrentRecord = (PixipRecord) r;

    // We only transform the detail records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      double tmpCompareAmount = CurrentRecord.origAmount;

      switch (CurrentRecord.teleserviceCode) {
        case VOICE:
          if (CurrentRecord.chargeDA1 > 0) {
            if (CurrentRecord.chargeDA1 == 70) {
              // 70 balances are applied before rating, we do not need to apply again after
            } else {
              tmpCompareAmount += (CurrentRecord.beforeDA1 - CurrentRecord.afterDA1);
            }
          }
          break;
        case SMS:
          if (CurrentRecord.chargeDA1 > 0) {
            tmpCompareAmount += (CurrentRecord.beforeDA1 - CurrentRecord.afterDA1);
          }
          break;
        case GPRS:
          if (CurrentRecord.chargeDA1 > 0) {
            tmpCompareAmount += (CurrentRecord.beforeDA1 - CurrentRecord.afterDA1);
          }
          break;
      }

      // Tariff based bonus/markup
      if (CurrentRecord.usedProduct.equals("Pay As You Go Dynamic PSB")
              && (CurrentRecord.teleserviceCode == VOICE)) {
        tmpCompareAmount *= 1.05;
      }

      CurrentRecord.compareAmount = ConversionUtils.getConversionUtilsObject().getRoundedValue(tmpCompareAmount, 2);

    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r) {
    // do nothing
    return r;
  }
}
