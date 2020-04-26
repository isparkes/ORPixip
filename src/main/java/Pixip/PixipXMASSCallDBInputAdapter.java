package Pixip;

import OpenRate.adapter.jdbc.JDBCInputAdapter;
import OpenRate.record.DBRecord;
import OpenRate.record.HeaderRecord;
import OpenRate.record.IRecord;
import OpenRate.record.TrailerRecord;

/**
 * This is the input adapter reading the CDRs from a table instead of a file
 *
 * @author Afzaal
 */
public class PixipXMASSCallDBInputAdapter extends JDBCInputAdapter {

  // This is the stream record number counter which tells us the number of
  // the compressed records
  private int StreamRecordNumber;

  // This is the object that is used to compress the records
  PixipRecord tmpDataRecord = null;

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
    StreamRecordNumber = 0;
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
  public IRecord procValidRecord(DBRecord r) {

    tmpDataRecord = new PixipRecord();

    // map the data to the working fields
    tmpDataRecord.mapXMASSCallDBDetailRecord(r.getOriginalColumns());

    // Return the created record
    StreamRecordNumber++;
    tmpDataRecord.recordNumber = StreamRecordNumber;

    return tmpDataRecord;

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
  public IRecord procErrorRecord(DBRecord r) {
    return r;
  }

  /**
   * This is called when the synthetic trailer record is encountered, and has
   * the meaning that the stream is now finished. In this example, all we do
   * is pass the control back to the transactional layer.
   *
   * @return
   */
  @Override
  public TrailerRecord procTrailer(TrailerRecord r) {
    TrailerRecord tmpTrailer;

    // set the trailer record count
    tmpTrailer = r;
    tmpTrailer.setRecordCount(StreamRecordNumber);

    return tmpTrailer;
  }
}
