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
      tmpCP.service = CurrentRecord.service;          // From CDR type
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
