/* ====================================================================
 * Limited Evaluation License:
 *
 * This software is open source, but licensed. The license with this package
 * is an evaluation license, which may not be used for productive systems. If
 * you want a full license, please contact us.
 *
 * The exclusive owner of this work is the OpenRate project.
 * This work, including all associated documents and components
 * is Copyright of the OpenRate project 2006-2014.
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
 * The OpenRate Project or its officially assigned agents be liable to any
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

import OpenRate.adapter.file.FlatFileOutputAdapter;
import OpenRate.record.FlatRecord;
import OpenRate.record.IRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The Output Adapter is reponsible for writing the completed records to the
 * target file.
 */
public class XMLOutputAdapter extends FlatFileOutputAdapter
{
  /**
   * CVS version info - Automatically captured and written to the Framework
   * Version Audit log at Framework startup. For more information
   * please <a target='new' href='http://www.open-rate.com/wiki/index.php?title=Framework_Version_Map'>click here</a> to go to wiki page.
   */
  public static String CVS_MODULE_INFO = "OpenRate, $RCSfile: XMLOutputAdapter.java,v $, $Revision: 1.7 $, $Date: 2014-06-26 14:56:10 $";

  /**
  * Constructor for CustomizeOutputAdapter.
  */
  public XMLOutputAdapter()
  {
    super();
  }

  /**
   * We transform the records here so that they are ready to output making any
   * specific changes to the record that are necessary to make it ready for
   * output.
   *
   * As we are using the FlatFileOutput adapter, we should transform the records
   * into FlatRecords, storing the data to be written using the SetData() method.
   * This means that we do not have to know about the internal workings of the
   * output adapter.
   *
   * Note that this is just undoing the transformation that we did in the input
   * adapter.
   */
  @Override
    public void openValidFile(String filename)
  {
    super.openValidFile(filename);

    try
    {
      getValidWriter().write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
              + "\n"
              + "<calls>\n");
    }
    catch( IOException ex )
    {
      getPipeLog().error("Exception in module <"+getSymbolicName()+">, <"+ex.getMessage()+">");
    }
  }

  @Override
  public int closeFiles(int TransactionNumber)
  {
    try
    {
      getValidWriter().write("</calls>");
    }
    catch( IOException ex )
    {
      getPipeLog().error("Exception in module <"+getSymbolicName()+">, <"+ex.getMessage()+">");
    }

    return super.closeFiles(TransactionNumber);
  }

  @Override
  public Collection<IRecord> procValidRecord(IRecord r)
  {
    FlatRecord tmpOutRecord;
    PixipRecord CurrentRecord;

    Collection<IRecord> Outbatch;
    Outbatch = new ArrayList<>();

    tmpOutRecord = new FlatRecord();
    CurrentRecord = (PixipRecord)r;

    // We only transform the detail records, and leave the others alone
    if (CurrentRecord.RECORD_TYPE == PixipRecord.FILE_DETAIL_RECORD) {
      tmpOutRecord.setData("\t<call>\n" +
            wrapValue("customerId"              , CurrentRecord.CustIDA        ) +
            wrapValue("subscriptionId"          , CurrentRecord.subscriptionID ) +
            wrapValue("call_a_number"           , CurrentRecord.A_Number       ) +
            wrapValue("call_b_number"           , CurrentRecord.B_Number       ) +
            wrapValue("call_started"            , CurrentRecord.Call_Date      ) +
            wrapValue("call_time"               , CurrentRecord.Call_Time      ) +
            wrapValue("call_rated_start_amount" , CurrentRecord.outputConnCost ) +
            wrapValue("call_rated_total_amount" , CurrentRecord.outputTotalCost) +
            wrapValue("wholesale_amount"        , CurrentRecord.origAmount     ) +
            wrapValue("margin"                  , CurrentRecord.marginFlag     ) +
            wrapValue("price_plan"              , CurrentRecord.UsedProduct    ) +
            wrapValue("supplier"                , CurrentRecord.supplier       ) +
            wrapValue("country"                 , CurrentRecord.Zone_Cat       ) +
            wrapValue("call_type"               , CurrentRecord.Dest_Phone_Type) +
            "\t</call>");
      Outbatch.add((IRecord)tmpOutRecord);
    }

    return Outbatch;
  }

  /**
   * Handle any error records here so that they are ready to output making any
   * specific changes to the record that are necessary to make it ready for
   * output.
   * @return 
   */
  @Override
  public Collection<IRecord> procErrorRecord(IRecord r)
  {
    return null;
  }

 /**
  * Wrap a value as a level 2 xml tag.
  *
  * @param tag The tag name
  * @param value The value to wrap
  * @return The wrapped value
  */
  private String wrapValue(String tag, String value)
  {
    String result;

    result = "\t\t<"+tag+">"+ value +"</"+tag+">\n";

    return result.replaceAll("&", "&amp;");
  }

 /**
  * Wrap a value as a level 2 xml tag.
  *
  * @param tag The tag name
  * @param value The value to wrap
  * @return The wrapped value
  */
  private String wrapValue(String tag, int value)
  {
    String result;

    result = "\t\t<"+tag+">"+ value +"</"+tag+">\n";

    return result.replaceAll("&", "&amp;");
  }

 /**
  * Wrap a value as a level 2 xml tag.
  *
  * @param tag The tag name
  * @param value The value to wrap
  * @return The wrapped value
  */
  private String wrapValue(String tag, double value)
  {
    String result;

    result = "\t\t<"+tag+">"+ value +"</"+tag+">\n";

    return result.replaceAll("&", "&amp;");
  }
}
