/*
 * Copyright 2006-2026 Ian Sparkes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * utcEventDate which you should set, and on the charge packet. In the case that
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
                + " eventStartDate=" + CurrentRecord.eventStartDate, ErrorType.DATA_NOT_FOUND, this.getSymbolicName());
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
