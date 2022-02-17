 // import required packages
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;

public class TelnetSim extends Frame implements ActionListener, KeyListener
{
	// t is the main textarea
	TextArea t;
	MenuBar mbar;
	MenuItem mnuItem, remoteSystem, disconnect;
	Menu m;

	//InputDialog class is used to accept Remote host's ip and port
	InputDialog idialog;

	Label status;
	String prevString;

	//ListenToClient thread listenes to the remote host continuously
	ListenToClient lt;

	BufferedReader br;
	PrintWriter pr;
	Socket s;

	//MessageBox is used to display necessary messages to the user
	MessageBox mb;

	public static void main(String [] args)
	{
		new TelnetSim();
	}

	public TelnetSim()
	{
		super("Telnet simulator 2.0");

		//start graphical user interface
		startGUI();

		//obtain host ip and port from user
		idialog = new InputDialog(this, "Connect");
		idialog.setVisible(true);

		//instantiate the MessageBox object
		mb = new MessageBox(this);
	}

	public void startGUI()
	{
		/*The setSize() function is replaced by the pack function at the
		  end which automatically sets the size of the frame
		setSize(650, 425);*/

		setResizable(false);
		setLocation(100, 100);
		setLayout(new BorderLayout());
		setBackground(new Color(192, 192, 192));
		addWindowListener(new MyWindowAdapter(true));

		t = new TextArea("", 23, 90, TextArea.SCROLLBARS_NONE);
		t.addKeyListener(this);
		add(t, BorderLayout.CENTER);
		t.setEditable(false);

		status = new Label("");
		add(status, BorderLayout.SOUTH);

		mbar = new MenuBar();

		m = new Menu("Connect");
		m.add(remoteSystem = new MenuItem("Remote system"));
		remoteSystem.addActionListener(this);
		m.add(disconnect = new MenuItem("Disconnect"));
		disconnect.setEnabled(false);
		disconnect.addActionListener(this);
		m.add(mnuItem = new MenuItem("-"));
		mnuItem.addActionListener(this);
		m.add(mnuItem = new MenuItem("Exit"));
		mnuItem.addActionListener(this);

		mbar.add(m);

		m = new Menu("Help");
		m.add(mnuItem = new MenuItem("About"));
		mnuItem.addActionListener(this);

		mbar.add(m);

		setMenuBar(mbar);

		pack();

		t.setFont(new Font("Arial", Font.PLAIN, 15));
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e)
	{
		if(e.getActionCommand().equals("Exit"))
		{

			System.exit(0);
		}
		if(e.getActionCommand().equals("Remote system"))
			idialog.setVisible(true);
		if(e.getActionCommand().equals("Disconnect"))
			disconnect();
		if(e.getActionCommand().equals("About"))
		{
			mb.setText("Designed and developed by: G. V. S. V. Kishor Babu");
			mb.setVisible(true);
		}
	}

	public void keyPressed(KeyEvent e)
	{
		String newString, currentString;

		if(e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			try
			{
				currentString = new String(t.getText());
				newString = new String(currentString.substring(prevString.length()));
				pr.println(newString);
				pr.flush();
				prevString = new String(currentString+"\n");
			}
			catch(StringIndexOutOfBoundsException ie)
			{}
		}
	}
	public void keyReleased(KeyEvent e){}
	public void keyTyped(KeyEvent e){}

	public void connect(String host, String portNumber)
	{
		int port;
		if(host.length() < 1 || portNumber.length() < 1)
		{
			mb.setText("Invalid port number or IPaddress");
			mb.setVisible(true);
			return;
		}
		status.setText("Looking for host " + host);
		try
		{
			prevString = new String("");
			port = Integer.parseInt(portNumber);
			s = new Socket(host, port);
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			pr = new PrintWriter(s.getOutputStream(), true);
			t.setEditable(true);
			status.setText("Connected to host "+s.getInetAddress().getHostAddress());
			lt = new ListenToClient();
			lt.start();
			remoteSystem.setEnabled(false);
			disconnect.setEnabled(true);
		}
		catch(NumberFormatException e)
		{
			mb.setText("Invalid port number");
			mb.setVisible(true);
			status.setText("Invalid port number");
			return;
		}
		catch(IOException e)
		{
			mb.setText("Cannot open a connection to " + host);
			mb.setVisible(true);
			status.setText("Cannot open a connection to " + host);
			return;
		}
	}

	private void disconnect()
	{
		try
		{
			status.setText("Disconnected from host "+s.getInetAddress().getHostAddress());

			s.close();

			lt.listening = false;
			if(lt.isAlive() == true)
				lt.interrupt();
			while(!lt.isInterrupted());

			t.setEditable(false);
			t.setText("");
			disconnect.setEnabled(false);
			remoteSystem.setEnabled(true);
		}
		catch(IOException e)
		{
		}
		catch(SecurityException e){System.out.println("Security exception");}
	}


