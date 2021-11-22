import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Color;
import javax.swing.border.LineBorder;

enum PaintMode {Pixel, Area};

public class UI extends JFrame {
	
	private static Socket socket;
	private static DataInputStream in;
	private static DataOutputStream out;
	
	private JTextField msgField;
	private JTextArea chatArea;
	private JPanel pnlColorPicker;
	private JPanel paintPanel;
	private JToggleButton tglPen;
	private JToggleButton tglBucket;
	
	private static UI instance;
	private int selectedColor = -543230; 	//golden
	
	int[][] data = new int[50][50];			// pixel color data array
	int blockSize = 16;
	PaintMode paintMode = PaintMode.Pixel;
	
	/**
	 * get the instance of UI. Singleton design pattern.
	 * @return
	 */
	public static UI getInstance(String server, int port) throws IOException {
		socket = new Socket(server, port);
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		
		
		
		if (instance == null)
			instance = new UI(socket);
		
		return instance;
	}
	
	/**
	 * private constructor. To create an instance of UI, call UI.getInstance() instead.
	 */
	private UI(Socket socket) {
		
		Thread t = new Thread(() -> {
			byte[] buffer = new byte[1024];
			try {				
				while (true) {
					int len = in.readInt();					
					in.read(buffer, 0, len);
					String pointStr = new String(buffer, 0, len);
					String point[] = pointStr.split(",");
					int x = Integer.parseInt(point[0]);
					int y = Integer.parseInt(point[1]);
					int c = Integer.parseInt(point[2]);
					if(pointStr != "" || !pointStr.isEmpty())
						paintPixel(x,y,c);
					else
						break;
					//g2.setColor(color);
					//paintPixel(x,y);
					//paintPanel.repaint(x, y, blockSize, blockSize);
					System.out.println(pointStr);
				}
			} catch (IOException ex) {
				System.out.println("receive error");
				chatArea.setText("Connection dropped!");
				System.exit(-1);
			} catch (NumberFormatException ex) {
				System.out.println("receive error NFE");
			}
		});
		t.start();
		
		setTitle("KidPaint");
		
		JPanel basePanel = new JPanel();
		getContentPane().add(basePanel, BorderLayout.CENTER);
		basePanel.setLayout(new BorderLayout(0, 0));
		//********************************************************************************
		paintPanel = new JPanel() {			
			
			// refresh the paint panel
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				
				
				Graphics2D g2 = (Graphics2D) g; // Graphics2D provides the setRenderingHints method
				
				// enable anti-aliasing
			    RenderingHints rh = new RenderingHints(
			             RenderingHints.KEY_ANTIALIASING,
			             RenderingHints.VALUE_ANTIALIAS_ON);
			    g2.setRenderingHints(rh);
			    
			    // clear the paint panel using black
				g2.setColor(Color.black);
				g2.fillRect(0, 0, this.getWidth(), this.getHeight());
				
				
				// draw and fill circles with the specific colors stored in the data array
				for(int x=0; x<data.length; x++) {
					for (int y=0; y<data[0].length; y++) {
						g2.setColor(new Color(data[x][y]));
						g2.fillArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
						g2.setColor(Color.darkGray);
						g2.drawArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
					}
				}
			}
		};
		
		
		
		paintPanel.addMouseListener(new MouseListener() {
			
			@Override public void mouseClicked(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}

			// handle the mouse-up event of the paint panel
			@Override
			public void mouseReleased(MouseEvent e) {
				if (paintMode == PaintMode.Area && e.getX() >= 0 && e.getY() >= 0)
					paintArea(e.getX()/blockSize, e.getY()/blockSize,0);
					int x = e.getX()/blockSize;
					int y = e.getY()/blockSize;
					/*		
					String str = x + "," + y; 
					try {
						out.writeInt(str.length());
						out.write(str.getBytes(), 0, str.length());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}*/					
			}
		});
		
