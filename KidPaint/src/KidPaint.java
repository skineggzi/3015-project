import java.io.IOException;

public class KidPaint {
	public static void main(String[] args) {
		try {
			UI ui = UI.getInstance("127.0.0.1", 12345);			// get the instance of UI
			ui.setData(new int[50][50], 20);	// set the data array and block size. comment this statement to use the default data array and block size.
			ui.setVisible(true);
		} catch (IOException e) {
			System.err.printf("Unable to connect server %s:%d\n", "127.0.0.1", 12345);
			System.exit(-1);
		} catch (IndexOutOfBoundsException | NumberFormatException e) {
			System.err.println("Usage: java SimpleChatClient ipaddress portNum");
			System.exit(-1);
		}
	}
}
