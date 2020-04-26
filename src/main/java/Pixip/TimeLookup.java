package Pixip;

import OpenRate.exception.ProcessingException;
import OpenRate.process.AbstractRUMTimeMatch;
import OpenRate.record.ErrorType;
import OpenRate.record.IRecord;
import OpenRate.record.RecordError;
import OpenRate.utils.ConversionUtils;

/**
 * This module looks up the time of day the call was made in. This allows us to
 * determine peak and off-peak calls. The lookup happens directly from the
 * UTCEventDate which you should set, and on the charge packet. In the case that
 * time splitting is needed (different rating based on a call crossing into a
 * separate time zone), the charge packet will be duplicated as necessary.
 */
public class TimeLookup extends AbstractRUMTimeMatch {

  private final ConversionUtils conversionUtils = ConversionUtils.getConversionUtilsObject();

  @Override
  public IRecord procValidRecord(IRecord r) {
    RecordError tmpError;
    PixipRecord CurrentRecord = (PixipRecord) r;

    // We only transform the basic records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      
      // Populate the end date - needed for time splitting
      CurrentRecord.eventEndDate = conversionUtils.addDateSeconds(CurrentRecord.eventStartDate, CurrentRecord.callDuration);

      try {
        performRUMTimeMatch(CurrentRecord);
      } catch (ProcessingException pe) {
        tmpError = new RecordError("TimeZone Value Not Found for TimeModel=" + CurrentRecord.getChargePacket(0).timeModel
                + " EventStartDate=" + CurrentRecord.eventStartDate, ErrorType.DATA_NOT_FOUND, this.getSymbolicName());
        CurrentRecord.addError(tmpError);
      }
    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r) {
    return r;
  }
}
