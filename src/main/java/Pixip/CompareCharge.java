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
