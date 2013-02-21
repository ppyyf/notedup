package com.evernote.trunk.nodedup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import java.awt.Dimension;
import javax.swing.JCheckBox;

public class MainUI extends JFrame {

	private static final long BUILDID=__BUILDID__;
	private static final long serialVersionUID = 1L;
	private JTextField uname_source;
	private JTextField uname_target;
	
	private final String supported_account_types[] = {Messages.getString("MainUI.service.name.evernote"), Messages.getString("MainUI.service.name.yinxiang")}; //$NON-NLS-1$ //$NON-NLS-2$
	private final Integer supported_thread_numbers[] = {1,2,4,8}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	
	private Account accountSource;
	private Account accountTarget;
	
	private String serviceHostSource;
	private String serviceHostTarget;
	
	private AuthHelperSystemBrowser authHelperSource = null;
	private AuthHelperSystemBrowser authHelperTarget = null;
	
	private JTextArea textArea;
	
	private JComboBox uploadLimitMB;
	private JComboBox threadNum;
	
	private JButton login_source;
	private JButton login_target;
	
	JButton btnStart;
	private boolean stopFlag = false;
	
	private JFrame self;
	private StatusPane statusPane;
	
	private List<String> selectedNotebooks;
	private List<String> selectedTags;
	private JCheckBox chckbxUpdateNoteLinks;
	private JButton btnFixAccount;
	private JButton btnSelection;
	
	private String getServiceHost(String serviceType){
		if (serviceType!=null){
			if (serviceType.equals(supported_account_types[0])){
				return "www.evernote.com"; //$NON-NLS-1$
			} else if (serviceType.equals(supported_account_types[1])){
				return "app.yinxiang.com"; //$NON-NLS-1$
			}
		}
		return ""; //$NON-NLS-1$
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					MainUI window = new MainUI();
					window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		new NewVersionChecker(BUILDID);
	}

	/**
	 * Create the application.
	 */
	public MainUI() {
		initialize();
		this.self = this;
	}

	public MainUI(boolean initialize) {
		if (initialize){
			initialize();
		}
		this.self = this;
	}
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		this.setBounds(100, 100, 650, 600);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel panel_accounts = new JPanel();
		panel_accounts.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		this.getContentPane().add(panel_accounts, BorderLayout.NORTH);
		panel_accounts.setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel panel_source = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_source.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		panel_accounts.add(panel_source);
		
		JLabel label_source = new JLabel(Messages.getString("MainUI.label.source.account")); //$NON-NLS-1$
		label_source.setMaximumSize(new Dimension(120, 16));
		label_source.setPreferredSize(new Dimension(120, 16));
		label_source.setMinimumSize(new Dimension(120, 16));
		label_source.setHorizontalAlignment(SwingConstants.RIGHT);
		label_source.setBackground(Color.WHITE);
		panel_source.add(label_source);
		