	class ListenToClient extends Thread
	{
		boolean listening;
		String received;

		public ListenToClient()
		{
			listening = true;
		}

		public void run()
		{
			while(listening)
			{
				try
				{
					received = new String(br.readLine());
					prevString = new String(prevString + received+"\n");
					t.append(received+"\n");
				}
				catch(IOException e)
				{
					//System.out.println("Connection closed.");
				}
				catch(NullPointerException e)
				{
					listening = false;
					mb.setText("Connection to host lost");
					mb.setVisible(true);
					disconnect();
				}
			}
		}
	}

}

class MyWindowAdapter extends WindowAdapter
{
	Dialog d;
	boolean isFrame;

	public MyWindowAdapter(Dialog d)
	{
		isFrame = false;
		this.d = d;
	}

	public MyWindowAdapter(boolean isFrame)
	{
		this.isFrame = isFrame;
	}

	public void windowClosing(WindowEvent e)
	{
		if(isFrame)
			System.exit(0);
		else
			d.setVisible(false);
	}
}

class InputDialog extends Dialog implements ActionListener, KeyListener
{
	TextField hostName, port;
	Button connect, cancel;
	TelnetSim superFrame;

	public InputDialog(Frame f, String name)
	{

		super(f, name, true);

		superFrame = (TelnetSim)f;

		addWindowListener(new MyWindowAdapter(this));
		setSize(300, 180);
		setResizable(false);

		Point p = superFrame.getLocation();
		setLocation(p.x + 150, p.y + 150);

		setLayout(new FlowLayout());

		add(new Label("Host :"));
		hostName = new TextField(20);
		hostName.addKeyListener(this);
		add(hostName);
		add(new Label("\n\rPort :"));
		port = new TextField(20);
		port.addKeyListener(this);
		add(port);

		connect = new Button("Connect");
		connect.setBackground(Color.GREEN);
		connect.addActionListener(this);
		connect.addKeyListener(this);
		add(connect);
		cancel = new Button("Cancel");
		cancel.addActionListener(this);
		cancel.addKeyListener(this);
		add(cancel);
	}
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == cancel)
			setVisible(false);
		if(hostName.getText().length() > 0 && port.getText().length() > 0)
			if(e.getSource() == connect)
			{
				setVisible(false);
				((TelnetSim)superFrame).connect(hostName.getText().trim(), port.getText().trim());
			}
	}
	public void keyPressed(KeyEvent e)
	{
		if(e.getSource() == hostName)
		{
			if(e.getKeyCode() == KeyEvent.VK_ENTER)
				port.requestFocus();
		}
		if(e.getSource() == port)
		{
			if(e.getKeyCode() == KeyEvent.VK_ENTER)
			{
				setVisible(false);
				((TelnetSim)superFrame).connect(hostName.getText().trim(), port.getText().trim());
			}
		}
		if(e.getSource() == connect)
		{
			if(e.getKeyCode() == KeyEvent.VK_ENTER)
			{
				setVisible(false);
				((TelnetSim)superFrame).connect(hostName.getText().trim(), port.getText().trim());
			}
		}
		if(e.getSource() == cancel)
		{
			if(e.getKeyCode() == KeyEvent.VK_ENTER)
				setVisible(false);
		}
	}
	public void keyReleased(KeyEvent e){}
	public void keyTyped(KeyEvent e){}
}

class MessageBox extends Dialog implements ActionListener, KeyListener
{
	Label l;
	Button ok;
	Panel pn;
	Dimension d;
	Frame f;
	Point p;
	public MessageBox(Frame f)
	{
		super(f,"Telnet",true);
		this.f = f;
		setLayout(new BorderLayout());
		l = new Label("Connection to host lost", Label.CENTER);
		add(l, BorderLayout.NORTH);
		pn = new Panel();
		add(pn, BorderLayout.CENTER);
		pn.setLayout(new FlowLayout());
		ok = new Button("OK");
		pn.add(ok);
		ok.addActionListener(this);
		ok.addKeyListener(this);
		setResizable(false);

		addWindowListener(new MyWindowAdapter(this));
	}

	public void actionPerformed(ActionEvent e)
	{
		setVisible(false);
	}


	public void keyPressed(KeyEvent e)
	{
		if(e.getKeyCode() == KeyEvent.VK_ENTER)
			setVisible(false);
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
	}

	public void setText(String s)
	{
		Dimension itsDimension;
		l.setText(s);
		pack();
		d = f.getSize();
		p =f.getLocation();
		itsDimension = getSize();
		setLocation(p.x + (d.width - itsDimension.width)/2, p.y + (d.height - itsDimension.height)/2);
	}
}