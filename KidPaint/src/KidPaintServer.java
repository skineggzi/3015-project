import java.io.DataInputStream;
import java.awt.Point;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class KidPaintServer{
	ServerSocket srvSocket;
	ArrayList<Socket> list = new ArrayList<Socket>();
	static int port = 12345;
	int[][] data;

	public KidPaintServer() throws IOException {
		srvSocket = new ServerSocket(port);

		while (true) {
			System.out.printf("Listening at port %d...\n", port);
			Socket cSocket = srvSocket.accept();

			synchronized (list) {
				list.add(cSocket);
				System.out.printf("Total %d clients are connected.\n", list.size());
			}

			Thread t = new Thread(() -> {
				try {
					serve(cSocket);
				} catch (IOException e) {
					System.err.println("connection dropped.");
				}
				synchronized (list) {
					list.remove(cSocket);
				}
			});
			t.start();
		}

	}

	private void serve(Socket clientSocket) throws IOException {
		byte[] buffer = new byte[1024];
		System.out.printf("Established a connection to host %s:%d\n\n", clientSocket.getInetAddress(),
				clientSocket.getPort());

		DataInputStream in = new DataInputStream(clientSocket.getInputStream());
		DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
		while (true) {
			int function = in.readInt();
			if(function == 1) {
				int col = in.readInt();
				int row = in.readInt();
				int selectedColor = in.readInt();
				forwardPen(selectedColor, col, row,clientSocket);
			}else if(function == 2) {
				int col = in.readInt();
				int row = in.readInt();
				int selectedColor = in.readInt();
				forwardArea(selectedColor, col, row,clientSocket);
			}else if(function == 3) {
				int len = in.readInt();
				in.read(buffer, 0, len);
				String text = new String(buffer, 0, len);
				forwardChat(text, len,clientSocket);
			}else if(function == 10) {
				forwardSave(clientSocket);
			}else if(function == 11) {
				forwardReload(clientSocket);
			}else if(function == 0) {
				int len = in.readInt();
				in.read(buffer, 0, len);
				String name = new String(buffer, 0, len);
				forwardJoin(name, len,clientSocket);
			}
		}
	}

	private void forwardPen(int selectedColor, int col, int row, Socket s) {
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				try { 
					Socket socket = list.get(i);
					if(socket!=s) {
						DataOutputStream out = new DataOutputStream(socket.getOutputStream());
						out.writeInt(1);
						out.writeInt(col);
						out.writeInt(row);
						out.writeInt(selectedColor);
					}
				} catch (IOException e) {
					// the connection is dropped but the socket is not yet removed.
				}
			}
		}
	}
	
	private void forwardArea(int selectedColor, int col, int row, Socket s) {
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				try { 
					Socket socket = list.get(i);
					if(socket!=s) {
						DataOutputStream out = new DataOutputStream(socket.getOutputStream());
						out.writeInt(2);
						out.writeInt(col);
						out.writeInt(row);
						out.writeInt(selectedColor);
					}
				} catch (IOException e) {
					// the connection is dropped but the socket is not yet removed.
				}
			}
		}
	}

	private void forwardChat(String text, int len, Socket s) {
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				try { 
					Socket socket = list.get(i);
					//if(socket!=s) {
						DataOutputStream out = new DataOutputStream(socket.getOutputStream());
						out.writeInt(3);
						out.writeInt(len);
						out.write(text.getBytes(), 0, len);
					//}
				} catch (IOException e) {
					// the connection is dropped but the socket is not yet removed.
				}
			}
		}
	}
	
	private void forwardJoin(String name, int len, Socket s) {
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				try { 
					Socket socket = list.get(i);
					if(socket!=s) {
						DataOutputStream out = new DataOutputStream(socket.getOutputStream());
						out.writeInt(0);
						out.writeInt(len);
						out.write(name.getBytes(), 0, len);
					}
				} catch (IOException e) {
					// the connection is dropped but the socket is not yet removed.
				}
			}
		}
	}
	
	private void forwardSave(Socket s) {
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				try { 
					Socket socket = list.get(i);
					if(socket==s) {
						DataOutputStream out = new DataOutputStream(socket.getOutputStream());
						out.writeInt(10);
					}
				} catch (IOException e) {
					// the connection is dropped but the socket is not yet removed.
				}
			}
		}
	}
	
	private void forwardReload(Socket s) {
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				try { 
					Socket socket = list.get(i);
					if(socket==s) {
						DataOutputStream out = new DataOutputStream(socket.getOutputStream());
						out.writeInt(11);
					}
				} catch (IOException e) {
					// the connection is dropped but the socket is not yet removed.
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		new KidPaintServer();
	}
	
}
