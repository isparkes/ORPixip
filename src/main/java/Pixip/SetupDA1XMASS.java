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
 * set up the DA field so that the normal processing can use it. Only for 
 * XMASS tables.
 *
 * @author ian
 */
public class SetupDA1XMASS extends AbstractStubPlugIn {

  @Override
  public IRecord procValidRecord(IRecord r) {

    PixipRecord CurrentRecord = (PixipRecord) r;

    // We only transform the detail records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {

      if ((CurrentRecord.beforeDA1 - CurrentRecord.afterDA1) != 0) {
        CurrentRecord.chargeDA1 = CurrentRecord.expectedDA1;
      }
    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r
  ) {
    // do nothing
    return r;
  }
}
