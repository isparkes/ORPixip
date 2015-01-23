/* ====================================================================
 * Limited Evaluation License:
 *
 * The exclusive owner of this work is Tiger Shore Management Ltd.
 * This work, including all associated documents and components
 * is Copyright Tiger Shore Management Limited 2006-2010.
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
