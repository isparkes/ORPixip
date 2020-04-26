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
