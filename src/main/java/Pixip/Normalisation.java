/* ====================================================================
 * Limited Evaluation License:
 *
 * The exclusive owner of this work is Tiger Shore Management Ltd.
 * This work, including all associated documents and components
 * is Copyright Tiger Shore Management Ltd 2006-2013.
 *
 * The following restrictions apply unless they are expressly relaxed in a
 * contractual agreement between the license holder or one of its officially
 * assigned agents and you or your organisation:
 *
 * 1) This work may not be disclosed, either in full or in part, in any form
 *    electronic or physical, to any third party. This includes both in the
 *    form of source code and compiled modules.
 * 2) This work contains trade secrets in the form of architecture, algorithms
 *    methods and technologies. These trade secrets may not be disclosed to
 *    third parties in any form, either directly or in summary or paraphrased
 *    form, nor may these trade secrets be used to construct products of a
 *    similar or competing nature either by you or third parties.
 * 3) This work may not be included in full or in part in any application.
 * 4) You may not remove or alter any proprietary legends or notices contained
 *    in or on this work.
 * 5) This software may not be reverse-engineered or otherwise decompiled, if
 *    you received this work in a compiled form.
 * 6) This work is licensed, not sold. Possession of this software does not
 *    imply or grant any right to you.
 * 7) You agree to disclose any changes to this work to the copyright holder
 *    and that the copyright holder may include any such changes at its own
 *    discretion into the work
 * 8) You agree not to derive other works from the trade secrets in this work,
 *    and that any such derivation may make you liable to pay damages to the
 *    copyright holder
 * 9) You agree to use this software exclusively for evaluation purposes, and
 *    that you shall not use this software to derive commercial profit or
 *    support your business or personal activities.
 *
 * This software is provided "as is" and any expressed or impled warranties,
 * including, but not limited to, the impled warranties of merchantability
 * and fitness for a particular purpose are disclaimed. In no event shall
 * Tiger Shore Management or its officially assigned agents be liable to any
 * direct, indirect, incidental, special, exemplary, or consequential damages
 * (including but not limited to, procurement of substitute goods or services;
 * Loss of use, data, or profits; or any business interruption) however caused
 * and on theory of liability, whether in contract, strict liability, or tort
 * (including negligence or otherwise) arising in any way out of the use of
 * this software, even if advised of the possibility of such damage.
 * This software contains portions by The Apache Software Foundation, Robert
 * Half International.
 * ====================================================================
 */
package Pixip;

import OpenRate.process.AbstractRegexMatch;
import OpenRate.record.ErrorType;
import OpenRate.record.IRecord;
import OpenRate.record.RecordError;
import java.util.ArrayList;

/**
 * This module performs rule driven normalisation on the B numbers, producing a
 * number which can be used in the zoning. Note that because the zoning has two
 * cases, there can be two different sorts of output:
 *  - C-Type number cases: In this case, the C is removed from the number,
 *    leaving only the C-Type prefix to be conpared to the zone model
 *  - Normal numbers: in this case the number is fully normalised, giving a
 *    result with IAC+CC+NDC. The IAC here is "00"
 *
 * @author afzaal
 */
public class Normalisation 
  extends AbstractRegexMatch
{
  // this is used for the lookup
  String[] tmpSearchParameters = new String[2];
  
  // -----------------------------------------------------------------------------
  // ------------------ Start of inherited Plug In functions ---------------------
  // -----------------------------------------------------------------------------

 /**
  * This is called when a data record is encountered. You should do any normal
  * processing here.
  * 
  * @return The processed record
  */
  @Override
  public IRecord procValidRecord(IRecord r)
  {
    String RegexGroup;
    PixipRecord CurrentRecord;
    ArrayList<String> Results;
    
    CurrentRecord = (PixipRecord) r;

    if (CurrentRecord.RECORD_TYPE == PixipRecord.FILE_DETAIL_RECORD)
    {
      // ********************* B Number Normalisation *********************
      // Prepare the paramters to perform the search on
      tmpSearchParameters[0] = "";
      tmpSearchParameters[1] = CurrentRecord.B_Number;

      RegexGroup = "TEL";

      Results = getRegexMatchWithChildData(RegexGroup,tmpSearchParameters);

      if ((Results != null) & (Results.size() > 1))
      {
        if (isValidRegexMatchResult(Results))
        {
          if (Results.get(0).isEmpty())
          {
            // just add the prefix
            CurrentRecord.B_NumberNorm = Results.get(1) + CurrentRecord.B_Number;
          }
          else
          {
            // remove an old prefix and add the new prefix
            CurrentRecord.B_NumberNorm = CurrentRecord.B_Number.replaceAll(Results.get(0), Results.get(1));
          }
        }
        else
        {
          RecordError tmpError = new RecordError("ERR_B_NORMALISATION_LOOKUP", ErrorType.SPECIAL);
          tmpError.setModuleName(getSymbolicName());
          tmpError.setErrorDescription(CurrentRecord.B_Number);
          CurrentRecord.addError(tmpError);
          return r;
        }
      }
      else
      {
        RecordError tmpError = new RecordError("ERR_B_NORMALISATION_LOOKUP", ErrorType.SPECIAL);
        tmpError.setModuleName(getSymbolicName());
        tmpError.setErrorDescription(CurrentRecord.B_Number);
        CurrentRecord.addError(tmpError);
        return r;
      }
      
      // bnumber discard rule
      if (CurrentRecord.B_NumberNorm.startsWith("9999"))
      {
        RecordError tmpError = new RecordError("DISC_B_NUMBER_DISCARD", ErrorType.SPECIAL);
        tmpError.setModuleName(getSymbolicName());
        tmpError.setErrorDescription(CurrentRecord.B_Number);
        CurrentRecord.addError(tmpError);
        return r;
      }      
    }

    return r;
  }

 /**
  * This is called when a data record with errors is encountered. You should do
  * any processing here that you have to do for error records, e.g. statistics,
  * special handling, even error correction!
  * 
  * @return The processed record
  */
  @Override
  public IRecord procErrorRecord(IRecord r)
  {
    return r;
  }

  // -----------------------------------------------------------------------------
  // ------------------------ Start of custom functions --------------------------
  // -----------------------------------------------------------------------------

}
