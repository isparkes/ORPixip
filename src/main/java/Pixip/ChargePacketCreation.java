/* ====================================================================
 * Limited Evaluation License:
 *
 * The exclusive owner of this work is Tiger Shore Management Ltd.
 * This work, including all associated documents and components
 * is Copyright Tiger Shore Management Ltd 2006-2010.
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

import OpenRate.process.AbstractRUMTimeMatch;
import OpenRate.process.AbstractStubPlugIn;
import OpenRate.record.ChargePacket;
import OpenRate.record.IRecord;
import static Pixip.model.TeleserviceCode.VOICE;

/**
 * This module creates the "seed" charge packet, which is used to drive the rest
 * of the rating process. This sets:
 *  - The time splitting flag, so that we do not check time splitting
 *  - The default Time Model (all plans use the same time model)
 *  - The default Zone Model (all plans use the same zone model)
 *  - The Zone Result will be looked up during zoning
 *  - The packet type "R" = "Retail" packet for base products
 *  - The priority 0 for base products, > 0 for overlay products - we use this 
 *    to control the error handling and rating -
 *  - The service we got from the CDR values
 *
 * @author Ian
 */
public class ChargePacketCreation extends AbstractStubPlugIn {

  // -----------------------------------------------------------------------------
  // ------------------ Start of inherited Plug In functions ---------------------
  // -----------------------------------------------------------------------------
  @Override
  public IRecord procValidRecord(IRecord r) {
    ChargePacket tmpCP;
    PixipRecord CurrentRecord = (PixipRecord) r;

    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      // ****************** Add the retail packet ********************
      //	initialise the Zone Model and time model with the value 'Default'.
      tmpCP = new ChargePacket();
      tmpCP.packetType = "R";                         // Default - Retail packet type
      tmpCP.zoneModel = "Default";                    // Default
      tmpCP.zoneResult = CurrentRecord.destination;   // Filled during zoning
      tmpCP.timeModel = CurrentRecord.usedProduct;    // To allow time zoning based on product - mapped in table TIME_MODEL_MAP to a time model
      tmpCP.service = CurrentRecord.Service;          // From CDR type
      tmpCP.ratePlanName = CurrentRecord.usedProduct; // Filled during rate plan lookup
      tmpCP.subscriptionID = "";                      // We don't need a subscription
      tmpCP.priority = 0;                             // Base product - prio 0

      // Mark voice records so we do splitting on them, others without
      if (CurrentRecord.teleserviceCode == VOICE) {
        tmpCP.timeSplitting = AbstractRUMTimeMatch.TIME_SPLITTING_CHECK_SPLITTING_BEAT_ROUNDING;
      } else {
        tmpCP.timeSplitting = AbstractRUMTimeMatch.TIME_SPLITTING_NO_CHECK;
      }
      CurrentRecord.addChargePacket(tmpCP);
    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r) {
    return r;
  }
}
