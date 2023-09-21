import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.*;
import javax.script.*;


public class Client implements Runnable{
	
	private Functions func;
	private String nameMain;
	private JTextField nameL;
	private JPasswordField passwordL;
	private JTextField nameR;
	private JPasswordField passwordR;
	private JPasswordField confirmPW;
	static private Login loginFrame;
	static private Register regFrame;
	static private Logged loggedFrame;
	private QueueSendToServer sender;
	private TopicRecvFromServer receiver;

	String host = "localhost";
	JPanel gamePanel = new JPanel();
	JLabel gameMsg = new JLabel("Waiting for players...", SwingConstants.CENTER);
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Client(args[0]));
	}
	
	public Client(String host) {
	    try {  	
	    	this.host = host;
	    	Registry registry = LocateRegistry.getRegistry(host);
	        func = (Functions)registry.lookup("Server");
			sender = new QueueSendToServer(host);
			receiver = new TopicRecvFromServer(host);
			Receiver r = new Receiver();
			new Thread(r).start();
			
	    } catch(Exception e) {
	        System.err.println("Failed accessing RMI: "+e);
	    }
	}
	
	public void run() {  
		loginFrame = new Login();
		loginFrame.setVisible(true);
	}
	
	private class Login extends JFrame {
		private static final long serialVersionUID = 33581L;
		
		public Login() {
			setTitle("Login");
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setBounds(100, 100, 450, 300);
	
			setLayout(null);
			
			JLabel nameTextL = new JLabel("Login name");
			nameTextL.setFont(new Font("PMingLiU", Font.PLAIN, 15));
			nameTextL.setBounds(34, 38, 125, 15);
			add(nameTextL);
			
			nameL = new JTextField();
			nameL.setBounds(34, 63, 347, 27);
			add(nameL);
			nameL.setColumns(30);
			
			passwordL = new JPasswordField();
			passwordL.setBounds(34, 136, 347, 27);
			passwordL.setColumns(30);
			add(passwordL);
			
			JLabel passwordTextL = new JLabel("Password");
			passwordTextL.setFont(new Font("PMingLiU", Font.PLAIN, 15));
			passwordTextL.setBounds(34, 111, 111, 15);
			add(passwordTextL);
			
			JButton loginButtonL = new JButton("Login");
			loginButtonL.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String name = nameL.getText();
					String password = String.valueOf(passwordL.getPassword());
					if (name.trim().isEmpty() || password.trim().isEmpty()) {
						JOptionPane.showMessageDialog(Login.this, "Login name or password should not be empty.");
					} else {
						try {
							int status = func.login(name, password);
							if (status == 2) {JOptionPane.showMessageDialog(Login.this, "You have already logged in.");}
							else if (status == 1) {JOptionPane.showMessageDialog(Login.this, "Incorrect login name or password.");}
							else if (status == -1) {JOptionPane.showMessageDialog(Login.this, "Some error has occured.");}
							else if (status == 0){
								setVisible(false);
								nameMain = name;
								System.out.println("Connected to server");
								loggedFrame = new Logged();
								loggedFrame.setVisible(true);
							}			
						} catch (Exception ex) {ex.printStackTrace();}
					}
				}
			});
			
			loginButtonL.setBounds(34, 199, 99, 27);
			add(loginButtonL);
			
			JButton registerButtonL = new JButton("Register");
			registerButtonL.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					regFrame = new Register();
					regFrame.setVisible(true);
				}
			});
			registerButtonL.setBounds(282, 199, 99, 27);
			add(registerButtonL);
			
		}
	}
	
	private class Register extends JFrame {	
		private static final long serialVersionUID = 33582L;
		public Register() {
			setTitle("Register");
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setBounds(100, 100, 450, 381);
	
			setLayout(null);
			
			JLabel nameTextR = new JLabel("Login name");
			nameTextR.setFont(new Font("PMingLiU", Font.PLAIN, 15));
			nameTextR.setBounds(34, 38, 125, 15);
			add(nameTextR);
			
			nameR = new JTextField();
			nameR.setBounds(34, 63, 347, 27);
			add(nameR);
			nameR.setColumns(30);
			
			passwordR = new JPasswordField();
			passwordR.setBounds(34, 136, 347, 27);
			passwordR.setColumns(30);
			add(passwordR);
			
			JLabel passwordTextR = new JLabel("Password");
			passwordTextR.setFont(new Font("PMingLiU", Font.PLAIN, 15));
			passwordTextR.setBounds(34, 111, 111, 15);
			add(passwordTextR);
			
			JButton registerButtonR = new JButton("Register");
			registerButtonR.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String name = nameR.getText();
					String password = String.valueOf(passwordR.getPassword());
					String confirm = String.valueOf(confirmPW.getPassword());
					if (name.trim().isEmpty() || password.trim().isEmpty()) {
						JOptionPane.showMessageDialog(Register.this, "Login name or password should not be empty.");
					} else if (!password.equals(confirm)) {
						JOptionPane.showMessageDialog(Register.this, "Password and confirm password do not match.");
						System.out.println(password + confirm);
					} else {
						try {
							int status = func.register(name, password);
							if (status == 1) {JOptionPane.showMessageDialog(Register.this, "The name has already been registered.");}
							else if (status == -1) {JOptionPane.showMessageDialog(Register.this, "Some error has occured.");}
							else if (status == 0) {
								JOptionPane.showMessageDialog(Register.this, "Registration success.");
								status = func.login(name, password);
								if (status == 0) {
									nameMain = name;
									System.out.println("Connected to server");
									setVisible(false);
									loggedFrame = new Logged();
									loggedFrame.setVisible(true);
								}
							}			
						} catch (Exception ex) {ex.printStackTrace();}
					}
				}
			});
			registerButtonR.setBounds(34, 282, 99, 27);
			add(registerButtonR);
			
			JButton cancelButtonR = new JButton("Cancel");
			cancelButtonR.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					loginFrame.setVisible(true);
				}
			});
			cancelButtonR.setBounds(282, 282, 99, 27);
			add(cancelButtonR);
			
			JLabel confirmPwText = new JLabel("Confirm password");
			confirmPwText.setFont(new Font("PMingLiU", Font.PLAIN, 15));
			confirmPwText.setBounds(34, 184, 125, 15);
			add(confirmPwText);
			
			confirmPW = new JPasswordField();
			confirmPW.setColumns(30);
			confirmPW.setBounds(34, 209, 347, 27);
			add(confirmPW);
		}
	}
	
	private class Logged extends JFrame{
		private static final long serialVersionUID = 33583L;
		private UserProfile tab1;
		private PlayGame tab2;
		private Leaderboard tab3;
		private Logout tab4;
		private JTabbedPane tabs;
		
		public Logged() {
			setTitle("Poker 24-Game");
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent e) {
			      try {func.logout(nameL.getText());}
			      catch (Exception ex) {ex.printStackTrace();}
			    }
			});
			setBounds(100, 100, 800, 380);
	
			tabs = new JTabbedPane();
		    tabs.addChangeListener(new ChangeListener() {
		        public void stateChanged(ChangeEvent e) {
		        	try {
			            tab1 = new UserProfile();
			            tab3 = new Leaderboard();
		        	} catch (Exception ex) {
						try {
							func.logout(nameL.getText());
						} catch (Exception exc) {exc.printStackTrace();}
						ex.printStackTrace();
		        	}
		        }
		    });
		    
			try {
				tab1 = new UserProfile();
				tab2 = new PlayGame();
				tab3 = new Leaderboard();
				tab4 = new Logout();
				tabs.add("User Profile", tab1);
				tabs.add("Play Game", tab2);
				tabs.add("Leaderboard", tab3);
				tabs.add("Logout",tab4);
				add(tabs);
			} catch (Exception e) {
				try {
					func.logout(nameMain);
				} catch (Exception ex) {ex.printStackTrace();}
				e.printStackTrace();
			}
		}
		
		private class UserProfile extends JPanel {
			private static final long serialVersionUID = 33584L;
			public UserProfile() throws Exception {
				setLayout(null);
				JTable table = func.getTable();
				TableModel model = table.getModel();
				String name = nameMain;
				
				int i = 0;
				while (i < table.getRowCount()) {
					if (name.equals((String) model.getValueAt(i, 1))) {break;} 
					i++;
				}
				if (i >= table.getRowCount()) {i=table.getRowCount()-1;}
				
				JLabel profileName = new JLabel((String) model.getValueAt(i, 1));
				profileName.setFont(new Font("PMingLiU", Font.BOLD, 20));
				profileName.setBounds(35, 35, 213, 24);
				add(profileName);
				
				JLabel profileStat = new JLabel("<html>Number of wins: " + (String) model.getValueAt(i, 2) + "<br/>"
						+ "Number of games: " + (String) model.getValueAt(i, 3) + "<br/>"
								+ "Average time to win: " + model.getValueAt(i, 4) + "</html>");
				profileStat.setBounds(35, 55, 213, 100);
				add(profileStat);
				
				JLabel profileRank = new JLabel("Rank: #" + (String) model.getValueAt(i, 0));
				profileRank.setFont(new Font("PMingLiU", Font.BOLD, 15));
				profileRank.setBounds(35, 105, 213, 100);
				add(profileRank);
				

			}
		}
		
		private class PlayGame extends JPanel {
			
			private static final long serialVersionUID = 33585L;
			
			public PlayGame() throws Exception {
				
				gamePanel.removeAll();
				setLayout(new BorderLayout());
				JButton newGame = new JButton("New game");
				gameMsg.setText("Waiting for players...");
				newGame.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						newGame.setVisible(false);			
						add(gameMsg, BorderLayout.CENTER);
						validate();
						try {
							sender.sendMessages(nameMain + ",start");
							add(gamePanel);
						} catch (JMSException ex) {
							System.out.println("Send Message error:");
							ex.printStackTrace();
						}
					}
				});
				add(newGame);
				
			}
		}
		
		private class Leaderboard extends JPanel {
			private static final long serialVersionUID = 33586L;
			
			public Leaderboard() throws Exception{
				setLayout(new BorderLayout());
				JTable table = func.getTable();
				TableModel model = table.getModel();
				
				for (int i=0; i<model.getRowCount(); i++) {
					model.setValueAt(Integer.parseInt((String)model.getValueAt(i, 0)), i, 0);
				}
				
				TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
				table.setRowSorter(sorter);
				
				add(table, BorderLayout.CENTER);
				add(new JScrollPane(table));	
			}
		}
		
		private class Logout extends JPanel {
			private static final long serialVersionUID = 33587L;
			public Logout() {
				setLayout(null);
				JLabel profileName = new JLabel("Are you sure you want to logout?");
				profileName.setFont(new Font("PMingLiU", Font.BOLD, 20));
				profileName.setBounds(235, 115, 353, 24);
				add(profileName);
				
				JButton logoutButton = new JButton("Yes");
				logoutButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							func.logout(nameMain);
							loggedFrame.setVisible(false);
							loginFrame.setVisible(true);
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(Logged.this, "Some error has occured.");
							ex.printStackTrace();
						}
					}
				});
				logoutButton.setBounds(325, 185, 70, 24);
				add(logoutButton);
				
			}
		}
	}
	
	
	public class QueueSendToServer {
		
		private String host;
		public QueueSendToServer(String host) throws NamingException, JMSException {
			this.host = host;
			
			// Access JNDI
			createJNDIContext();
			
			// Lookup JMS resources
			lookupConnectionFactory();
			lookupQueue();
			
			// Create connection->session->sender
			createConnection();

		}
		
		private Context jndiContext;
		private void createJNDIContext() throws NamingException {
			System.setProperty("org.omg.CORBA.ORBInitialHost", host);
			System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
			try {
				jndiContext = new InitialContext();
			} catch (NamingException e) {
				System.err.println("Could not create JNDI API context: " + e);
				throw e;
			}
		}
		
		private ConnectionFactory connectionFactory;
		private void lookupConnectionFactory() throws NamingException {

			try {
				connectionFactory = (ConnectionFactory)jndiContext.lookup("jms/JPoker24GameConnectionFactory");
			} catch (NamingException e) {
				System.err.println("JNDI API JMS connection factory lookup failed: " + e);
				throw e;
			}
		}
		
		private Queue queue;
		private void lookupQueue() throws NamingException {

			try {
				queue = (Queue)jndiContext.lookup("jms/JPoker24GameQueue");
			} catch (NamingException e) {
				System.err.println("JNDI API JMS queue lookup failed: " + e);
				throw e;
			}
		}
		
		private Connection connection;
		private void createConnection() throws JMSException {
			try {
				connection = connectionFactory.createConnection();
				connection.start();
			} catch (JMSException e) {
				System.err.println("Failed to create connection to JMS provider: " + e);
				throw e;
			}
		}
		
		public void sendMessages(String msg) throws JMSException {
			createSession();
			createSender();			
			TextMessage message = session.createTextMessage(); 

			message.setText(msg);
			queueSender.send(message);
			System.out.println("Sending message "+msg);

			// send non-text control message to end
			queueSender.send(session.createMessage());
		}
		
		
		private Session session;
		private void createSession() throws JMSException {
			try {
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			} catch (JMSException e) {
				System.err.println("Failed to create session: " + e);
				throw e;
			}
		}
		
		// Was: QueueSender
		private MessageProducer queueSender;
		private void createSender() throws JMSException {
			try {
				queueSender = session.createProducer(queue);
			} catch (JMSException e) {
				System.err.println("Failed to create session: " + e);
				throw e;
			}
		}
		
		public void close() {
			if(connection != null) {
				try {
					connection.close();
				} catch (JMSException e) { }
			}
		}
		
	}
	
	public class TopicRecvFromServer {
		
		private String host;
		public TopicRecvFromServer(String host) throws NamingException, JMSException {
			this.host = host;
			
			// Access JNDI
			createJNDIContext();
			
			// Lookup JMS resources
			lookupConnectionFactory();
			lookupTopic();
			
			// Create connection->session->sender
			createConnection();

		}
		
		private Context jndiContext;
		private void createJNDIContext() throws NamingException {
			System.setProperty("org.omg.CORBA.ORBInitialHost", host);
			System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
			try {
				jndiContext = new InitialContext();
			} catch (NamingException e) {
				System.err.println("Could not create JNDI API context: " + e);
				throw e;
			}
		}
		
		private ConnectionFactory connectionFactory;
		private void lookupConnectionFactory() throws NamingException {

			try {
				connectionFactory = (ConnectionFactory)jndiContext.lookup("jms/JPoker24GameConnectionFactory");
			} catch (NamingException e) {
				System.err.println("JNDI API JMS connection factory lookup failed: " + e);
				throw e;
			}
		}
		
		private Topic topic;
		private void lookupTopic() throws NamingException {

			try {
				topic = (Topic)jndiContext.lookup("jms/JPoker24GameTopic");
			} catch (NamingException e) {
				System.err.println("JNDI API JMS topic lookup failed: " + e);
				throw e;
			}
		}
		
		private Connection connection;
		private void createConnection() throws JMSException {
			try {
				connection = connectionFactory.createConnection();
				connection.start();
			} catch (JMSException e) {
				System.err.println("Failed to create connection to JMS provider: " + e);
				throw e;
			}
		}
		
		// Modified to take care of the entire gameplay mechanism
		public void receiveMessages() throws JMSException {
			createSession();
			createReceiver();
			
			// message format: {command, args}
			while(true) {
				Message m = topicReceiver.receive();
				if(m instanceof TextMessage) {
					TextMessage textMessage = (TextMessage)m;
					String message = textMessage.getText();
					System.out.println("Received message: "+message);
					String[] msgs = message.split(",");
					if (msgs.length > 2) {
						if (msgs[0].equals(nameMain)) {
							if (msgs[1].equals("answer:")) {
								gamePanel.removeAll();
								gamePanel.setLayout(new BorderLayout());
								JButton newG = new JButton("New game");
								newG.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										newG.setVisible(false);			
										gameMsg.setText("Waiting for players...");
										gamePanel.validate();
										try {
											sender.sendMessages(nameMain + ",start");
										} catch (JMSException ex) {
											System.out.println("Send Message error:");
											ex.printStackTrace();
										}
									}
								});
								gamePanel.add(newG, BorderLayout.SOUTH);
	
								if (msgs[2].equals("right")) { 
									gameMsg.setText("You win!");
								} else if (msgs[2].equals("wrong")) { 
									gameMsg.setText("Wrong answer!");
								} else {
									gameMsg.setText("<html>Winner: "+msgs[2]+"<br/>"+msgs[3]+"</html>");
								}
								try {
	
									loggedFrame.tabs.remove(2);
									loggedFrame.tabs.remove(0);
									loggedFrame.tabs.add(loggedFrame.new UserProfile(),0);
									loggedFrame.tabs.add(loggedFrame.new Leaderboard(),2);
									loggedFrame.tabs.setTitleAt(0, "User Profile");
									loggedFrame.tabs.setTitleAt(2, "Leaderboard");
									loggedFrame.tabs.validate();
	
								} catch (Exception e) {
									System.out.println("Error showing updated user data.");
								}
							}
							if (msgs[1].equals("start:")) {
								gameMsg.setText("");
								gamePanel.setLayout(null);
								JLabel calSum = new JLabel("= 0");
								calSum.setFont(new Font("PMingLiU", Font.BOLD, 15));
								calSum.setBounds(480,250,100,30);
								gamePanel.add(calSum);
								
								JTextField answer = new JTextField(55);
								
								answer.getDocument().addDocumentListener(new DocumentListener() {
									  public void changedUpdate(DocumentEvent e) {
									    cal();
									  }
									  public void removeUpdate(DocumentEvent e) {
									    cal();
									  }
									  public void insertUpdate(DocumentEvent e) {
									    cal();
									  }
									  
									  public void cal() {
									     try {
											ScriptEngineManager mgr = new ScriptEngineManager();
											ScriptEngine engine = mgr.getEngineByName("JavaScript");
											Object result = engine.eval(answer.getText());
											calSum.setText("= "+ result);
	
									     } catch (Exception e) {
									    	 calSum.setText("= NaN");
									     }
									  }
									});
								
								answer.addActionListener(new ActionListener() {								  
									  public void actionPerformed(ActionEvent e) {
									     try {
											sender.sendMessages(nameMain + ",answer," + answer.getText());
											answer.setEnabled(false);
									     } catch (JMSException ex) {
									    	 System.out.println("Send answer error");
									    	 ex.printStackTrace();
									     }
									  }
									});
								
								answer.setBounds(20,250,450,30);
								gamePanel.add(answer);
								
								JLabel[] cards = {new JLabel(), new JLabel(), new JLabel(), new JLabel()};
								for (int i=0; i<4; i++) {
									gamePanel.setLayout(null);
									cards[i].setIcon(new ImageIcon("Images/card_" + (Integer.parseInt(msgs[i+2])/13+1) + (Integer.parseInt(msgs[i+2])%13+1) + ".gif"));
									cards[i].setBounds(50+i*100, 40, 200, 200);
									gamePanel.add(cards[i]);
								}
								
								JTable table = new JTable();
								try {
									table = func.getTable();
								} catch (RemoteException e) {
									System.out.println("RMI exception");
									e.printStackTrace();
								}
								TableModel model = table.getModel();
								for (int i=6; i<msgs.length; i++) {
									JPanel playerPanel = new JPanel();
									
									String name = msgs[i];
									int j = 0;
									while (j < table.getRowCount()) {
										if (name.equals((String) model.getValueAt(j, 1))) {
											break;
										} 
										j++;
									}
									JLabel opponent = new JLabel((String) model.getValueAt(j, 1));
									opponent.setFont(new Font("PMingLiU", Font.BOLD, 20));
									JLabel stat = new JLabel("Win:" +(String) model.getValueAt(j, 2)+"/"+(String) model.getValueAt(j, 3)+" avg: " + model.getValueAt(j, 4) +"s");
									stat.setFont(new Font("PMingLiU", Font.BOLD, 15));
									
									playerPanel.setLayout(new BorderLayout());
									playerPanel.add(opponent,BorderLayout.NORTH);
									playerPanel.add(stat, BorderLayout.SOUTH);
									playerPanel.setBorder(BorderFactory.createLineBorder(Color.black));
									playerPanel.setBounds(550,20+(i-6)*65,190,55);
									gamePanel.add(playerPanel);
									gamePanel.validate();
									gamePanel.repaint();
								}
							}
						}
					}
				}
			}
		}
		
		private Session session;
		private void createSession() throws JMSException {
			try {
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			} catch (JMSException e) {
				System.err.println("Failed to create session: " + e);
				throw e;
			}
		}
		
		// Was: TopicReceiver
		private MessageConsumer topicReceiver;
		private void createReceiver() throws JMSException {
			try {
				topicReceiver = session.createConsumer(topic);
			} catch (JMSException e) {
				System.err.println("Failed to create session: " + e);
				throw e;
			}
		}
		
		public void close() {
			if(connection != null) {
				try {
					connection.close();
				} catch (JMSException e) { }
			}
		}
	}
	
	private class Receiver implements Runnable {
		public void run() {
			try {
				receiver.receiveMessages();
			} catch (Exception e) {
				System.out.println("JMS receive error: ");
				e.printStackTrace();
			}
		}
	}
}