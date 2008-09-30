/*
     * Copyright (C) 2008  The University of Kansas
     *
     * [INSERT KU-APPROVED LICENSE TEXT HERE]
     *
     */
/**
 * 
 */
package edu.ku.brc.specify.datamodel.busrules;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;

import edu.ku.brc.af.ui.db.ViewBasedDisplayDialog;
import edu.ku.brc.af.ui.forms.BaseBusRules;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.af.ui.forms.Viewable;
import edu.ku.brc.af.ui.forms.validation.ValComboBox;
import edu.ku.brc.af.ui.forms.validation.ValTextField;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.config.DisciplineType;
import edu.ku.brc.specify.datamodel.AutoNumberingScheme;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Sep 28, 2008
 *
 */
public class CollectionSetupBusRules extends BaseBusRules
{
    protected ValComboBox numSchemeCBX      = null;
    protected ValComboBox disciplineTypeCBX = null;
    
    //protected AutoNumberingScheme numberingScheme = null;
    protected Collection          collection      = null;
    
    /**
     * @param dataClasses
     */
    public CollectionSetupBusRules()
    {
        super(Collection.class);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#afterFillForm(java.lang.Object)
     */
    @Override
    public void afterFillForm(Object dataObj)
    {
        super.afterFillForm(dataObj);
        
        collection = (Collection)dataObj;
        
        /*
        Collection collection = (Collection)dataObj;
        if (collection != null)
        {
            PickListDBAdapterIFace plAdapter = (PickListDBAdapterIFace)numSchemeCBX.getModel();
            
            for (AutoNumberingScheme ans : collection.getNumberingSchemes())
            {
                for (PickListItemIFace pli : plAdapter.getList())
                {
                    if (ans.getIdentityTitle().equals(pli.getTitle()))
                    {
                        numSchemeCBX.getComboBox().setSelectedItem(pli);
                        break;
                    }
                }
            }
        }*/
        
        if (disciplineTypeCBX != null)
        {
            DefaultComboBoxModel model = (DefaultComboBoxModel)disciplineTypeCBX.getComboBox().getModel();
            for (DisciplineType disciplineType : DisciplineType.getDisciplineList())
            {
                if (disciplineType.getType() == 0)
                {
                    model.addElement(disciplineType);
                }
            }
            
            //disciplineTypeCBX
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#beforeMerge(java.lang.Object, edu.ku.brc.dbsupport.DataProviderSessionIFace)
     */
    @Override
    public void beforeMerge(Object dataObj, DataProviderSessionIFace session)
    {
        super.beforeMerge(dataObj, session);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#formShutdown()
     */
    @Override
    public void formShutdown()
    {
        super.formShutdown();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#initialize(edu.ku.brc.af.ui.forms.Viewable)
     */
    @Override
    public void initialize(Viewable viewableArg)
    {
        super.initialize(viewableArg);
        
        //numSchemeCBX      = (ValComboBox)formViewObj.getCompById("4");
        disciplineTypeCBX = (ValComboBox)formViewObj.getCompById("disciplineCBX");
        
        JButton btn = (JButton)formViewObj.getCompById("newNumSchemeBTN");
        if (btn != null)
        {
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    createNewNumScheme();
                }
            });
            btn = (JButton)formViewObj.getCompById("chooseNumSchemeBTN");
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    chooseNumScheme();
                }
            });
        }
    }
    
    protected void createNewNumScheme()
    {
        ViewBasedDisplayDialog dlg = new ViewBasedDisplayDialog((Dialog)UIRegistry.getMostRecentWindow(),
                null,
                "CatAutoNumberingScheme",
                null,
                "Create Numbering Scheme",
                null,
                AutoNumberingScheme.class.getName(),
                "autoNumberingSchemeId",
                true,
                MultiView.HIDE_SAVE_BTN);
        AutoNumberingScheme scheme = new AutoNumberingScheme();
        scheme.initialize();
        scheme.setTableNumber(Collection.getClassTableId());
        dlg.setData(scheme);
        UIHelper.centerAndShow(dlg);
        if (!dlg.isCancelled())
        {
            ValTextField numSchemTxt = (ValTextField)formViewObj.getCompById("numScheme");
            numSchemTxt.setText(scheme.getIdentityTitle());
            
            if (collection != null)
            {
                collection.getNumberingSchemes().add(scheme);
                scheme.getCollections().add(collection);
            }
        }
    }

    protected void chooseNumScheme()
    {
        
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.ui.forms.BaseBusRules#okToEnableDelete(java.lang.Object)
     */
    @Override
    public boolean okToEnableDelete(Object dataObj)
    {
        return super.okToEnableDelete(dataObj);
    }

}
