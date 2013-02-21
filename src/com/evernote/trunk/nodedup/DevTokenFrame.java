package com.evernote.trunk.nodedup;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class DevTokenFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private final JFrame parent;
	private final JFrame self;
	private final String devTokenURL;
	private JTextField textField;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DevTokenFrame frame = new DevTokenFrame(null, "www.evernote.com", 0); //$NON-NLS-1$
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public DevTokenFrame(final JFrame parent, final String serviceHost, final int type) {
		this.parent = parent;
		this.devTokenURL = "https://"+serviceHost+"/api/DeveloperToken.action"; //$NON-NLS-1$ //$NON-NLS-2$
		this.self = this;
		if (parent != null){
			parent.setEnabled(false);
			parent.setFocusable(false);
		}
		setBounds(100, 100, 532, 300);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		JTextPane txtpnUseWeb = new JTextPane();
		StringBuffer sb = new StringBuffer();
		sb.append(Messages.getString("DevTokenFrame.devtoken.title")); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append("\n1. "+Messages.getString("DevTokenFrame.devtoken.step1")+devTokenURL); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("\n2. "+Messages.getString("DevTokenFrame.devtoken.step2")); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("\n3. "+Messages.getString("DevTokenFrame.devtoken.step3")); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		txtpnUseWeb.setText(sb.toString());
		getContentPane().add(txtpnUseWeb, BorderLayout.CENTER);
		txtpnUseWeb.setEditable(false);
		txtpnUseWeb.setFocusable(false);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		textField = new JTextField();
		textField.setColumns(25);
		
		panel.add(textField);
		textField.setFocusable(true);
		
		JButton btnOK = new JButton(Messages.getString("DevTokenFrame.devtoken.ok")); //$NON-NLS-1$
		btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!textField.getText().startsWith("S=s")){ //$NON-NLS-1$
					new Dialog(self,Messages.getString("DevTokenFrame.devtoken.dialog.title"),Messages.getString("DevTokenFrame.devtoken.invalid")); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					dispose();
					if (parent !=null){
						((MainUI)parent).setDevToken(textField.getText(), type);
					}
				}
			}
		});
		panel.add(btnOK);
		
		JButton btnCancel = new JButton(Messages.getString("DevTokenFrame.devtoken.cancel")); //$NON-NLS-1$
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
				if (type == 1){
					((MainUI)parent).setDevToken(textField.getText(), 2);
				}
			}
		});
		panel.add(btnCancel);
		
		JButton btnOpenURL = new JButton(Messages.getString("DevTokenFrame.devtoken.openurl")); //$NON-NLS-1$
		btnOpenURL.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					Desktop.getDesktop().browse(java.net.URI.create(devTokenURL));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		panel.add(btnOpenURL);
		pack();
		if (parent != null){
			Dimension parentSize = parent.getSize();
			Point p = parent.getLocation();
			setLocation(p.x+parentSize.width/2-this.getSize().width/2,p.y+parentSize.height/2-this.getSize().height/2);
		}
		this.setVisible(true);
	}
	
	public void dispose(){
		super.dispose();
		if (parent != null){
			parent.setEnabled(true);
			parent.setFocusable(true);
		}
	}

}
