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

import OpenRate.process.AbstractBestMatch;
import OpenRate.record.ChargePacket;
import OpenRate.record.ErrorType;
import OpenRate.record.IRecord;
import OpenRate.record.RecordError;
import static Pixip.model.TeleserviceCode.GPRS;
import java.util.ArrayList;

/**
 * Look up the zone result, category and type based on the B Number. The zone
 * result is used to calculate the zone for rating purposes, the category is
 * used for billing output information and markup, and the type is used for
 * markup.
 */
public class ZoneLookup extends AbstractBestMatch {

  // convenience variables
  private final int IDX_ZONE_RESULT = 0; // Zone result used for rating
  private final int IDX_ZONE_CAT = 1;    // Category

  // -----------------------------------------------------------------------------
  // ------------------ Start of inherited Plug In functions ---------------------
  // -----------------------------------------------------------------------------
  @Override
  public IRecord procValidRecord(IRecord r) {
    RecordError tmpError;
    ArrayList<String> ZoneValue;
    PixipRecord CurrentRecord = (PixipRecord) r;

    // We only transform the detail records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      
      // Put in defaults: These will be filled by world zone lookup
      CurrentRecord.origZone = "";
      CurrentRecord.destZone = "";
      
      if (CurrentRecord.teleserviceCode.equals(GPRS)) {
        // Write the information back into the record
        CurrentRecord.destination = "GPRS";
        CurrentRecord.destCategory = "GPRS";
      } else {
        try {
          // Look up the Destination from the general list
          ZoneValue = getBestMatchWithChildData("Default", CurrentRecord.BNumberNorm);
        } catch (ArrayIndexOutOfBoundsException ex) {
          // B Number containing strange characters
          tmpError = new RecordError("ERR_INVALID_B_NUMBER", ErrorType.SPECIAL);
          CurrentRecord.addError(tmpError);
          return r;
        }

        if (isValidBestMatchResult(ZoneValue)) {
          // Write the information back into the record
          CurrentRecord.destination = ZoneValue.get(IDX_ZONE_RESULT);
          CurrentRecord.destCategory = ZoneValue.get(IDX_ZONE_CAT);
        } else {
          // no zone found, add an error to the record
          tmpError = new RecordError("ERR_ZONE_LOOKUP", ErrorType.SPECIAL);
          CurrentRecord.addError(tmpError);
          return r;
        }
      }

      // ****************************** CPs **********************************
      // Place the result into the charge packet(s)
      for (int idx = 0; idx < CurrentRecord.getChargePacketCount(); idx++) {
        ChargePacket tmpCP = CurrentRecord.getChargePacket(idx);

        tmpCP.zoneResult = CurrentRecord.destination;
        tmpCP.zoneInfo = CurrentRecord.destCategory;
      }
    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r
  ) {
    return r;
  }
}
