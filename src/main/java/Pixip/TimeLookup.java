/* ====================================================================
 * Limited Evaluation License:
 *
 * The exclusive owner of this work is OpenRate Project.
 * This work, including all associated documents and components
 * is Copyright Tiger Shore Management Limited 2006-2014.
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
