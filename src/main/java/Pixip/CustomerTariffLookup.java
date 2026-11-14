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
 * Lookup the customer tariff from the MSISDN.
 *
 * @author ian
 */
public class CustomerTariffLookup
        extends AbstractRegexMatch {

  // this is used for the lookup
  String[] tmpSearchParameters = new String[1];

  @Override
  public IRecord procValidRecord(IRecord r) {
    String RegexGroup;
    PixipRecord CurrentRecord;
    ArrayList<String> result;

    CurrentRecord = (PixipRecord) r;

    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      // ********************* B Number Normalisation *********************
      // Prepare the paramters to perform the search on
      tmpSearchParameters[0] = CurrentRecord.ANumber;

      RegexGroup = "Default";

      result = getRegexMatchWithChildData(RegexGroup, tmpSearchParameters);

      if (isValidRegexMatchResult(result)) {
        CurrentRecord.usedProduct = result.get(0);
        CurrentRecord.expectedDA1 = Integer.parseInt(result.get(1));
      } else {
        RecordError tmpError = new RecordError("ERR_CUST_TARIFF_NOT_FOUND", ErrorType.SPECIAL);
        tmpError.setModuleName(getSymbolicName());
        tmpError.setErrorDescription(CurrentRecord.ANumber);
        CurrentRecord.addError(tmpError);
        return r;
      }
    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r) {
    return r;
  }
}
