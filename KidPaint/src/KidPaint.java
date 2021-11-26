import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JOptionPane;

public class KidPaint {
	public KidPaint(String username, String ip) throws IOException{
		
		UI ui = UI.getInstance(ip, username);			// get the instance of UI
		ui.setUsername(username);
		ui.setData(new int[50][50], 20);	// set the data array and block size. comment this statement to use the default data array and block size.
		ui.setVisible(true);				// set the ui
		
		
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		String username;
		
		InetAddress ip = InetAddress.getLocalHost();
		username = JOptionPane.showInputDialog("Please input your username");
		
		new KidPaint(username, ip.getHostAddress());
		

	}
}
