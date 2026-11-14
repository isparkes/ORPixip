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

import OpenRate.process.AbstractRegexMatch;
import OpenRate.record.ErrorType;
import OpenRate.record.IRecord;
import OpenRate.record.RecordError;
import java.util.ArrayList;

/**
 * This module looks up the hierarchy of the price plan, and prepares a list of
 * the price plans to evaluate. In general this will be
 *
 * n Overlay Plans (Prio >= 1) 1 Base Plan (Prio 0)
 */
public class PriceHierarchyLookup extends AbstractRegexMatch {

  // used to perform the lookup - defined here for performance reasons
  String[] searchParams = new String[1];

  // -----------------------------------------------------------------------------
  // ------------------ Start of inherited Plug In functions ---------------------
  // -----------------------------------------------------------------------------
  /**
   * This is called when a data record is encountered. You should do any normal
   * processing here.
   *
   * @return
   */
  @Override
  public IRecord procValidRecord(IRecord r) {
    PixipRecord CurrentRecord = (PixipRecord) r;

    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      // check if this is one of the markup scenarios
      searchParams[0] = CurrentRecord.usedProduct;
      String pricePlanList = getRegexMatch("Default", searchParams);

      // If we got a match, process it
      if (isValidRegexMatchResult(pricePlanList)) {
        // split the plans
        String[] planList = pricePlanList.split(":");

        // Check that we got something useful
        if (planList.length == 0) {
          CurrentRecord.addError(new RecordError("ERR_INVALID_PLAN_LIST", ErrorType.DATA_NOT_FOUND));
        } else {
          // Create the overlay list
          CurrentRecord.overlay = new ArrayList<>();

          // Set the option list
          for (int i = 0; i < planList.length - 1; i++) {
            CurrentRecord.overlay.add(planList[i]);
          }

          // Set the base plan
          CurrentRecord.baseProduct = planList[planList.length - 1];
        }
      } else {
        CurrentRecord.addError(new RecordError("ERR_PLAN_LIST_NOT_DEFINED", ErrorType.DATA_NOT_FOUND));
      }
    }

    return r;
  }

  /**
   * This is called when a data record with errors is encountered. You should do
   * any processing here that you have to do for error records, e.g.
   * statistics, special handling, even error correction!
   *
   * @return
   */
  @Override
  public IRecord procErrorRecord(IRecord r) {
    //transform((TyfonRecord)r);

    return r;
  }
}
