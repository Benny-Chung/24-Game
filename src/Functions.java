import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.swing.JTable;

public interface Functions extends Remote {
	int login(String name, String password) throws RemoteException;
	int register(String name, String password) throws RemoteException;
	int logout(String name) throws RemoteException;
	JTable getTable() throws RemoteException;
}
