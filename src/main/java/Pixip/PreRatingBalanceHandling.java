package Pixip;

import OpenRate.process.AbstractStubPlugIn;
import OpenRate.record.IRecord;

/**
 * Handle balances before rating.
 *
 * @author ian
 */
public class PreRatingBalanceHandling extends AbstractStubPlugIn {

  @Override
  public IRecord procValidRecord(IRecord r) {

    PixipRecord CurrentRecord = (PixipRecord) r;

    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      if (CurrentRecord.chargeDA1 == 70) {
        double tmpCompareAmount = (CurrentRecord.beforeDA1 - CurrentRecord.afterDA1) * -60;

        // HACK: Reduce the RUm to rate by this amount
        CurrentRecord.updateRUMValue("DUR", tmpCompareAmount);
      }
    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r) {
    // do nothing
    return r;
  }
}
