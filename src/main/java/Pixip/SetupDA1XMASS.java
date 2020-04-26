package Pixip;

import OpenRate.process.AbstractStubPlugIn;
import OpenRate.record.IRecord;

/**
 * set up the DA field so that the normal processing can use it. Only for 
 * XMASS tables.
 *
 * @author ian
 */
public class SetupDA1XMASS extends AbstractStubPlugIn {

  @Override
  public IRecord procValidRecord(IRecord r) {

    PixipRecord CurrentRecord = (PixipRecord) r;

    // We only transform the detail records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {

      if ((CurrentRecord.beforeDA1 - CurrentRecord.afterDA1) != 0) {
        CurrentRecord.chargeDA1 = CurrentRecord.expectedDA1;
      }
    }

    return r;
  }

  @Override
  public IRecord procErrorRecord(IRecord r
  ) {
    // do nothing
    return r;
  }
}
