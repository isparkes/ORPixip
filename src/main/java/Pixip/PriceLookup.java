package Pixip;

import OpenRate.process.AbstractRegexMatch;
import OpenRate.record.ChargePacket;
import OpenRate.record.ErrorType;
import OpenRate.record.IRecord;
import OpenRate.record.RecordError;
import OpenRate.record.TimePacket;

/**
 * Look up the price group for the cases where we have to calculate the retail
 * price instead of just marking up. This looks up the combination of:
 *  - tariff
 *  - zone result
 *  - time result
 *  - service
 * 
 * To arrive at a price group to be applied.
 */
public class PriceLookup extends AbstractRegexMatch {

  // Regex search parameters - defined here for performance reasons
  private final String[] tmpSearchParameters = new String[5];

  @Override
  public IRecord procValidRecord(IRecord r) {
    PixipRecord CurrentRecord = (PixipRecord) r;

    // We only transform the detail records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
        tmpSearchParameters[0] = CurrentRecord.service; 
        tmpSearchParameters[1] = CurrentRecord.origZone; 
        tmpSearchParameters[2] = CurrentRecord.destZone;
        
        // Find the price group and place them into the charge packets
        for (ChargePacket tmpCP : CurrentRecord.getChargePackets()) {
          if (tmpCP.Valid) {
            for (TimePacket tmpTZ : tmpCP.getTimeZones()) {
              tmpSearchParameters[3] = tmpCP.zoneResult;
              tmpSearchParameters[4] = tmpTZ.timeResult;
              String tmpPriceGroup = getRegexMatch(tmpCP.ratePlanName, tmpSearchParameters);

              if (isValidRegexMatchResult(tmpPriceGroup)) {
                tmpTZ.priceGroup = tmpPriceGroup;
              } else {
                // if this is a base product, error, otherwise turn the CP off
                if (tmpCP.priority == 0) {
                  // base product
                  CurrentRecord.addError(new RecordError("ERR_BASE_PROD_PRICE_MAP", ErrorType.DATA_NOT_FOUND));
                } else {
                  // overlay product
                  tmpCP.Valid = false;
                }
              }
            }
          }
        }
    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r) {
    return r;
  }
}
