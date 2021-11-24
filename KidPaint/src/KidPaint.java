import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JOptionPane;

public class KidPaint {
	public KidPaint(String username, String ip) throws IOException{
		UI ui = UI.getInstance(ip, username);			// get the instance of UI
		ui.setData(new int[50][50], 20);	// set the data array and block size. comment this statement to use the default data array and block size.
		ui.setVisible(true);				// set the ui
		
		
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		String username;
		InetAddress ip = InetAddress.getLocalHost();
		username = JOptionPane.showInputDialog("Please input your username");
		//StudiosList studio = new StudiosList(username);
		
		new KidPaint(username, ip.getHostAddress());

	}
}
