/*
     * Copyright (C) 2007  The University of Kansas
     *
     * [INSERT KU-APPROVED LICENSE TEXT HERE]
     *
     */
/**
 * 
 */
package edu.ku.brc.specify.tools.fielddesc;

import static edu.ku.brc.ui.UIRegistry.getResourceString;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.specify.ui.HelpMgr;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Oct 3, 2007
 *
 */
public class SchemaToolsDlg extends CustomDialog
{
    protected JButton editSchemaBtn      = new JButton(getResourceString("SL_EDIT_SCHEMA"));
    protected JButton removeLocaleBtn    = new JButton(getResourceString("SL_REMOVE_SCHEMA_LOC"));
    protected JButton exportSchemaLocBtn = new JButton(getResourceString("SL_EXPORT_SCHEMA_LOC"));
    protected JList   localeList;

    
    /**
     * @param frame
     * @param title
     * @param isModal
     * @param whichBtns
     * @param contentPanel
     * @throws HeadlessException
     */
    public SchemaToolsDlg(final Frame frame) throws HeadlessException
    {
        super(frame, getResourceString("SL_TOOLS_TITLE"), true, OKHELP, null);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.CustomDialog#createUI()
     */
    @Override
    public void createUI()
    {
        setOkLabel(getResourceString("Close"));
        
        super.createUI();
        

        Vector<DisplayLocale> localeDisplays = new Vector<DisplayLocale>();
        for (Locale locale : SchemaLocalizerDlg.getLocalesInUseInDB())
        {
            localeDisplays.add(new DisplayLocale(locale));
        }
        
        localeList = new JList(localeDisplays);
        JScrollPane sp   = new JScrollPane(localeList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        CellConstraints cc = new CellConstraints();
        
        PanelBuilder builder   = new PanelBuilder(new FormLayout("p,2px,f:p:g", "p,2px,p,16px,p,4px,p,8px,p,10px"));
        builder.addSeparator(getResourceString("SL_LOCALES_IN_USE"), cc.xywh(1, 1, 3, 1));
        builder.add(sp, cc.xywh(1,3,3,1));
        
        builder.addSeparator(getResourceString(getResourceString("SL_TASKS")), cc.xywh(1, 5, 3, 1));
        builder.add(editSchemaBtn,      cc.xy(1,7));
        builder.add(removeLocaleBtn,    cc.xy(3,7));
        builder.add(exportSchemaLocBtn, cc.xy(1,9));
        
        contentPanel = builder.getPanel();
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        HelpMgr.setHelpID(getHelpBtn(), getResourceString("SL_TOOLS_HELP_CONTEXT"));
        
        enableBtns(false);
        
        localeList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                localeSelected();
            }
        });
        
        editSchemaBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0)
            {
                editSchema();
            }
        });
        
        removeLocaleBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0)
            {
                removeSchemaLocale();
            }
        });
        
        exportSchemaLocBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0)
            {
                exportSchemaLocales();
            }
        });
        
        pack();
    }
    
    /**
     * @param enable
     */
    protected void enableBtns(final boolean enable)
    {
        editSchemaBtn.setEnabled(enable);
        removeLocaleBtn.setEnabled(enable);
        exportSchemaLocBtn.setEnabled(enable);
    }

    /**
     * 
     */
    protected void localeSelected()
    {
        DisplayLocale dispLocale = (DisplayLocale)localeList.getSelectedValue();
        if (dispLocale != null)
        {
            enableBtns(true);
            
        } else
        {
            enableBtns(false);
        }
    }
    
    
    /**
     * 
     */
    protected void editSchema()
    {
        SwingUtilities.invokeLater(new Runnable() {

            public void run()
            {
                okButtonPressed();
                
                DisplayLocale dispLocale = (DisplayLocale)localeList.getSelectedValue();
                if (dispLocale != null)
                {
                    SchemaLocalizerDlg dlg = new SchemaLocalizerDlg((Frame)UIRegistry.getTopWindow(), dispLocale.getLocale());
                    dlg.setVisible(true);
                }
            }
        });
    }
    
    /**
     * 
     */
    protected void removeSchemaLocale()
    {
        
    }
    
    /**
     * 
     */
    protected void exportSchemaLocales()
    {
        
    }
}
