package edu.ku.brc.specify.tasks.subpane.security;

import java.awt.Frame;
import java.util.HashSet;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import edu.ku.brc.af.auth.specify.principal.GroupPrincipal;
import edu.ku.brc.af.ui.db.ViewBasedDisplayDialog;
import edu.ku.brc.af.ui.forms.MultiView;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.config.init.DataBuilder;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.Institution;
import edu.ku.brc.specify.datamodel.SpPrincipal;
import edu.ku.brc.specify.datamodel.SpecifyUser;
import edu.ku.brc.specify.datamodel.UserGroupScope;
import edu.ku.brc.ui.UIRegistry;

/**
 * This class perform operations on the security administration navigation tree, such as 
 * the creation or deletion of an item on the tree. An items is one instance of Discipline,
 * Collection, SpPrincipal (user group) or SpecifyUser. 
 * 
 * @author Ricardo
 *
 */
public class NavigationTreeMgr
{
    private static final Logger log = Logger.getLogger(NavigationTreeMgr.class);

    private JTree tree;
    
    /**
     * @param tree
     */
    NavigationTreeMgr(final JTree tree)
    {
        this.tree = tree;
    }

    /**
     * @return
     */
    public final JTree getTree()
    {
        return tree;
    }
    
    /**
     * @param grpNode
     */
    public void addNewUser(final DefaultMutableTreeNode grpNode) 
    {
        if (grpNode == null || !(grpNode.getUserObject() instanceof DataModelObjBaseWrapper))
        {
            return; // Nothing is selected or object type isn't relevant 
        }
        
        DataModelObjBaseWrapper parentWrp  = (DataModelObjBaseWrapper) (grpNode.getUserObject());
        if (!parentWrp.isGroup())
        {
            return; // selection isn't a suitable parent for a group
        }
        
        SpPrincipal group = (SpPrincipal) parentWrp.getDataObj();
        SpecifyUser user = new SpecifyUser();
        user.initialize();
        
        ViewBasedDisplayDialog dlg = new ViewBasedDisplayDialog((Frame)UIRegistry.getMostRecentWindow(),
                                                                null,
                                                                "User",
                                                                null,
                                                                "New User",
                                                                null,
                                                                user.getClass().getName(),
                                                                "specifyUserId",
                                                                true,
                                                               MultiView.HIDE_SAVE_BTN | MultiView.DONT_ADD_ALL_ALTVIEWS | MultiView.USE_ONLY_CREATION_MODE);
        dlg.setData(user);
        dlg.setVisible(true);
        if (!dlg.isCancelled())
        {
            user.setUserType(group.getGroupType());
            addGroupToUser(group, user);
            
            DataModelObjBaseWrapper userWrp  = new DataModelObjBaseWrapper(user);
            DefaultMutableTreeNode  userNode = new DefaultMutableTreeNode(userWrp);
            
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.insertNodeInto(userNode, grpNode, grpNode.getChildCount());
            
            tree.setSelectionPath(new TreePath(userNode.getPath()));
        }
    }
    
    /**
     * @param grpNode
     * @param userArray
     */
    public void addExistingUser(final DefaultMutableTreeNode grpNode, 
                                final SpecifyUser[] userArray) 
    {
        if (userArray.length == 0 || grpNode == null || 
                !(grpNode.getUserObject() instanceof DataModelObjBaseWrapper))
        {
            return; // Nothing is selected or object type isn't relevant 
        }

        DataModelObjBaseWrapper parentWrp  = (DataModelObjBaseWrapper) (grpNode.getUserObject());
        if (!parentWrp.isGroup())
        {
            return; // selection isn't a suitable parent for a group
        }
        
        SpPrincipal group = (SpPrincipal) parentWrp.getDataObj();
        addGroupToUser(group, userArray);
        
        DefaultMutableTreeNode lastUserNode = addUsersToTree(grpNode, userArray);
        
        tree.setSelectionPath(new TreePath(lastUserNode.getPath()));
    }
    
    private DefaultMutableTreeNode addUsersToTree(final  DefaultMutableTreeNode grpNode, 
                                                  final SpecifyUser[] userArray)
    {
        DefaultMutableTreeNode lastUserNode = null;
        for (SpecifyUser user : userArray) 
        {
            DataModelObjBaseWrapper userWrp  = new DataModelObjBaseWrapper(user);
            DefaultMutableTreeNode  userNode = new DefaultMutableTreeNode(userWrp);

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.insertNodeInto(userNode, grpNode, grpNode.getChildCount());
            
            lastUserNode = userNode;
        }
        return lastUserNode;
    }
    
    /**
     * @param parentNode
     */
    public void addNewGroup(final DefaultMutableTreeNode parentNode) 
    {
        if (parentNode == null || !(parentNode.getUserObject() instanceof DataModelObjBaseWrapper))
        {
            return; // Nothing is selected or object type isn't relevant    
        }

        DataModelObjBaseWrapper parentWrp  = (DataModelObjBaseWrapper) (parentNode.getUserObject());
        if (!parentWrp.isInstitution() && !parentWrp.isDiscipline() && !parentWrp.isCollection())
        {
            return; // selection isn't a suitable parent for a group
        }
        
        UserGroupScope scope = (UserGroupScope) parentWrp.getDataObj();
        SpPrincipal group = new SpPrincipal();
        group.initialize();
        group.setGroupSubClass(GroupPrincipal.class.getCanonicalName());
        group.setScope(scope);
        group.setName("New Group");
        save(group);
        
        DataModelObjBaseWrapper grpWrp  = new DataModelObjBaseWrapper(group);
        DefaultMutableTreeNode  grpNode = new DefaultMutableTreeNode(grpWrp);
        
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.insertNodeInto(grpNode, parentNode, parentNode.getChildCount());
        
        tree.setSelectionPath(new TreePath(grpNode.getPath()));
    }
    
