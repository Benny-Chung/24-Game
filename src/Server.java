import java.rmi.*;
import java.rmi.server.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Topic;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.util.*;

public class Server extends UnicastRemoteObject implements Functions{
	
	private static final long serialVersionUID = 33580L;
	private static JDBC jdbc;
	TopicSendToClient sender = null;
	QueueRecvFromClients receiver = null;
	ArrayList<String> waiters = new ArrayList<String>();
	ArrayList<Game> gameList = new ArrayList<Game>(); 
	boolean timerFlag = false;
	
	public static void main(String[] args) {
		try {
			Server app = new Server();
			jdbc = app.new JDBC();
			jdbc.resetOnline();
			jdbc.list();
			System.setSecurityManager(new SecurityManager());
			Naming.rebind("Server", app);
			System.out.println("Service registered");
			// int count = app.count("The quick brown fox jumps over a lazy dog");
			// System.out.println("There are "+count+" words");
		} catch(Exception e) {
			System.err.println("Exception thrown: "+e);
		}
	}	
	
	public Server() throws RemoteException {
		String host = "localhost";
		try {
			sender = new TopicSendToClient(host);
			receiver = new QueueRecvFromClients(host);
			Receiver r = new Receiver();
			new Thread(r).start();
		} catch (Exception ex) {
			System.out.println("Error in connecting queues");
			ex.printStackTrace();
		}
	}
	

	
	
	public synchronized int register(String name, String password) throws RemoteException {
		try {
			boolean code = jdbc.checkName(name);
			if (code) {return 1;} // Unsuccessful
			else {
				jdbc.insert(name, password, 0, 0, 0., 0);
				return 0; // Successful
			}  
		} catch (Exception e) {
			e.printStackTrace();
			return -1; // Exception
		}
	}
 
	public synchronized int login(String name, String password) throws RemoteException {
		try {
			boolean code = jdbc.checkNamePassword(name, password);
			if (code) {
				code = jdbc.checkOnline(name);
				if (code) {
					return 2; // Unsuccessful (player online)
				} else {
					jdbc.updateOnline(name, 1);
					return 0; // Successful
				}
			}
			return 1; // Unsuccessful (name-password pair not found)
		} catch (Exception e) {
			e.printStackTrace();
			return -1; // Exception
		}
	}
	
	public synchronized int logout(String name) throws RemoteException {
		try {
			jdbc.updateOnline(name, 0);
			for (Game g: gameList) {
				if (g.findUser(name)) {
					g.removeUser(name);
					break;
				}
			}
			System.out.println(name + " has logged out");		
			return 0; // Successful
		} catch (Exception e) {
			e.printStackTrace();
			return -1; // Exception
		}
	}
	
	public JTable getTable() {
		try {
			return jdbc.list();
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	private class JDBC {
		private java.sql.Connection conn;
		public JDBC() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://sophia.cs.hku.hk/h3568630", "h3568630", "password");	// url, user, password for MySQL
			System.out.println("Database connection successful.");
		}
		
		private void insert(String name, String password, int gamesWon, int gamesPlayed, double avgWinTime, int online) {
			try {
				PreparedStatement stmt = conn.prepareStatement("INSERT INTO game (name, password, gamesWon, gamesPlayed, avgWinTime, online) VALUES (?, ?, ?, ?, ?, ?)");
				stmt.setString(1, name);
				stmt.setString(2, password);
				stmt.setInt(3, gamesWon);
				stmt.setInt(4, gamesPlayed);
				stmt.setDouble(5, avgWinTime);
				stmt.setInt(6, online);
				stmt.execute();
				System.out.println("Record created");
			} catch (SQLException | IllegalArgumentException e) {
				System.err.println("Error insert: "+e);
			}
		}
		
		private void updateOnline(String name, int online) {
			try {
				PreparedStatement stmt = conn.prepareStatement("UPDATE game SET online = ? WHERE name = ?");
				stmt.setInt(1, online);
				stmt.setString(2, name);
				int rows = stmt.executeUpdate();
				if(rows <= 0) {
					System.out.println(name+" not found!");
				}
			} catch (SQLException e) {
				System.err.println("Error updateOnline: "+e);
			}
		}
		
		// reset online statuses upon server starts
		private void resetOnline() {
			try {
				Statement stmt = conn.createStatement();
				int rows = stmt.executeUpdate("UPDATE game SET online = 0");
				if(rows > 0) {
				System.out.println("Reseted online status");			
				} else {
				System.out.println("No update");
				}
			} catch (SQLException e) {
				System.err.println("Error resetOnline: "+e);
			}
		}
		
		
		// Check online status of a player
		private boolean checkOnline(String name) {
			try {
				PreparedStatement stmt = conn.prepareStatement("SELECT name FROM game WHERE name = ? AND online = 1");
				stmt.setString(1, name);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					System.out.println(name + " is online");
					return true;
				}
				return false;
			} catch (SQLException e) {
				System.err.println("Error checkOnline: "+e);
				return true; // true prevent further action during login
			}
		}
		
		// Check if player is in table
		private boolean checkName(String name) {
			try {
				PreparedStatement stmt = conn.prepareStatement("SELECT name FROM game WHERE name = ?");
				stmt.setString(1, name);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					System.out.println(name + " is already reigstered");
					return true;
				}
				return false;
			} catch (SQLException e) {
				System.err.println("Error checkName: "+e);
				return true; // true prevents further action during registration
			}
		}
		
