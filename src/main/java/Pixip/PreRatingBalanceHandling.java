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
