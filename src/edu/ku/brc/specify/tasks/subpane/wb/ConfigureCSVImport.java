/*
 * Copyright (C) 2007 The University of Kansas
 * 
 * [INSERT KU-APPROVED LICENSE TEXT HERE]
 * 
 */
package edu.ku.brc.specify.tasks.subpane.wb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Vector;

import org.apache.poi.hssf.usermodel.HSSFCell;

import com.csvreader.CsvReader;

import edu.ku.brc.ui.ChooseFromListDlg;

/**
 * @author timbo
 * 
 * @code_status Alpha
 * 
 * Configures a csv file for import to a workbench. Currently the default behavior is to use
 * annoying prompts for delimiter, escapemode, character set, and whether the first row has headers.
 * If first row has no headers they are automatically set to "column1", "column2" ...
 * 
 * Error handling is not handled.
 */
public class ConfigureCSVImport extends ConfigureImportBase implements ConfigureDataImport
{
    private int     escapeMode;
    private char    delimiter;
    private Charset charset;

    /**
     * Constructor sets defaults (hard coded)
     */
    public ConfigureCSVImport(File file)
    {
        super();
        escapeMode = getDefaultEscapeMode();
        delimiter = getDefaultDelimiter();
        charset = getDefaultCharset();
        getConfig(file);
    }

    public char getDelimiter()
    {
        return delimiter;
    }

    public Charset getCharset()
    {
        return charset;
    }

    public int getEscapeMode()
    {
        return escapeMode;
    }

    /**
     * @param delimiter -
     *            the column delimiter
     * @param charset -
     *            the character set used in the file (e.g. ISO-8859-1)
     * @param escapeMode -
     *            method used to escape reserved characters (backslash or doubled)
     * @return CsvReader for inputFile
     */
    private CsvReader makeReader(char delimiter, Charset charset, int escapeMode)
    {
        try
        {
            InputStream input = new FileInputStream(inputFile);
            CsvReader result = new CsvReader(input, delimiter, charset);
            result.setEscapeMode(escapeMode);
            return result;
        } catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * creates reader using current configuration
     * 
     * @return CsvReader for inputFile
     */
    private CsvReader makeReader()
    {
        return makeReader(delimiter, charset, escapeMode);
    }

    /**
     * Currently (tentatively) used to generate default column headers.
     * 
     * @return "Column"
     */
    private String getDefaultColHeader()
    {
        return "Column";
    }

    /**
     * Tentatively used to generate default column headers
     * 
     * @return ("Column1", "Column2", ...)
     */
    private String[] setupHeaders()
    {
        CsvReader csv = makeReader();
        if (csv != null)
        {
            try
            {
                if (csv.readRecord())
                {
                    String[] result = new String[csv.getColumnCount()];
                    for (int h = 0; h < csv.getColumnCount(); h++)
                    {
                        result[h] = getDefaultColHeader() + String.valueOf(h + 1);
                    }
                    return result;
                }
            } catch (IOException ex)
            {
                ex.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Lame prompt for delimiter.
     * 
     * @return selected delimiter
     */
    private char determineDelimiter()
    {
        Vector<String> list = new Vector<String>();
        list.add(",");
        list.add("TAB");
        ChooseFromListDlg<String> dlg = new ChooseFromListDlg<String>(null, "Delimiter?", list);
        dlg.setVisible(true);

        String delim = dlg.getSelectedObject();

        if (delim == "TAB")
        {
            return '\t';
        } else
        {
            return ',';
        }
    }

    private char getDefaultDelimiter()
    {
        return ',';
    }

    /**
     * Lame prompt for Character set.
     * 
     * @return selected character set
     */
    private Charset determineCharset()
    {
        Vector<String> list = new Vector<String>();
        list.add("default");
        list.add("US-ASCII");
        list.add("ISO-8859-1");
        list.add("UTF-8");
        ChooseFromListDlg<String> dlg = new ChooseFromListDlg<String>(null, "Character Set", list);
        dlg.setVisible(true);

        String delim = dlg.getSelectedObject();

        if (delim == "default")
        {
            return Charset.defaultCharset();
        } else return Charset.forName(delim);
    }

    private Charset getDefaultCharset()
    {
        return Charset.defaultCharset();
    }

    /**
     * lame prompt for escape mode.
     * 
     * @return selected escape mode
     */
    private int determineEscapeMode()
    {
        Vector<String> list = new Vector<String>();
        list.add("backslash");
        list.add("doubled");
        ChooseFromListDlg<String> dlg = new ChooseFromListDlg<String>(null, "Escape Mode?", list);
        dlg.setVisible(true);

        String delim = dlg.getSelectedObject();

        if (delim == "backslash")
        {
            return CsvReader.ESCAPE_MODE_BACKSLASH;
        } else return CsvReader.ESCAPE_MODE_DOUBLED;
    }

    private int getDefaultEscapeMode()
    {
        return CsvReader.ESCAPE_MODE_DOUBLED;
    }

    private int getCellType(int colIndex)
    {
        return HSSFCell.CELL_TYPE_STRING; // hmmmm....
    }

    /*
     * (non-Javadoc) Gets configuration properties from user.
     * 
     * @see edu.ku.brc.specify.tasks.subpane.wb.ConfigureImportBase#interactiveConfig()
     */
    protected void interactiveConfig()
    {
        delimiter = determineDelimiter();
        charset = determineCharset();
        escapeMode = determineEscapeMode();
        firstRowHasHeaders = determineFirstRowHasHeaders();
        nonInteractiveConfig();
    }

    /*
     * (non-Javadoc)
     * 
     * Sets up colInfo for inputFile.
     * 
     * @see edu.ku.brc.specify.tasks.subpane.wb.ConfigureImportBase#nonInteractiveConfig()
     */
    protected void nonInteractiveConfig()
    {
        CsvReader csv = makeReader();
        if (csv != null)
        {
            try
            {
                if (firstRowHasHeaders)
                {
                    csv.readHeaders();
                } else
                {
                    csv.setHeaders(setupHeaders());
                }

                colInfo = new Vector<ImportColumnInfo>(csv.getHeaderCount());

                for (int h = 0; h < csv.getHeaderCount(); h++)
                {
                    colInfo.add(new ImportColumnInfo(h, getCellType(h), csv.getHeader(h), null));
                }
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
            Collections.sort(colInfo);
        }
    }
}
