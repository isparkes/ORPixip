package Pixip;

import OpenRate.process.AbstractStubPlugIn;
import OpenRate.record.ErrorType;
import OpenRate.record.IRecord;
import OpenRate.record.RecordError;
import OpenRate.utils.ConversionUtils;

/**
 * Compare the charge we calculated with the charge we got.
 *
 * @author ian
 */
public class CompareCharge extends AbstractStubPlugIn {

  @Override
  public IRecord procValidRecord(IRecord r) {

    PixipRecord CurrentRecord = (PixipRecord) r;

    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      double delta = Math.abs(CurrentRecord.ratedAmount - CurrentRecord.compareAmount);
      double comparisonValueRounded = ConversionUtils.getConversionUtilsObject().getRoundedValue(delta, 2);

      switch (CurrentRecord.teleserviceCode) {
        case VOICE:
          if (comparisonValueRounded > 0.08) {
            CurrentRecord.addError(new RecordError("ERR_COMPARISON_FAIL", ErrorType.DATA_VALIDATION));
          }
          break;
        default:
          if (comparisonValueRounded > 0.05) {
            CurrentRecord.addError(new RecordError("ERR_COMPARISON_FAIL", ErrorType.DATA_VALIDATION));
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
}