		// Check if name-password pair is in table
		private boolean checkNamePassword(String name, String password) {
			try {
				PreparedStatement stmt = conn.prepareStatement("SELECT name FROM game WHERE name = ? AND password = ?");
				stmt.setString(1, name);
				stmt.setString(2, password);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					return true;
				}
				System.out.println(name + ": incorrect username or password");
				return false;
			} catch (SQLException e) {
				System.err.println("Error checkNamePassword: "+e);
				return false; // false prevents further action during login
			}
		}
		
		private JTable list() {
			JTable table = new JTable();
			DefaultTableModel model = new DefaultTableModel(new String[]{"Rank", "Name", "Games won", "Games played", "Avg. winning time"}, 0);
			try {
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT (@row_number := @row_number + 1) AS rank, name, gamesWon, gamesPlayed, avgWinTime FROM game, (SELECT @row_number := 0) AS x ORDER BY gamesWon DESC, gamesPlayed, avgWinTime ASC");
				while(rs.next()) {
					model.addRow(new String[]{""+rs.getInt(1), rs.getString(2), ""+rs.getInt(3), ""+rs.getInt(4), ""+rs.getDouble(5)});
				}
				
				table.setModel(model);
				return table;
			} catch (SQLException e) {
				System.err.println("Error list: "+e);
				return table;
			}
		}
		
		private void updateLose(String name) {
			try {
				PreparedStatement stmt = conn.prepareStatement("UPDATE game SET gamesPlayed = gamesPlayed+1 WHERE name = ?");
				stmt.setString(1, name);
				int rows = stmt.executeUpdate();
				if(rows > 0) {
				System.out.println("Updated statistics of "+name);			
				} else {
				System.out.println(name+" not found!");
				}
			} catch (SQLException e) {
				System.err.println("Error updateOnline: "+e);
			}
		}
		
		private void updateWin(String name, Double time) {
			try {
				PreparedStatement stmt = conn.prepareStatement("UPDATE game SET avgWinTime = ROUND((gamesWon*avgWinTime+?)/(gamesWon+1),2), gamesWon=gamesWon+1, gamesPlayed=gamesPlayed+1 WHERE name = ?");
				stmt.setDouble(1, time);
				stmt.setString(2, name);
				int rows = stmt.executeUpdate();
				if(rows > 0) {
				System.out.println("Updated statistics of "+name);			
				} else {
				System.out.println(name+" not found!");
				}
			} catch (SQLException e) {
				System.err.println("Error updateOnline: "+e);
			}
		}
	}
	
	public class TopicSendToClient {
	
		private String host;
		public TopicSendToClient(String host) throws NamingException, JMSException {
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
		
		public void sendMessages(String msg) throws JMSException {
			createSession();
			createSender();			

			TextMessage message = session.createTextMessage(); 

			message.setText(msg);
			topicSender.send(message);
			System.out.println("Sending message: "+ msg);

			// send non-text control message to end
			topicSender.send(session.createMessage());
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
		
		// Was: TopicSender
		private MessageProducer topicSender;
		private void createSender() throws JMSException {
			try {
				topicSender = session.createProducer(topic);
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

	
	public class QueueRecvFromClients {
		
		private String host;
		public QueueRecvFromClients(String host) throws NamingException, JMSException {
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
		
		// Modified to take care of the entire gameplay mechanism
		public void receiveMessages() throws JMSException {
			createSession();
			createReceiver();
			
			// message format: {playerName, command, channel, additional args}
			while(true) {
				Message m = queueReceiver.receive();
				if(m instanceof TextMessage) {
					String message = ((TextMessage)m).getText();
					System.out.println("Received message: "+message);
					String[] msgSplit = message.split(",");

					if (msgSplit.length > 1) {
						if (msgSplit[1].equals("start")) {
							System.out.println(msgSplit[0] + " has joined the game");
							waiters.add(msgSplit[0]);
							if (waiters.size() >= 1 && !timerFlag) {
								gameTimer(new TimerHelper());
							}
							
						} else if (msgSplit[1].equals("answer")) { 
							boolean wrongFlag = false;
							try {
								ScriptEngineManager mgr = new ScriptEngineManager();
								ScriptEngine engine = mgr.getEngineByName("JavaScript");
								Object result = engine.eval(msgSplit[2]);
								if (result instanceof Double) {
									double r = (Double) result;
									if (r%1 != 0 || ((int) r) != 24) {wrongFlag = true;}
								}
								if (result instanceof Integer) {
									int r = (Integer) result;
									if (r != 24) {wrongFlag = true;}
								}
							} catch (Exception e) {
								wrongFlag = true;
							}
							
							for (Game g: gameList) {
								if (g.findUser(msgSplit[0])) {
									g.removeUser(msgSplit[0]);
									if (wrongFlag) {
										sender.sendMessages(msgSplit[0] + ",answer:,wrong");
										jdbc.updateLose(msgSplit[0]);
									}
									else {
										String[] nums = msgSplit[2].split("[+-/()*]+");
										if (g.checkCard(nums)) {
											sender.sendMessages(msgSplit[0] + ",answer:,right");
											long nanoT = System.nanoTime() - g.getStart();
											double time = nanoT/1000000000.;
											jdbc.updateWin(msgSplit[0], time);
											
											ArrayList<String> players = new ArrayList<String>(g.getPlayers());
											for (String player: players) {
												if (!player.equals(msgSplit[0])) {
													sender.sendMessages(player + ",answer:," +msgSplit[0]+","+msgSplit[2]);
													jdbc.updateLose(player);
													g.removeUser(player);
												}
											}
										}
										else {
											sender.sendMessages(msgSplit[0] + ",answer:,wrong");
											jdbc.updateLose(msgSplit[0]);
										}	
									}
									break;
								}							 
							}
						}
					}
				}
			} 
		}
		
		public class TimerHelper {
			int i = 0;
			Timer timer = new Timer();
		}
		
		// for game joining stages
		public void gameTimer(TimerHelper th) {
			timerFlag = true;
			TimerTask task;
			task = new TimerTask() {
				@Override
				public void run() { 
					int l = waiters.size();
					if (l >= 4 || (th.i >= 10 && l >= 2 && l <= 3) ) {
						int len = Math.min(l, 4);
						String[] players = new String[len];
						for (int i=0; i<len; i++) {players[i] = waiters.remove(0);}
						gameList.add(new Game(players));
						System.out.println("Started game with " + len + " players");
						timerFlag = false;
						th.timer.cancel();
						th.timer.purge();
					}
					th.i++;
					System.out.println("Second " + th.i);
				}
			};
			th.timer.schedule(task, 0, 1000);
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
		
		// Was: QueueReceiver
		private MessageConsumer queueReceiver;
		private void createReceiver() throws JMSException {
			try {
				queueReceiver = session.createConsumer(queue);
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
	
	private class Game {
		long start = System.nanoTime();;
		ArrayList<String> players = new ArrayList<String>();
		int[] cards = new int[4];
		
		public Game(String[] p) {
			for (int i=0; i<p.length; i++) {players.add(p[i]);}
			Random r = new Random();
			cards[0] = r.nextInt(52);
			for (int i=1; i<4; i++) {
				cards[i] = r.nextInt(52);
				
				for (int j=0; j<i; j++) {
					if (cards[i]%13 == cards[j]%13) {
						i--;
						break;
					}
				}
			}
			String msg = ",start:";
			for (int i=0; i<4; i++) {msg += "," + cards[i];}
			
			for (String player: players) {msg += "," + player;}
			
			try {
				for (String player: players) {sender.sendMessages(player + msg);}
				
			} catch (JMSException e) {
				System.out.println("Send start message error");
				e.printStackTrace();
			}
		}
		
		public boolean findUser(String name) {
			return players.contains(name);
		}
		
		public boolean checkCard(String[] val) {
			int counter = 0;
			if (val.length < 4 || val.length>5) {return false;}
			
			for (int i = 0; i<val.length; i++) {
				if (!val[i].isEmpty()) {
					for (int j=0; j<5; j++) {
						if (j==4) {return false;}
						if (cards[j]%13+1 == Integer.parseInt(val[i])) {
							counter += Math.pow(3, j);
							break;
						}
					}
				}
			}
			if (counter == 40) {return true;}
			return false;
	   	}
		
		public void removeUser(String p) {
			players.remove(p);
			if (players.size() == 0) {gameList.remove(this);}
		}
		
		public ArrayList<String> getPlayers() {return players;}
		public long getStart() {return start;}
	}
}
