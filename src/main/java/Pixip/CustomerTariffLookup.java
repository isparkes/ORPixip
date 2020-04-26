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
