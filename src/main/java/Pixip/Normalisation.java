package Pixip;

import OpenRate.process.AbstractRegexMatch;
import OpenRate.record.ErrorType;
import OpenRate.record.IRecord;
import OpenRate.record.RecordError;
import java.util.ArrayList;

/**
 * This module performs rule driven normalisation on the B numbers, producing a
 * number which can be used in the zoning. The number is fully normalised,
 * giving a result with IAC+CC+NDC. The IAC here is "00".
 *
 * We run down the list of rules in the table NORM_MAP in the order defined by
 * the RANK column. Rules of the same RANK may be executed in any order, but
 * this is usually the natural order of the database.
 *
 * The first rule that fires stops the evaluation.
 *
 * @author afzaal
 */
public class Normalisation
        extends AbstractRegexMatch {

  // this is used for the lookup
  String[] tmpSearchParameters = new String[2];

  @Override
  public IRecord procValidRecord(IRecord r) {
    String RegexGroup;
    PixipRecord CurrentRecord;
    ArrayList<String> Results;

    CurrentRecord = (PixipRecord) r;

    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      // ********************* B Number Normalisation *********************
      // Prepare the paramters to perform the search on
      tmpSearchParameters[0] = "";
      tmpSearchParameters[1] = CurrentRecord.BNumber;

      RegexGroup = CurrentRecord.teleserviceCode.value();

      Results = getRegexMatchWithChildData(RegexGroup, tmpSearchParameters);

      if ((Results != null) & (Results.size() > 1)) {
        if (isValidRegexMatchResult(Results)) {
          if (Results.get(0).isEmpty()) {
            // just add the prefix
            CurrentRecord.BNumberNorm = Results.get(1) + CurrentRecord.BNumber;
          } else {
            // remove an old prefix and add the new prefix
            CurrentRecord.BNumberNorm = CurrentRecord.BNumber.replaceAll(Results.get(0), Results.get(1));
          }
        } else {
          RecordError tmpError = new RecordError("ERR_B_NORMALISATION_LOOKUP", ErrorType.SPECIAL);
          tmpError.setModuleName(getSymbolicName());
          tmpError.setErrorDescription(CurrentRecord.BNumber);
          CurrentRecord.addError(tmpError);
          return r;
        }
      } else {
        RecordError tmpError = new RecordError("ERR_B_NORMALISATION_LOOKUP", ErrorType.SPECIAL);
        tmpError.setModuleName(getSymbolicName());
        tmpError.setErrorDescription(CurrentRecord.BNumber);
        CurrentRecord.addError(tmpError);
        return r;
      }

      // bnumber discard rule
      if (CurrentRecord.BNumberNorm.startsWith("9999")) {
        RecordError tmpError = new RecordError("DISC_B_NUMBER_DISCARD", ErrorType.SPECIAL);
        tmpError.setModuleName(getSymbolicName());
        tmpError.setErrorDescription(CurrentRecord.BNumber);
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
