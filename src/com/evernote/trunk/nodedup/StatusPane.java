package com.evernote.trunk.nodedup;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

public class StatusPane extends JWindow  {

	private static final long serialVersionUID = 1L;
	private JFrame parent;
	private JLabel label;
	public StatusPane(JFrame parent, String message)  {
		super(parent);

		JPanel messagePane = new JPanel();
		label = new JLabel(message);
		messagePane.add(label);        
		getContentPane().add(messagePane);

		setSize(300,40);
		if(parent != null) {
			Dimension parentSize = parent.getSize();
			Point p = parent.getLocation();
			setLocation(p.x+parentSize.width/2-this.getSize().width/2,p.y+parentSize.height/2-this.getSize().height/2);
			this.parent = parent;
			parent.setEnabled(false);
		}	
		setBackground(new Color(224,224,224));
		setVisible(true);
	}

	public void done()  {
		if (parent !=null){
			parent.setEnabled(true);
		}
		setVisible(false);
		dispose();
	}
	public void setMessage(String m){
		label.setText(m);
	}
}
