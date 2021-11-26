import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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
	
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	
	private JTextField msgField;
	private JTextArea chatArea;
	private JPanel pnlColorPicker;
	private JPanel paintPanel;
	private JToggleButton tglPen,tglBucket,tglSave,tglReload,tglExport,tglUpload;
	private JFileChooser chooser;
	private static int port = 12345;
	private String username;
	
	private static UI instance;
	private int selectedColor = -543230; 	//golden
	
	int[][] data = new int[50][50];			// pixel color data array
	private Graphics2D g2;
	int blockSize = 16;
	PaintMode paintMode = PaintMode.Pixel;
	
	/**
	 * get the instance of UI. Singleton design pattern.
	 * @return
	 */
	public static UI getInstance(String ip, String username) throws IOException {
		if (instance == null)
			instance = new UI(ip, username);
		
		return instance;
	}
	public static UI getInstance() throws IOException {
		return instance;
	}
	
	/**
	 * private constructor. To create an instance of UI, call UI.getInstance() instead.
	 * @throws IOException 
	 */
	private UI(String ip, String username) throws IOException {
		this.username = username;
		socket = new Socket(ip, port);
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		
		Thread t = new Thread(() -> {
			byte[] buffer = new byte[1024];			
			try {
				
				while (true) {
					int function = in.readInt();
					if(function == 1) {		//pen			
						int col = in.readInt();
						int row = in.readInt();
						selectedColor = in.readInt();
						update_paintPixel(col, row);
						continue;
					}else if(function == 2) { //bucket
						int col = in.readInt();
						int row = in.readInt();
						selectedColor = in.readInt();
						update_paintArea(col, row);
						continue;
					}else if(function == 3){					
						int len = in.readInt();
						in.read(buffer, 0, len);
						String text = new String(buffer, 0, len);
						update_chat(text);
					}else if(function == 10) {
						save();
					}else if(function == 11) {
						reload();
					}else if(function == 0){					
						int len = in.readInt();
						in.read(buffer, 0, len);
						String text = "Welcome "+new String(buffer, 0, len)+" to join us!";
						welcome_chat(text);
					}
				}
			} catch (IOException ex) {
				System.out.println("receive error");
				chatArea.setText("Connection dropped!");
				System.exit(-1);
			}
		});
		t.start();
		
		setTitle("KidPaint ["+username+"]");
		
		JPanel basePanel = new JPanel();
		getContentPane().add(basePanel, BorderLayout.CENTER);
		basePanel.setLayout(new BorderLayout(0, 0));
		//********************************************************************************
		paintPanel = new JPanel() {			
			
			// refresh the paint panel
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				
				
				g2 = (Graphics2D) g; // Graphics2D provides the setRenderingHints method
				
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
					try {
						paintArea(e.getX()/blockSize, e.getY()/blockSize);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					int x = e.getX()/blockSize;
					int y = e.getY()/blockSize;				
			}
		});
		
		paintPanel.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				
				if (paintMode == PaintMode.Pixel && e.getX() >= 0 && e.getY() >= 0)
					try {
						paintPixel(e.getX()/blockSize,e.getY()/blockSize);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

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
		
		tglSave = new JToggleButton("Save");
		toolPanel.add(tglSave);
		
		tglReload = new JToggleButton("Reload");
		toolPanel.add(tglReload);
		
		tglExport = new JToggleButton("Export");
		toolPanel.add(tglExport);
		
		tglUpload = new JToggleButton("Upload");
		toolPanel.add(tglUpload);
		
		// change the paint mode to PIXEL mode
		tglPen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(true);
				tglBucket.setSelected(false);
				tglSave.setSelected(false);
				tglReload.setSelected(false);
				tglExport.setSelected(false);
				tglUpload.setSelected(false);
				paintMode = PaintMode.Pixel;
			}
		});
		
		// change the paint mode to AREA mode
		tglBucket.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(false);
				tglBucket.setSelected(true);
				tglSave.setSelected(false);
				tglReload.setSelected(false);
				tglExport.setSelected(false);
				tglUpload.setSelected(false);	
				paintMode = PaintMode.Area;
			}
		});		
			
		tglSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(false);
				tglBucket.setSelected(false);
				tglSave.setSelected(true);
				tglReload.setSelected(false);
				tglExport.setSelected(false);
				tglUpload.setSelected(false);	
				String c ="";
				for(int x=0; x<data.length; x++) {
					for (int y=0; y<data[x].length; y++) {
						int colorCode = data[x][y];
						if(c!="") {
							c += ","+colorCode;
						}else {
							c = String.valueOf(colorCode);
						}
					}
				}
				try {
				      File myObj = new File("saveIMG.txt");
				      if (myObj.createNewFile()) {				        
					      FileWriter myWriter = new FileWriter("saveIMG.txt");
					      myWriter.write(c);
					      myWriter.close();
					      System.out.println("IMG saved");
				      } else {
				    	  myObj.delete();
				    	  FileWriter myWriter = new FileWriter("saveIMG.txt");
					      myWriter.write(c);
					      myWriter.close();
					      System.out.println("IMG saved");
				      }
				 }catch (IOException e) {
				      System.out.println("An error occurred.");
				      e.printStackTrace();
				 }
				System.out.println(c);
			}
		});
		
		tglReload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(false);
				tglBucket.setSelected(false);
				tglSave.setSelected(false);
				tglReload.setSelected(true);
				tglExport.setSelected(false);
				tglUpload.setSelected(false);
				try {
				      File myObj = new File("saveIMG.txt");
				      if (myObj.exists()) {				     
				    	  String actual = new String ( Files.readAllBytes( Paths.get("saveIMG.txt") ) );
				  	      String colors[] = actual.split(",");
				  	      System.out.println(colors.length);
				  	      String colors2d[][] = new String[50][50];
					  	  for(int i=0; i<50;i++)
					  		  for(int j=0;j<50;j++)
					  			  colors2d[i][j] = colors[(j*50) + i]; 
				  	      for(int x=0; x<data.length; x++) {
				  	    	  for(int y=0; y<data[x].length; y++) {
				  	    		  data[y][x] = Integer.parseInt(colors2d[x][y]);
				  	    		  paintPanel.repaint(x * blockSize, y * blockSize, blockSize, blockSize);
				  	    	  }
				  	      }
				      } else {
					      System.out.println("No IMG was saved");
				      }
				 }catch (IOException e) {
				      System.out.println("An error occurred.");
				      e.printStackTrace();
				 }
			}
		});
		
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		tglExport.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(false);
				tglBucket.setSelected(false);
				tglSave.setSelected(false);
				tglReload.setSelected(false);
				tglExport.setSelected(false);
				tglUpload.setSelected(true);
				BufferedImage imagebuf=null;
			    try {
			        imagebuf = new Robot().createScreenCapture(paintPanel.bounds());
			    } catch (AWTException e1) {
			        // TODO Auto-generated catch block
			        e1.printStackTrace();
			    }  
			     Graphics2D graphics2D = imagebuf.createGraphics();
			     paintPanel.paint(graphics2D);
			     Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			     try {
			        ImageIO.write(imagebuf,"jpeg", new File(username+"_"+sdf.format(timestamp)+".jpeg"));
			    } catch (Exception e) {
			        // TODO Auto-generated catch block
			        System.out.println("error");
			    }
			}
		});
		
		tglUpload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(false);
				tglBucket.setSelected(false);
				tglReload.setSelected(true);
				chooser = new JFileChooser();
				chooser.showOpenDialog(null);
				BufferedImage image;
				try {
					image = ImageIO.read(chooser.getSelectedFile());
					System.out.println(chooser.getSelectedFile().getName());
					/*JLabel picLabel = new JLabel(new ImageIcon(image));
					paintPanel.add(picLabel);*/
					Graphics g = image.getGraphics();
					g.drawImage(image, 0, 0, null);
					paintPanel.repaint();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		JPanel msgPanel = new JPanel();
		
		getContentPane().add(msgPanel, BorderLayout.EAST);
		
		msgPanel.setLayout(new BorderLayout(0, 0));
		
		msgField = new JTextField();	// text field for inputting message
		
		msgPanel.add(msgField, BorderLayout.SOUTH);
		
		// handle key-input event of the message field
		msgField.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e){
						// if the user press ENTER															
					if (e.getKeyCode() == 10) {
						try {
							chat(msgField.getText());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}					

				}
			}
			
		});
		
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
	/*private void onTextInputted(String text) {
		chatArea.setText(chatArea.getText() + text + "\n");
	}*/
	//update new paint
	public void update_paintPixel(int col, int row) {
		if (col >= data.length || row >= data[0].length) return;
		
		data[col][row] = selectedColor;
		paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);
	}
	
	/**
	 * change the color of a specific pixel
	 * @param col, row - the position of the selected pixel
	 * @throws IOException 
	 */
	public void paintPixel(int col, int row) throws IOException {
		if (col >= data.length || row >= data[0].length) return;
		data[col][row] = selectedColor;
		paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);
		out.writeInt(1);
		out.writeInt(col);
		out.writeInt(row);
		out.writeInt(selectedColor);
		
	}
	
	public List update_paintArea(int col, int row){
		LinkedList<Point> filledPixels = new LinkedList<Point>();

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
		
		return filledPixels;
		
	}
	
	public void chat(String text) throws IOException {	
		if (text.length()==0) return;
		String strMessage = "["+username + "]: " + text;
		out.writeInt(3);
		out.writeInt(strMessage.length());
		out.write(strMessage.getBytes(), 0, strMessage.length());
		msgField.setText("");
	}
		
	
	public void update_chat(String text){
		if (text.length()==0) return;
		String old = chatArea.getText();
		if(old != "" || !old. isEmpty()){
			chatArea.setText(old + '\n' + text);
		}else {
			chatArea.setText(text);
		}
	}
	
	public void welcome_chat(String name){
		if (name.length()==0) return;
		String old = chatArea.getText();
		if(old != "" || !old.isEmpty()){
			chatArea.setText(old + '\n' + name);
		}
		paintMode = PaintMode.Pixel;
	}
	
	/**
	 * change the color of a specific area
	 * @param col, row - the position of the selected pixel
	 * @return a list of modified pixels
	 * @throws IOException 
	 */
	public List paintArea(int col, int row) throws IOException {
		LinkedList<Point> filledPixels = new LinkedList<Point>();

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
		
		out.writeInt(2);
		out.writeInt(col);
		out.writeInt(row);
		out.writeInt(selectedColor);
		
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
	
	public void setUsername(String username) throws IOException{
		if(username==null) return;
		this.username = username;
		chatArea.setText("Welcome "+username+"~");
		out.writeInt(0);
		out.writeInt(username.length());
		out.write(username.getBytes(), 0, username.length());
		
	}
	
	public void save() {
	}
	
	public void reload() {
	}
}
