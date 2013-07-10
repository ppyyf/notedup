package com.evernote.trunk.nodedup;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.FlowLayout;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;

public class NewVersionChecker extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private final long buildid;
	private static final String ROOT_ELEMENT="versions"; //$NON-NLS-1$
	private long latest = 0;
	private List<String> changes = new LinkedList<String>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new NewVersionChecker(2012111600);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public NewVersionChecker(long buildid) {
		this.buildid = buildid;
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(Constants.VERSIONS_XML_URL+"?"+(new Date()).getTime()); //$NON-NLS-1$
			doc.getDocumentElement().normalize();
			if (ROOT_ELEMENT.equals(doc.getDocumentElement().getNodeName())){
				NodeList nList = doc.getElementsByTagName("version"); //$NON-NLS-1$
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						long id = Long.parseLong(eElement.getAttribute("id")); //$NON-NLS-1$
						if (id > this.buildid){
							if (this.latest < id){
								this.latest = id;
							}
							NodeList changes = ((Element)eElement.getElementsByTagName("changes").item(0)).getElementsByTagName("change"); //$NON-NLS-1$ //$NON-NLS-2$
							for (int i=0;i<changes.getLength();i++){
								Node cNode = changes.item(i).getFirstChild();
								this.changes.add(cNode.getNodeValue());
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (latest > 0){
			setBounds(250, 250, 450, 300);
			contentPane = new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			setContentPane(contentPane);
			contentPane.setLayout(new BorderLayout(0, 0));

			JPanel panel = new JPanel();
			FlowLayout flowLayout = (FlowLayout) panel.getLayout();
			flowLayout.setAlignment(FlowLayout.LEFT);
			contentPane.add(panel, BorderLayout.NORTH);

			JScrollPane scrollPane = new JScrollPane();
			contentPane.add(scrollPane, BorderLayout.CENTER);

			JEditorPane editorPane = new JEditorPane();
			editorPane.setContentType("text/html"); //$NON-NLS-1$
			editorPane.setEditable(false);
			editorPane.addHyperlinkListener(new HyperlinkListener() {  
				public void hyperlinkUpdate(HyperlinkEvent hle) {  
					if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {  
						try {
							Desktop.getDesktop().browse(new URI(hle.getURL().toString()));
						} catch (IOException e) {
						} catch (URISyntaxException e) {
						}
					}
				}
			});
			scrollPane.setViewportView(editorPane);
			String textHtml = "<H1>"+Messages.getString("NewVersionChecker.title")+"</H1>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			textHtml += Messages.getString("NewVersionChecker.current.version")+": "+this.buildid+"<br/>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			textHtml += Messages.getString("NewVersionChecker.latest.version")+": "+this.latest+"<br/>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//textHtml += "<H3>"+Messages.getString("NewVersionChecker.downloads")+"</H3>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//textHtml += "<li><a href=\"http://vdisk.weibo.com/s/hIoBo\">"+"Windows"+"</li>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//textHtml += "<li><a href=\"http://vdisk.weibo.com/s/hIjMU\">"+"Mac"+"</li>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			textHtml += "<H3>"+Messages.getString("NewVersionChecker.whats.new")+"</H3>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			for (String change: this.changes){
				textHtml += "<li>"+change+"</li>"; //$NON-NLS-1$ //$NON-NLS-2$
			}


			editorPane.setText(textHtml);
			this.pack();
			this.setVisible(true);
			this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			this.setTitle(Messages.getString("NewVersionChecker.title")); //$NON-NLS-1$
			this.setAlwaysOnTop(true);
		} else {
			this.dispose();
		}
	}

}