    /**
     * @param discNode
     */
    public void addNewCollection(final DefaultMutableTreeNode discNode) 
    {
        if (discNode == null || !(discNode.getUserObject() instanceof DataModelObjBaseWrapper))
        {
            return;// Nothing is selected or object type isn't relevant    
        }

        DataModelObjBaseWrapper discWrp  = (DataModelObjBaseWrapper) (discNode.getUserObject());
        if (!discWrp.isDiscipline())
        {
            return; // selection isn't a discipline
        }
        
        Discipline discipline = (Discipline) discWrp.getDataObj();
        Collection collection = new Collection();
        collection.initialize();
        collection.setDiscipline(discipline);
        collection.setCollectionName("New Collection");
        save(collection);
        
        DataModelObjBaseWrapper collWrp  = new DataModelObjBaseWrapper(collection);
        DefaultMutableTreeNode  collNode = new DefaultMutableTreeNode(collWrp);
        
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.insertNodeInto(collNode, discNode, discNode.getChildCount());
        
        tree.setSelectionPath(new TreePath(collNode.getPath()));
    }
    
    /**
     * @param instNode
     */
    public void addNewDiscipline(final DefaultMutableTreeNode instNode) 
    {
        if (instNode == null || !(instNode.getUserObject() instanceof DataModelObjBaseWrapper))
        {
            return;// Nothing is selected or object type isn't relevant    
        }

        DataModelObjBaseWrapper instWrp  = (DataModelObjBaseWrapper) (instNode.getUserObject());
        if (!instWrp.isInstitution())
        {
            return; // selection isn't an institution
        }
        
        Institution institution = (Institution) instWrp.getDataObj();
        Division    division    = new Division();
        Discipline  discipline  = new Discipline();
        
        division.initialize();
        discipline.initialize();
        
        division.setInstitution(institution);
        discipline.setDivision(division);
        
        division.setName("Anonymous Division");
        discipline.setName("New Discipline");
        
        save(new Object[] { division, discipline });
        
        // The commented lines below insert a division into the tree with the discipline
        // It's there for reference only
        
        //DataModelObjBaseWrapper divWrp  = new DataModelObjBaseWrapper(division);
        DataModelObjBaseWrapper discWrp = new DataModelObjBaseWrapper(discipline);
        
        //DefaultMutableTreeNode divNode  = new DefaultMutableTreeNode(divWrp);
        DefaultMutableTreeNode discNode = new DefaultMutableTreeNode(discWrp);
        
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        //model.insertNodeInto(divNode,  instNode, instNode.getChildCount());
        //model.insertNodeInto(discNode, divNode,  divNode.getChildCount());
        model.insertNodeInto(discNode, instNode,  instNode.getChildCount());
        
        tree.setSelectionPath(new TreePath(discNode.getPath()));
    }
    
    /**
     * @param node
     */
    public void deleteItem(@SuppressWarnings("unused")DefaultMutableTreeNode node) 
    {
        
    }
    
    /**
     * @param object
     */
    private final void save(final Object object) 
    {
        save(new Object[] {object});
    }
    
    /**
     * @param objectArray
     */
    private final void save(final Object[] objectArray) 
    {
        DataProviderSessionIFace session = null;
        try
        {
            session = DataProviderFactory.getInstance().createSession();
            session.beginTransaction();
            for (Object object : objectArray)
            {
                session.attach(object);
                session.saveOrUpdate(object);
            }
            session.commit();
            
        } catch (final Exception e1)
        {
            session.rollback();
            log.error("Exception caught: " + e1.toString());
            e1.printStackTrace();
            
        } finally
        {
            if (session != null)
            {
                session.close();
            }
        }
    }
    
    /**
     * @param group
     * @param user
     */
    private final void addGroupToUser(final SpPrincipal group, final SpecifyUser user)
    {
        addGroupToUser(group, new SpecifyUser[] { user });
    }
    
    /**
     * @param group
     * @param users
     */
    private final void addGroupToUser(final SpPrincipal group, final SpecifyUser[] users)
    {
        DataProviderSessionIFace session = null;
        try
        {
            session = DataProviderFactory.getInstance().createSession();
            session.beginTransaction();
            for (SpecifyUser user : users) 
            {
                if (user.getId() != null)
                {
                    session.attach(user);
                }
                
                session.attach(group);
                if (user.getSpPrincipals() == null)
                {
                    user.setSpPrincipals(new HashSet<SpPrincipal>());
                }
                
                SpPrincipal userPrincipal = DataBuilder.createUserPrincipal(user);
                user.addUserToSpPrincipalGroup(userPrincipal);

                user.getSpPrincipals().add(group);
                group.getSpecifyUsers().add(user);
                
                session.saveOrUpdate(user);
                session.saveOrUpdate(userPrincipal);
                session.saveOrUpdate(group);
            }
            session.commit();
            
        } catch (final Exception e1)
        {
            session.rollback();
            log.error("Exception caught: " + e1.toString());
            e1.printStackTrace();
            
        } finally
        {
            if (session != null)
            {
                session.close();
            }
        }
    }
}