		JComboBox acctype_source = new JComboBox();
		acctype_source.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				serviceHostSource = getServiceHost((String)((JComboBox)e.getSource()).getSelectedItem());
			}
		});
		acctype_source.setModel(new DefaultComboBoxModel(supported_account_types));
		serviceHostSource = getServiceHost((String) acctype_source.getSelectedItem());
		panel_source.add(acctype_source);
		
		login_source = new JButton(Messages.getString("MainUI.button.login")); //$NON-NLS-1$
		login_source.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				Thread t = new Thread(){
					@Override
					public void run(){
						setEnabled(false);
						statusPane = new StatusPane(self,Messages.getString("MainUI.status.authenticating")); //$NON-NLS-1$
						authHelperSource = new AuthHelperSystemBrowser(serviceHostSource);
						String oauthToken = authHelperSource.getOAuthToken();
						statusPane.setMessage(Messages.getString("MainUI.status.retrieving")); //$NON-NLS-1$
						if (oauthToken != null){
							accountSource = new Account(serviceHostSource,oauthToken);
							uname_source.setText(accountSource.getUsername());
						}
						statusPane.done();
						setEnabled(true);
					}
				};
				t.start();
			}
		});
		panel_source.add(login_source);
		
		uname_source = new JTextField();
		uname_source.setForeground(Color.GRAY);
		uname_source.setText(Messages.getString("MainUI.username.not.available")); //$NON-NLS-1$
		uname_source.setEditable(false);
		panel_source.add(uname_source);
		uname_source.setColumns(15);
		
		JPanel panel_target = new JPanel();
		FlowLayout flowLayout_3 = (FlowLayout) panel_target.getLayout();
		flowLayout_3.setAlignment(FlowLayout.LEFT);
		panel_accounts.add(panel_target);
		
		JLabel label_target = new JLabel(Messages.getString("MainUI.label.target.account")); //$NON-NLS-1$
		label_target.setPreferredSize(new Dimension(120, 16));
		label_target.setMinimumSize(new Dimension(120, 16));
		label_target.setMaximumSize(new Dimension(120, 16));
		label_target.setHorizontalAlignment(SwingConstants.RIGHT);
		panel_target.add(label_target);
		
		JComboBox acctype_target = new JComboBox();
		acctype_target.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				serviceHostTarget = getServiceHost((String)((JComboBox)e.getSource()).getSelectedItem());
			}
		});
		acctype_target.setModel(new DefaultComboBoxModel(supported_account_types));
		serviceHostTarget = getServiceHost((String) acctype_target.getSelectedItem());
		panel_target.add(acctype_target);
		
		login_target = new JButton(Messages.getString("MainUI.button.login")); //$NON-NLS-1$
		login_target.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Thread t = new Thread(){
					@Override
					public void run(){
						setEnabled(false);
						btnFixAccount.setEnabled(false);
						statusPane = new StatusPane(self,Messages.getString("MainUI.status.authenticating")); //$NON-NLS-1$
						authHelperTarget = new AuthHelperSystemBrowser(serviceHostTarget);
						String oauthToken = authHelperTarget.getOAuthToken();
						statusPane.setMessage(Messages.getString("MainUI.status.retrieving")); //$NON-NLS-1$
						if (oauthToken != null){
							accountTarget = new Account(serviceHostTarget,oauthToken);
							uname_target.setText(accountTarget.getUsername());
							if (accountTarget.getEvernoteHost().contains("yinxiang")){ //$NON-NLS-1$
								btnFixAccount.setEnabled(true);
							}
						}
						statusPane.done();
						setEnabled(true);
					}
				};
				t.start();
			}
		});
		panel_target.add(login_target);
		
		uname_target = new JTextField();
		uname_target.setForeground(Color.GRAY);
		uname_target.setText(Messages.getString("MainUI.username.not.available")); //$NON-NLS-1$
		uname_target.setEditable(false);
		panel_target.add(uname_target);
		uname_target.setColumns(15);
		
		btnFixAccount = new JButton(Messages.getString("MainUI.button.fixAccount")); //$NON-NLS-1$
		btnFixAccount.setToolTipText(Messages.getString("MainUI.btnFixAccount.toolTipText")); //$NON-NLS-1$
		btnFixAccount.setEnabled(false);
		btnFixAccount.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(accountTarget==null || !accountTarget.isInitialized()){
					new Dialog(self,Messages.getString("MainUI.dialog.error"),Messages.getString("MainUI.dialog.target")); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				} else {
					Thread t = new Thread(){
						@Override
						public void run(){
							btnFixAccount.setEnabled(false);
							login_source.setEnabled(false);
							login_target.setEnabled(false);
							btnStart.setEnabled(false);
							btnSelection.setEnabled(false);
							textArea.setText(Messages.getString("MainUI.status.fixingAccount")+" ... "); //$NON-NLS-1$ //$NON-NLS-2$
							int count = accountTarget.removeInvalidSharedLinks();
							textArea.append(Messages.getString("MainUI.status.done_fullstop")+" "+ count + " "+Messages.getString("MainUI.status.notesfixed")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							btnFixAccount.setEnabled(true);
							login_source.setEnabled(true);
							login_target.setEnabled(true);
							btnStart.setEnabled(true);
							btnSelection.setEnabled(true);
						}
					};
					t.start();
				}
			}
		});

		panel_target.add(btnFixAccount);
		
		JPanel panel_command = new JPanel();
		this.getContentPane().add(panel_command, BorderLayout.SOUTH);
		panel_command.setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panel.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		panel_command.add(panel, BorderLayout.WEST);
		
		JLabel lblUploadLimit = new JLabel(Messages.getString("MainUI.upload.limit.mb")); //$NON-NLS-1$
		panel.add(lblUploadLimit);
		
		uploadLimitMB = new JComboBox();
		panel.add(uploadLimitMB);
		uploadLimitMB.setEditable(true);
		uploadLimitMB.setModel(new DefaultComboBoxModel(new String[] {Messages.getString("MainUI.upload.limit.unlimited"), "10", "20", "30", "50", "100"})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		
		JLabel lblThreadNumber = new JLabel(Messages.getString("MainUI.thread.number")); //$NON-NLS-1$
		panel.add(lblThreadNumber);
		
		threadNum = new JComboBox();
		panel.add(threadNum);
		threadNum.setModel(new DefaultComboBoxModel(supported_thread_numbers));
		
		chckbxUpdateNoteLinks = new JCheckBox(Messages.getString("MainUI.chckbx.updateNoteLinks")); //$NON-NLS-1$
		chckbxUpdateNoteLinks.setSelected(true);
		panel.add(chckbxUpdateNoteLinks);
		
		JPanel panel_1 = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) panel_1.getLayout();
		flowLayout_2.setAlignment(FlowLayout.RIGHT);
		panel_command.add(panel_1, BorderLayout.EAST);
		
		btnStart = new JButton(Messages.getString("MainUI.button.copy")); //$NON-NLS-1$
		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (Messages.getString("MainUI.button.stop").equals(btnStart.getText())){ //$NON-NLS-1$
					stopFlag = true;
					btnStart.setEnabled(false);
					btnStart.setText(Messages.getString("MainUI.button.stopping")); //$NON-NLS-1$
					return;
				}
				
				// check source 
				if (accountSource == null){
					new Dialog(self,Messages.getString("MainUI.dialog.error"),Messages.getString("MainUI.dialog.source")); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				// check target
				if (accountTarget == null){
					new Dialog(self,Messages.getString("MainUI.dialog.error"),Messages.getString("MainUI.dialog.target")); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				// get upload limit
					String stringUploadLimit = (String)uploadLimitMB.getSelectedItem();
					long longUploadLimit;
					if (Messages.getString("MainUI.upload.limit.unlimited").equals(stringUploadLimit)){ //$NON-NLS-1$
						longUploadLimit = 0;
					} else {
						try {
							longUploadLimit = Long.parseLong(stringUploadLimit);
						} catch (Exception e){
							new Dialog(self,Messages.getString("MainUI.dialog.error"),Messages.getString("MainUI.dialog.upload.limit")); //$NON-NLS-1$ //$NON-NLS-2$
							return;
						}
					}
				final long uploadLimit =  longUploadLimit;
				final int thread = (Integer)threadNum.getSelectedItem();
				// get thread number
				// copy notes.
				Thread t = new Thread(){
					@Override
					public void run(){
						btnStart.setText(Messages.getString("MainUI.button.stop")); //$NON-NLS-1$
						login_source.setEnabled(false);
						login_target.setEnabled(false);
						stopFlag = false;
						copyNotes(textArea,accountSource,accountTarget,uploadLimit,thread);
						login_source.setEnabled(true);
						login_target.setEnabled(true);
						btnStart.setText(Messages.getString("MainUI.button.copy")); //$NON-NLS-1$
						btnStart.setEnabled(true);
					}
				};
				t.start();
			}
		});
		
		btnSelection = new JButton(Messages.getString("MainUI.button.selection")); //$NON-NLS-1$
		btnSelection.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(accountSource==null || !accountSource.isInitialized()){
					new Dialog(self,Messages.getString("MainUI.dialog.error"),Messages.getString("MainUI.dialog.source")); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				} else {
					new SelectionFrame(self, accountSource, selectedNotebooks, selectedTags);
				}
			}
		});
		panel_1.add(btnSelection);
		panel_1.add(btnStart);
		
		JPanel panel_status = new JPanel();
		this.getContentPane().add(panel_status);
		panel_status.setLayout(new BorderLayout(0, 0));
		
		textArea = new JTextArea();
		textArea.setPreferredSize(new Dimension(600,500));
		textArea.setEditable(false);
		panel_status.add(textArea, BorderLayout.CENTER);
		textArea.setText(Messages.getString("NewVersionChecker.current.version")+": "+BUILDID); //$NON-NLS-1$ //$NON-NLS-2$
		this.pack();
	}
	
	public void copyNotes(JTextArea textArea, Account source, Account target, long uploadLimit, int threads){
		if (source.isInitialized() && target.isInitialized()){
			if(source.getEvernoteHost().equals("app.yinxiang.com") && target.getEvernoteHost().equals("www.evernote.com")){ //$NON-NLS-1$ //$NON-NLS-2$
				System.out.println("WARNING: Copying from yinxiang to evernote is buggy due to network issues. :("); //$NON-NLS-1$
				System.out.println("WARNING: Disabling temporarily in case it is messed up."); //$NON-NLS-1$
				System.out.println("WARNING: Remove this check at your own risk."); //$NON-NLS-1$
				return;
			}
			source.updateTagHashes();
			uploadLimit *= 1024*1024;
			long targetUploadLimit = target.getUploadLimit();
			if (uploadLimit == 0 || uploadLimit > targetUploadLimit){
				uploadLimit = targetUploadLimit;
			}
			if (threads < 1){
				threads = 1;
			}
	
			if (textArea == null){
				textArea = new JTextArea();
			}
			
			final Date startTime = new Date();
			final String srcTag = "__ND_to_"+target.getUsername()+"-"+target.getEvernoteHost(); //$NON-NLS-1$ //$NON-NLS-2$
			final String dstTag = "__ND_from_"+source.getUsername()+"-" //$NON-NLS-1$ //$NON-NLS-2$
					+source.getEvernoteHost()+"-"+String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", startTime); //$NON-NLS-1$ //$NON-NLS-2$
			
			final String summaryFormat
					="  "+Messages.getString("MainUI.summary.note.copied")+": %4s\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+"  "+Messages.getString("MainUI.summary.note.failed")+"   : %4s\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+"  "+Messages.getString("MainUI.summary.note.skipped")+": %4s\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+"  "+Messages.getString("MainUI.summary.note.not.copied")+": %4s\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+"  "+Messages.getString("MainUI.summary.uploaded.bytes")+": %s\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+"  "+Messages.getString("MainUI.summary.time")+": %s\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					
			final String splitLine = "=========================================================\n"; //$NON-NLS-1$
			final String head = 
					  Messages.getString("MainUI.head.copying.from")+":\n"  //$NON-NLS-1$//$NON-NLS-2$
					+ "\t"+source.getUsername()+" - "+source.getEvernoteHost()+"\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "    "+Messages.getString("MainUI.head.to")+":\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "\t"+target.getUsername()+" - "+target.getEvernoteHost()+"\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "\n" //$NON-NLS-1$
					+ Messages.getString("MainUI.head.thread.number")+": "+ threads+ "\t  "+Messages.getString("MainUI.head.upload.limit.bytes")+": "+uploadLimit+"\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			String taskState = ""; //$NON-NLS-1$
			String taskSummary = ""; //$NON-NLS-1$
			textArea.setText(head+splitLine);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
			final CopyState cs = new CopyState(uploadLimit);
			BlockingQueue<NoteCopyTask> noteQueue = new LinkedBlockingQueue<NoteCopyTask>(threads*4);
			NoteCopyMaster master = new NoteCopyMaster(source, target, srcTag, dstTag,noteQueue,cs,this.selectedNotebooks, this.selectedTags);
			master.start();
			NoteCopySlave[] slaves = new NoteCopySlave[threads];
			for (int i = 0; i<slaves.length ; i++){
				slaves[i] = new NoteCopySlave(source, target, srcTag, dstTag,noteQueue,cs);
				slaves[i].start();
			}
			
			// update status in textArea while master/slave threads are running
			boolean allThreadsDone = true;
			
			while (true){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				
				// check whether stop button was pressed by user.
				if (this.stopFlag){
					this.stopFlag = false;
					cs.stop();
					if (master !=null){
						master.interrupt();
					}
					for (NoteCopySlave slave:slaves){
						if (slave !=null){
							slave.interrupt();
						}
					}					
				}
				
				// collect state of each slave thread
				allThreadsDone = true;
				if (master == null){
				} else if (master.isAlive()){
					allThreadsDone = false;
				} else {
					try {
						master.join();
						master = null;
					} catch (InterruptedException e) {
					}
				}
				taskState = ""; //$NON-NLS-1$
				
				if (master != null){
					taskState += "  * "+Messages.getString("MainUI.status.masterthread")+": "+master.getCurrentState()+"\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				} else {
					taskState += "  * "+Messages.getString("MainUI.status.masterthread")+": ["+Messages.getString("MainUI.status.done")+"]\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				}
				
				for (int i = 0 ; i < slaves.length; i++){
					if (slaves[i] == null){
						taskState += "  * "+Messages.getString("MainUI.status.thread")+" "+i+": ["+Messages.getString("MainUI.status.done")+"]\n";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					} else if (slaves[i].isAlive()){
						taskState += "  * "+Messages.getString("MainUI.status.thread")+" "+i+": "+slaves[i].getCurrentState()+"\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						allThreadsDone = false;
					} else if (!slaves[i].isDone()){
						NoteCopyTask task = slaves[i].getLastTask();
						try {
							slaves[i].join();
							slaves[i] = null;
						} catch (InterruptedException e) {
						}
						System.gc();
						taskState += "  * "+Messages.getString("MainUI.status.thread")+" "+i+": ["+Messages.getString("MainUI.status.restarting")+"]\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
						slaves[i] = new NoteCopySlave(source, target, srcTag, dstTag,noteQueue,cs);
						slaves[i].setLastTask(task);
						slaves[i].start();
						allThreadsDone = false;
					} else {
						taskState += "  * "+Messages.getString("MainUI.status.thread")+" "+i+": "+slaves[i].getCurrentState()+"\n";						 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						try {
							slaves[i].join();
							slaves[i] = null;
						} catch (InterruptedException e) {
						}
					}
				}
				Date currentTime = new Date();
				long seconds = (currentTime.getTime()-startTime.getTime())/1000;
				String timeUsed = String.format("%02d:%02d:%02d", seconds/3600, (seconds%3600)/60, seconds%60); //$NON-NLS-1$
				taskSummary = String.format(summaryFormat, cs.getCopied(),cs.getFailed(),cs.getSkipped(),cs.getNotcopied(),cs.getUploadedSize(),timeUsed);
				if (allThreadsDone){
					taskSummary += splitLine+Messages.getString("MainUI.status.all.done"); //$NON-NLS-1$
				}
				textArea.setText(head+splitLine+taskState+splitLine+taskSummary);
				if (allThreadsDone){
					if (this.chckbxUpdateNoteLinks.isSelected()){
						taskSummary += "\n"+splitLine+Messages.getString("MainUI.status.updatingNoteLinks")+" ... "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						textArea.setText(head+splitLine+taskState+splitLine+taskSummary);
						int count = target.updateNoteLinks(source);
						taskSummary += Messages.getString("MainUI.status.done_fullstop")+" "+count+" "+Messages.getString("MainUI.status.notesUpdated"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						textArea.setText(head+splitLine+taskState+splitLine+taskSummary);
					}
					break;
				}
			}
		}
	}

	public void setSelectedNotebooks(List<String> selectedNotebooks) {
		this.selectedNotebooks = selectedNotebooks;
	}

	public void setSelectedTags(List<String> selectedTags) {
		this.selectedTags = selectedTags;
	}
}
