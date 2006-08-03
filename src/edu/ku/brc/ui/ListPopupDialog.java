/**
 * 
 */
package edu.ku.brc.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

/**
 *
 *
 * @author jstewart
 * @version %I% %G%
 */
public class ListPopupDialog extends JDialog
{
	protected ListPopupCallback callback;
	protected Vector<Object> options;
	
	protected JPanel messagePanel;
	protected JPanel cbPanel;
	protected JPanel buttonPanel;
	protected JComboBox optionList;
	protected DefaultComboBoxModel model;
	
	protected JButton okButton;
	protected JButton cancelButton;
	
	public ListPopupDialog(Frame owner,String message,List<Object> options,ListPopupCallback popupCallback)
	{
		super(owner);
		this.setLayout(new BorderLayout());
		this.callback = popupCallback;
		this.options = new Vector<Object>(options);
		
		model = new DefaultComboBoxModel(this.options);
		optionList = new JComboBox(model);
		cbPanel = new JPanel();
		cbPanel.add(optionList);
		add(cbPanel,BorderLayout.CENTER);
		
		add(new JLabel(message),BorderLayout.NORTH);
		
		buttonPanel = new JPanel(new FlowLayout());
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
				callback.cancelled();
			}
		});
		okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
				callback.completed(optionList.getSelectedItem());
			}
		});
		
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		
		add(buttonPanel,BorderLayout.SOUTH);
		
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
				callback.cancelled();
			}
		});
	}
	
	public void setComboBoxCellRenderer(ListCellRenderer renderer)
	{
		optionList.setRenderer(renderer);
	}
	
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		okButton.requestFocusInWindow();
		okButton.requestFocus();
	}
	
	public void addOption(Object option)
	{
		options.add(option);
	}
	
	public void removeOption(Object option)
	{
		options.remove(option);
	}
	
	public interface ListPopupCallback
	{
		public void completed(Object userSelection);
		public void cancelled();
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args)
	{
		ListPopupCallback cb = new ListPopupCallback()
		{
			public void cancelled()
			{
				System.out.println("User cancelled");
			}
			public void completed(Object userSelection)
			{
				System.out.println("User selected " + userSelection);
			}
		};
		Vector options = new Vector();
		options.add("Hello");
		options.add(new Object());
		options.add(new WindowAdapter(){});
		ListPopupDialog d = new ListPopupDialog(null,"Make a selection",options,cb);
		d.setSize(300,300);
		d.setVisible(true);
	}
}
