package Pixip;

import OpenRate.exception.InitializationException;
import OpenRate.exception.ProcessingException;
import OpenRate.process.AbstractRUMRateCalc;
import OpenRate.record.ErrorType;
import OpenRate.record.IRecord;
import OpenRate.record.RecordError;

/**
 * Perform rating based on the information we have looked up.
 */
public class Rating extends AbstractRUMRateCalc {

  // -----------------------------------------------------------------------------
  // ------------------ Start of inherited Plug In functions ---------------------
  // -----------------------------------------------------------------------------
  /**
   * Initialise the module. Called during pipeline creation to initialise: -
   * Configuration properties that are defined in the properties file. - The
   * references to any cache objects that are used in the processing - The
   * symbolic name of the module
   *
   * @param PipelineName The name of the pipeline this module is in
   * @param ModuleName The name of this module in the pipeline
   * @throws OpenRate.exception.InitializationException
   */
  @Override
  public void init(String PipelineName, String ModuleName)
          throws InitializationException {
    // Do the inherited work, e.g. setting the symbolic name etc
    super.init(PipelineName, ModuleName);

    // Set exception reporting - we want to manage the exceptions
    this.setExceptionReporting(true);
  }

  /**
   * This is called when a data record is encountered. You should do any normal
   * processing here.
   *
   * @return
   */
  @Override
  public IRecord procValidRecord(IRecord r) {
    RecordError tmpError;
    PixipRecord CurrentRecord = (PixipRecord) r;

    // First of all, see what the session time is if we are calculating
    // the max session time. In a second step we will then rate the session
    // time in order to know the amount of money to reserve
    // Lookup the customer for all cases
    // We only transform the detail records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.DETAIL_RECORD) {
      try {
        performRating(CurrentRecord);
      } catch (ProcessingException pe) {
        tmpError = new RecordError("ERR_RATE_LOOKUP", ErrorType.SPECIAL, getSymbolicName(), pe.getMessage());
        CurrentRecord.addError(tmpError);
      }
    }

    return r;
  }

  /**
   * No processing for error records.
   *
   * @return
   */
  @Override
  public IRecord procErrorRecord(IRecord r) {

    return r;
  }
}
