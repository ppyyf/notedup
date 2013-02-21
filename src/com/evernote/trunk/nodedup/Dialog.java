package com.evernote.trunk.nodedup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

public class Dialog extends JWindow implements ActionListener   {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JFrame parent;
	    public Dialog(JFrame parent, String title, String message)  {
	      super(parent);

	      JPanel messagePane = new JPanel();
	      messagePane.add(new JLabel(message));        
	      getContentPane().add(messagePane);

	      JPanel buttonPane = new JPanel();
	      JButton button = new JButton(Messages.getString("Dialog.warning.ok"));  //$NON-NLS-1$
	      buttonPane.add(button);
	      button.addActionListener(this);
	      getContentPane().add(buttonPane, BorderLayout.SOUTH);
	      
	      pack();
	      if(parent != null) {
		        Dimension parentSize = parent.getSize();
		        Point p = parent.getLocation();
		        setLocation(p.x+parentSize.width/2-this.getSize().width/2,p.y+parentSize.height/2-this.getSize().height/2);
		        this.parent = parent;
		        parent.setEnabled(false);
		        parent.setFocusable(false);
	      }	
	      setAlwaysOnTop(true);
	      setVisible(true);
	    }

	    @Override
		public void actionPerformed(ActionEvent e)  {
	    	if (parent !=null){
	    		parent.setFocusable(true);
	    		parent.setEnabled(true);
	    	}
	    	setVisible(false);
	    	dispose();
	    }
	  }
