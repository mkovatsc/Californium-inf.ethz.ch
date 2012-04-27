package ch.ethz.inf.vs.californium.examples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import ch.ethz.inf.vs.californium.coap.*;

/**
 * A CoAP Client to communicate with other CoAP resources.
 * 
 * @author Martin Lanter
 */
public class GUIClient extends JPanel {

	private static final long serialVersionUID = -8656652459991661071L;
	
	private static final String DEFAULT_URI = "coap://localhost:5683";
	private static final String TESTSERVER_URI = "coap://vs0.inf.ethz.ch:5683";
	private static final String COAP_PROTOCOL = "coap://";
	
	private JComboBox cboTarget;
	
	private JTextArea txaPayload;
	private JTextArea txaResponse;
	
	private JPanel pnlResponse;
	private TitledBorder responseBorder;
	
	private DefaultMutableTreeNode dmtRes;
	private DefaultTreeModel dtmRes;
	private JTree treRes;
	
	public GUIClient() {
		JButton btnGet = new JButton("GET");
		JButton btnPos = new JButton("POST");
		JButton btnPut = new JButton("PUT");
		JButton btnDel = new JButton("DELETE");
		JButton btnDisc = new JButton("Discovery");
		
		btnGet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performRequest(new GETRequest());
			}
		});
		
		btnPos.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performRequest(new POSTRequest());
			}
		});
		
		btnPut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performRequest(new PUTRequest());
			}
		});
		
		btnDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performRequest(new DELETERequest());
			}
		});
		
		btnDisc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				discover();
			}
		});
		
		cboTarget = new JComboBox();
		cboTarget.setEditable(true);
		cboTarget.setMinimumSize(cboTarget.getPreferredSize());
		cboTarget.addItem(DEFAULT_URI);
		cboTarget.addItem(TESTSERVER_URI);
		cboTarget.setSelectedIndex(0);
		
		txaPayload = new JTextArea("", 8, 50);
		txaResponse = new JTextArea("", 8, 50);
		txaResponse.setEditable(false);
		
		JPanel pnlDisc = new JPanel(new BorderLayout());
		pnlDisc.add(cboTarget, BorderLayout.CENTER);
		pnlDisc.add(btnDisc, BorderLayout.EAST);
		
		JPanel pnlTarget = new JPanel(new BorderLayout());
		pnlTarget.setBorder(new TitledBorder("Target"));
		pnlTarget.add(pnlDisc, BorderLayout.NORTH);
		pnlTarget.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnlTarget.getPreferredSize().height));
		
		JPanel pnlButtons = new JPanel(new GridLayout(1, 4, 10, 10));
		pnlButtons.setBorder(new EmptyBorder(10,10,10,10));
		pnlButtons.add(btnGet);
		pnlButtons.add(btnPos);
		pnlButtons.add(btnPut);
		pnlButtons.add(btnDel);
		
		JPanel pnlRequest = new JPanel(new BorderLayout());
		pnlRequest.setBorder(new TitledBorder("Request"));
		pnlRequest.add(new JScrollPane(txaPayload), BorderLayout.CENTER);
		pnlRequest.add(pnlButtons, BorderLayout.SOUTH);
		
		pnlResponse = new JPanel(new BorderLayout());
		responseBorder = new TitledBorder("Response");
		pnlResponse.setBorder(responseBorder);
		pnlResponse.add(new JScrollPane(txaResponse));
		
		JPanel panelC = new JPanel();
		panelC.setLayout(new BoxLayout(panelC, BoxLayout.Y_AXIS));
		
		panelC.add(pnlTarget);
		panelC.add(pnlRequest);
		
		JSplitPane splReqRes = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splReqRes.setContinuousLayout(true);
		splReqRes.setResizeWeight(.5f);
		splReqRes.setTopComponent(panelC);
		splReqRes.setBottomComponent(pnlResponse);

		dmtRes = new DefaultMutableTreeNode("Resources");
		dtmRes = new DefaultTreeModel(dmtRes);
		treRes = new JTree(dtmRes);

		JScrollPane scrRes = new JScrollPane(treRes);
		scrRes.setPreferredSize(new Dimension(200,scrRes.getPreferredSize().height));
		
		JPanel panelE = new JPanel(new BorderLayout());
		panelE.setBorder(new TitledBorder("Resources"));
		panelE.add(scrRes,BorderLayout.CENTER);
		
		setLayout(new BorderLayout());
		
		JSplitPane splCE = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splCE.setContinuousLayout(true);
		splCE.setResizeWeight(.5f);
		splCE.setLeftComponent(panelE);
		splCE.setRightComponent(splReqRes);
		add(splCE);
		
		treRes.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath tp= e.getNewLeadSelectionPath();
				if (tp!=null) {
					Object[] nodes = tp.getPath();
					StringBuffer sb = new StringBuffer(COAP_PROTOCOL+getHost());
					for (int i=1;i<nodes.length;i++) // nodes[0] is Resource and not necessary
						sb.append("/"+nodes[i].toString());
					cboTarget.setSelectedItem(sb.toString());
				}
			}
		});

		discover();
	}
	
	private void discover() {
		dmtRes.removeAllChildren();
		dtmRes.reload();
		
		Request request = new GETRequest();
		request.setURI(COAP_PROTOCOL+getHost()+"/.well-known/core");
		request.registerResponseHandler(new ResponseHandler() {
			public void handleResponse(Response response) {
				String text = response.getPayloadString();
				Scanner scanner = new Scanner(text);
				Pattern pattern = Pattern.compile("<");
				scanner.useDelimiter(pattern);
				
				ArrayList<String> ress1 = new ArrayList<String>();
				ArrayList<String> ress2 = new ArrayList<String>();
				while(scanner.hasNext()) {
					String part = scanner.next();
					String res = part.split(">")[0];
					ress1.add(COAP_PROTOCOL+getHost()+res);
					ress2.add(res);
				}
				cboTarget.setModel(new DefaultComboBoxModel(ress1.toArray(new String[ress1.size()])));
				populateTree(ress2);
			}
		});
		execute(request);
	}
	
	private void populateTree(List<String> ress) {
		Node root = new Node("Resource");
		for (String res:ress) {
			String[] parts = res.split("/");
			Node cur = root;
			for (int i=1;i<parts.length;i++) {
				Node n = cur.get(parts[i]);
				if (n==null)
					cur.children.add(n = new Node(parts[i]));
				cur = n;
			}
		}
		dmtRes.removeAllChildren();
		addNodes(dmtRes,root);
		dtmRes.reload();
		for (int i = 0; i < treRes.getRowCount(); i++) {
			treRes.expandRow(i);
		}
	}
	
	private void addNodes(DefaultMutableTreeNode parent, Node node) {
		for (Node n:node.children) {
			DefaultMutableTreeNode dmt = new DefaultMutableTreeNode(n.name);
			parent.add(dmt);
			addNodes(dmt, n);
		}
	}
	
	private class Node {
		private String name;
		private ArrayList<Node> children = new ArrayList<Node>();
		private Node(String name) {
			this.name = name;
		}
		private Node get(String name) {
			for (Node c:children)
				if (name.equals(c.name))
					return c;
			return null;
		}
	}
	
	public static class MyPostRequest extends POSTRequest {	
	}
	
	private void performRequest(Request request) {
		txaResponse.setText("no response yet");
		responseBorder.setTitle("Response: none");
		pnlResponse.repaint();
		request.registerResponseHandler(new ResponsePrinter());
		request.setPayload(txaPayload.getText());
		request.setURI(cboTarget.getSelectedItem().toString().replace(" ", "%20"));
		execute(request);
	}
	
	private void execute(Request request) {
		try {
			request.execute();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private class ResponsePrinter implements ResponseHandler {
		public void handleResponse(Response response) {
			txaResponse.setText(response.getPayloadString());
			responseBorder.setTitle("Response: "+CodeRegistry.toString(response.getCode()));
			pnlResponse.repaint();
		}
	}
	
	public static void main(String[] args) {
		setLookAndFeel();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("CoAP Client");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(new GUIClient());
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}
	
	private static void setLookAndFeel() {
		try {
	        UIManager.setLookAndFeel(
		        	UIManager.getCrossPlatformLookAndFeelClassName());
//		            UIManager.getSystemLookAndFeelClassName());
//		        	"com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	private String getHost() {
		String uri = (String) cboTarget.getSelectedItem(); 
		StringTokenizer st = new StringTokenizer(uri, "/");
		st.nextToken();
		String host = st.nextToken();
		return host;
	}
}