		paintPanel.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				
				if (paintMode == PaintMode.Pixel && e.getX() >= 0 && e.getY() >= 0)
					paintPixel(e.getX()/blockSize,e.getY()/blockSize,0);
				/*
					String str = e.getX()/blockSize + "," + e.getY()/blockSize; 
					try {
						out.writeInt(str.length());
						out.write(str.getBytes(), 0, str.length());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}*/
			}

			@Override public void mouseMoved(MouseEvent e) {}
			
		});
		
		paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));
		
		JScrollPane scrollPaneLeft = new JScrollPane(paintPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		basePanel.add(scrollPaneLeft, BorderLayout.CENTER);
		
		JPanel toolPanel = new JPanel();
		basePanel.add(toolPanel, BorderLayout.NORTH);
		toolPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		
		pnlColorPicker = new JPanel();
		pnlColorPicker.setPreferredSize(new Dimension(24, 24));
		pnlColorPicker.setBackground(new Color(selectedColor));
		pnlColorPicker.setBorder(new LineBorder(new Color(0, 0, 0)));

		// show the color picker
		pnlColorPicker.addMouseListener(new MouseListener() {
			@Override public void mouseClicked(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {
				ColorPicker picker = ColorPicker.getInstance(UI.instance);
				Point location = pnlColorPicker.getLocationOnScreen();
				location.y += pnlColorPicker.getHeight();
				picker.setLocation(location);
				picker.setVisible(true);
			}
			
		});
		
		toolPanel.add(pnlColorPicker);
		
		tglPen = new JToggleButton("Pen");
		tglPen.setSelected(true);
		toolPanel.add(tglPen);
		
		tglBucket = new JToggleButton("Bucket");
		toolPanel.add(tglBucket);
		
		// change the paint mode to PIXEL mode
		tglPen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(true);
				tglBucket.setSelected(false);
				paintMode = PaintMode.Pixel;
			}
		});
		
		// change the paint mode to AREA mode
		tglBucket.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(false);
				tglBucket.setSelected(true);
				paintMode = PaintMode.Area;
			}
		});
		
		JPanel msgPanel = new JPanel();
		
		getContentPane().add(msgPanel, BorderLayout.EAST);
		
		msgPanel.setLayout(new BorderLayout(0, 0));
		
		msgField = new JTextField();	// text field for inputting message
		
		/*Thread t = new Thread(() -> {
			byte[] buffer = new byte[1024];
			try {				
				while (true) {
					int len = in.readInt();					
					in.read(buffer, 0, len);
					String old = chatArea.getText();
					if(old != "" || !old.isEmpty()){
						chatArea.setText(old + '\n' +new String(buffer, 0, len));
					}else {
						chatArea.setText(new String(buffer, 0, len));
					}
					
				}
			} catch (IOException ex) {
				chatArea.setText("Connection dropped!");
				System.exit(-1);
			}
		});
		t.start();*/
		msgPanel.add(msgField, BorderLayout.SOUTH);
		
		// handle key-input event of the message field
	
		
		chatArea = new JTextArea();		// the read only text area for showing messages
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		
		JScrollPane scrollPaneRight = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneRight.setPreferredSize(new Dimension(300, this.getHeight()));
		msgPanel.add(scrollPaneRight, BorderLayout.CENTER);
		
		this.setSize(new Dimension(800, 600));
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	/**
	 * it will be invoked if the user selected the specific color through the color picker
	 * @param colorValue - the selected color
	 */
	public void selectColor(int colorValue) {
		SwingUtilities.invokeLater(()->{
			selectedColor = colorValue;
			pnlColorPicker.setBackground(new Color(colorValue));
		});
	}
		 
	/**
	 * it will be invoked if the user inputted text in the message field
	 * @param text - user inputted text
	 */
	private void onTextInputted(String text) {
		chatArea.setText(chatArea.getText() + text + "\n");
	}
	
	/**
	 * change the color of a specific pixel
	 * @param col, row - the position of the selected pixel
	 */
	public void paintPixel(int col, int row, int c) {
		
		if (col >= data.length || row >= data[0].length) return;
		if(c==0) {
			
			String str = col + "," + row + "," + selectedColor; 
			try {
				out.writeInt(str.length());
				out.write(str.getBytes(), 0, str.length());
			} catch (IOException e1) {
				System.out.println("send error");
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			data[col][row] = selectedColor;
			paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);		
		}else {
			
			data[col][row] = c;
			paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);	
		}
		
	}
	
	/**
	 * change the color of a specific area
	 * @param col, row - the position of the selected pixel
	 * @return a list of modified pixels
	 */
	public List paintArea(int col, int row, int c) {
		LinkedList<Point> filledPixels = new LinkedList<Point>();
		if(c==0) {			
			
	
			if (col >= data.length || row >= data[0].length) return filledPixels;
	
			int oriColor = data[col][row];
			LinkedList<Point> buffer = new LinkedList<Point>();
			
			if (oriColor != selectedColor) {
				buffer.add(new Point(col, row));
				
				while(!buffer.isEmpty()) {
					
					Point p = buffer.removeFirst();
					int x = p.x;
					int y = p.y;
					
					
					
					if (data[x][y] != oriColor) continue;
					
					data[x][y] = selectedColor;
					filledPixels.add(p);
					
					if (x > 0 && data[x-1][y] == oriColor) buffer.add(new Point(x-1, y));
					if (x < data.length - 1 && data[x+1][y] == oriColor) buffer.add(new Point(x+1, y));
					if (y > 0 && data[x][y-1] == oriColor) buffer.add(new Point(x, y-1));
					if (y < data[0].length - 1 && data[x][y+1] == oriColor) buffer.add(new Point(x, y+1));
									
				}
				paintPanel.repaint();
			}
			
		}else {
			
			if (col >= data.length || row >= data[0].length) return filledPixels;
	
			int oriColor = data[col][row];
			LinkedList<Point> buffer = new LinkedList<Point>();
			
			if (oriColor != selectedColor) {
				buffer.add(new Point(col, row));
				
				while(!buffer.isEmpty()) {
					
					Point p = buffer.removeFirst();
					int x = p.x;
					int y = p.y;
					
					
					if (data[x][y] != oriColor) continue;
					
					data[x][y] = c;
					filledPixels.add(p);
		
					if (x > 0 && data[x-1][y] == oriColor) {
						buffer.add(new Point(x-1, y));
						String str = (x-1) + "," + y + "," + c; 
						try {
							out.writeInt(str.length());
							out.write(str.getBytes(), 0, str.length());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					if (x < data.length - 1 && data[x+1][y] == oriColor) {
						buffer.add(new Point(x+1, y));
						String str = (x+1) + "," + y + "," + c; 
						try {
							out.writeInt(str.length());
							out.write(str.getBytes(), 0, str.length());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					if (y > 0 && data[x][y-1] == oriColor) {
						buffer.add(new Point(x, y-1));
						String str = x + "," + (y-1) + "," + c; 
						try {
							out.writeInt(str.length());
							out.write(str.getBytes(), 0, str.length());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					if (y < data[0].length - 1 && data[x][y+1] == oriColor) {
						buffer.add(new Point(x, y+1));
						String str = x + "," + (y+1) + "," + c; 
						try {
							out.writeInt(str.length());
							out.write(str.getBytes(), 0, str.length());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
	
				}
				paintPanel.repaint();
				
			}
		}
		return filledPixels;
		
	}
	
	/**
	 * set pixel data and block size
	 * @param data
	 * @param blockSize
	 */
	public void setData(int[][] data, int blockSize) {
		this.data = data;
		this.blockSize = blockSize;
		paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));
		paintPanel.repaint();
	}
}
