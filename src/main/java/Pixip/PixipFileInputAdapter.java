package Pixip;

import OpenRate.adapter.file.FlatFileInputAdapter;
import OpenRate.record.FlatRecord;
import OpenRate.record.HeaderRecord;
import OpenRate.record.IRecord;
import OpenRate.record.TrailerRecord;

/**
 * Instance of the input adapter for the Ventelo traffic type.
 *
 * @author TGDSPIA1
 */
public class PixipFileInputAdapter extends FlatFileInputAdapter {

  private int IntRecordNumber;

  // -----------------------------------------------------------------------------
  // ------------------ Start of inherited Plug In functions ---------------------
  // -----------------------------------------------------------------------------  
  /**
   * This is called when the synthetic Header record is encountered, and has the
   * meaning that the stream is starting. In this example we have nothing to do
   *
   * @return
   */
  @Override
  public HeaderRecord procHeader(HeaderRecord r) {
    IntRecordNumber = 0;

    return r;
  }

  /**
   * This is called when a data record is encountered. You should do any normal
   * processing here. For the input adapter, we probably want to change the
   * record type from FlatRecord to the record(s) type that we will be using in
   * the processing pipeline.
   *
   * This is also the location for accumulating records into logical groups
   * (that is records with sub records) and placing them in the pipeline as they
   * are completed. If you receive a sub record, simply return a null record in
   * this method to indicate that you are handling it, and that it will be
   * purged at a later date.
   *
   * @return
   */
  @Override
  public IRecord procValidRecord(FlatRecord r) {
    PixipRecord tmpDataRecord;
    FlatRecord tmpFlatRecord;

    /* The source of the record is FlatRecord, because we are using the
     * FlatFileInputAdapter as the source of the records. We cast the record
     * to this to extract the data, and then create the target record type
     * (CustomizedRecord) and cast this back to the generic class before passing
     * back
     */
    tmpFlatRecord = (FlatRecord) r;

    // Create the new record
    tmpDataRecord = new PixipRecord();

    // Normal detail record
    tmpDataRecord.mapFileDetailRecord(tmpFlatRecord.getData());
    IntRecordNumber++;
    tmpDataRecord.recordNumber = IntRecordNumber;

    // Return the modified record in the Common record format (IRecord)
    return (IRecord) tmpDataRecord;
  }

  /**
   * This is called when a data record with errors is encountered. You should do
   * any processing here that you have to do for error records, e.g. statistics,
   * special handling, even error correction!
   *
   * The input adapter is not expected to provide any records here.
   *
   * @return
   */
  @Override
  public IRecord procErrorRecord(FlatRecord r) {
    // The FlatFileInputAdapter is not able to create error records, so we
    // do not have to do anything for this
    return r;
  }

  /**
   * This is called when the synthetic trailer record is encountered, and has
   * the meaning that the stream is now finished. In this example, all we do is
   * pass the control back to the transactional layer.
   *
   * @return
   */
  @Override
  public TrailerRecord procTrailer(TrailerRecord r) {

    return r;
  }
}
