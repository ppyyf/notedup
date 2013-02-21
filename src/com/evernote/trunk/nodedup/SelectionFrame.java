package com.evernote.trunk.nodedup;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Tag;

public class SelectionFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private final JFrame parent;
	private final Account source;
	private List<JCheckBox> notebookCheckBoxes;
	private List<JCheckBox> tagCheckBoxes;
	public static final String noTagsCheckBoxText = Messages.getString("SelectionFrame.no.user.tags"); //$NON-NLS-1$
	
	public SelectionFrame(final JFrame parent, Account source, List<String> selectedNotebooks, List<String> selectedTags) {
		super();
		this.parent = parent;
		this.source = source;
		
		if (this.source != null){
			List<Notebook> notebooks = source.getNotebooks();
			notebookCheckBoxes = new LinkedList<JCheckBox>();
			for (int i = 0;i<notebooks.size();i++){
				JCheckBox checkBox = new JCheckBox(notebooks.get(i).getName(), true);
				if (selectedNotebooks!=null && !selectedNotebooks.contains(notebooks.get(i).getName())){
					checkBox.setSelected(false);
				}
				notebookCheckBoxes.add(checkBox);
			}
			
			List<Tag> tags = source.getTags();
			tagCheckBoxes = new LinkedList<JCheckBox>();
			JCheckBox checkBoxNoTags = new JCheckBox(noTagsCheckBoxText, true);
			if(selectedTags!=null&&!selectedTags.contains(noTagsCheckBoxText)){
				checkBoxNoTags.setSelected(false);
			}
			tagCheckBoxes.add(checkBoxNoTags);
			for (int i = 0;i<tags.size();i++){
				if (tags.get(i).getName().startsWith("__ND_")){ //$NON-NLS-1$
					continue;
				}
				JCheckBox checkBox = new JCheckBox(tags.get(i).getName(), true);
				if(selectedTags!=null&&!selectedTags.contains(tags.get(i).getName())){
					checkBox.setSelected(false);
				}
				tagCheckBoxes.add(checkBox);
			}
		}
		
		if (parent != null){
			parent.setEnabled(false);
			parent.setFocusable(false);
		}
		
		this.setSize(800,600);
		this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width/2-getSize().width/2, Toolkit.getDefaultToolkit().getScreenSize().height/2-getSize().height/2);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setBackground(SystemColor.windowBorder);
		getContentPane().add(splitPane, BorderLayout.CENTER);
		
		JPanel panel_left = new JPanel();
		panel_left.setLayout(new BorderLayout(0, 0));
		splitPane.setLeftComponent(panel_left);
				
		JScrollPane scrollPaneNotebook = new JScrollPane();
		panel_left.add(scrollPaneNotebook);
		
		JPanel notebookCheckBoxesPanel = new JPanel();
		scrollPaneNotebook.setViewportView(notebookCheckBoxesPanel);
		notebookCheckBoxesPanel.setLayout(new BoxLayout(notebookCheckBoxesPanel, BoxLayout.Y_AXIS));
		for (JCheckBox checkBox: this.notebookCheckBoxes){
			notebookCheckBoxesPanel.add(checkBox);
		}
		
		JPanel notebooksTitlePanel = new JPanel();
		panel_left.add(notebooksTitlePanel, BorderLayout.NORTH);
		notebooksTitlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel notebooksLabel = new JLabel(Messages.getString("SelectionFrame.label.notebooks")); //$NON-NLS-1$
		notebooksTitlePanel.add(notebooksLabel);
		notebooksLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		JButton btnNotebooksAll = new JButton(Messages.getString("SelectionFrame.button.all")); //$NON-NLS-1$
		btnNotebooksAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (JCheckBox checkBox : notebookCheckBoxes){
					checkBox.setSelected(true);
				}
			}
		});
		notebooksTitlePanel.add(btnNotebooksAll);
		
		JButton btnNotebooksNone = new JButton(Messages.getString("SelectionFrame.button.none")); //$NON-NLS-1$
		btnNotebooksNone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (JCheckBox checkBox : notebookCheckBoxes){
					checkBox.setSelected(false);
				}
			}
		});
		notebooksTitlePanel.add(btnNotebooksNone);
		
		JPanel panel_right = new JPanel();
		panel_right.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPaneTag = new JScrollPane();
		panel_right.add(scrollPaneTag);
		
		JPanel tagCheckBoxesPanel = new JPanel();
		scrollPaneTag.setViewportView(tagCheckBoxesPanel);
		tagCheckBoxesPanel.setLayout(new BoxLayout(tagCheckBoxesPanel, BoxLayout.Y_AXIS));
		for (JCheckBox checkBox:this.tagCheckBoxes){
			tagCheckBoxesPanel.add(checkBox);
		}
	
		splitPane.setRightComponent(panel_right);
		
		JPanel tagsTitlePanel = new JPanel();
		panel_right.add(tagsTitlePanel, BorderLayout.NORTH);
		
		JLabel tagsLabel = new JLabel(Messages.getString("SelectionFrame.label.tags")); //$NON-NLS-1$
		tagsTitlePanel.add(tagsLabel);
		tagsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		JButton btnTagsAll = new JButton(Messages.getString("SelectionFrame.button.all")); //$NON-NLS-1$
		btnTagsAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (JCheckBox checkBox : tagCheckBoxes){
					checkBox.setSelected(true);
				}
			}
		});
		tagsTitlePanel.add(btnTagsAll);
		
		JButton btnTagsNone = new JButton(Messages.getString("SelectionFrame.button.none")); //$NON-NLS-1$
		btnTagsNone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (JCheckBox checkBox : tagCheckBoxes){
					checkBox.setSelected(false);
				}
			}
		});
		tagsTitlePanel.add(btnTagsNone);
		
		JPanel panel_bottom = new JPanel();
		getContentPane().add(panel_bottom, BorderLayout.SOUTH);
		
		JButton btnOkay = new JButton(Messages.getString("SelectionFrame.button.ok")); //$NON-NLS-1$
		btnOkay.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(parent!=null && parent.getClass().getName().equals("com.evernote.trunk.nodedup.MainUI")){ //$NON-NLS-1$

					List<String> selectedNotebooks = new LinkedList<String>();
					for (JCheckBox checkBox : notebookCheckBoxes){
						if (checkBox.isSelected()){
							selectedNotebooks.add(checkBox.getText());
						}
					}
					((MainUI)parent).setSelectedNotebooks(selectedNotebooks);
					
					List<String> selectedTags = new LinkedList<String>();
					for (JCheckBox checkBox : tagCheckBoxes){
						if (checkBox.isSelected()){
							selectedTags.add(checkBox.getText());
						}
					}
					((MainUI)parent).setSelectedTags(selectedTags);				
				}
				dispose();
			}
		});
		panel_bottom.add(btnOkay);
		
		JButton btnCancel = new JButton(Messages.getString("SelectionFrame.button.cancel")); //$NON-NLS-1$
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		panel_bottom.add(btnCancel);

		this.setAlwaysOnTop(true);
		this.setVisible(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		splitPane.setDividerLocation(0.5);
		
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (parent != null){
			parent.setEnabled(true);
			parent.setFocusable(true);
		}
	}

}
