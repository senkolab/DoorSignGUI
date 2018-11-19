
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/**
 * Door sign GUI
 *
 * @author Rich
 */
public class TrappyTownDoorSign extends JFrame {

    private JPanel _mainpanel;
    private StatusLabel[] _indicators;

    private final int PORTNUM = 9000;
    private final String MCAST_ADDR = "230.0.0.0";
    private final String ICON_PATH="./laser.png";

    public static final int BLINK_TIME = 1000;
    public final int DEBOUNCE_TIME = 30;

    public static final int NUM_INDICATORS = 6;
    private final int[] INDICATOR_GRID = {2, 3};

    private final BroadcastSender _sender;
    private final BroadcastReceiver _receiver;
    private CenteredLabel _titleLabel;
    private JButton _testButton;

    private Timer _resendTimer;
    private final boolean _isListenerOnly;
    private boolean[] _status;

    private GpioController _gpioController;
    private GpioPinDigitalInput _gpioSwitch17;  // Pi4J pin GPIO 0
    private GpioPinDigitalInput _gpioSwitch22;  // Pi4J pin GPIO 4
    private GpioPinDigitalInput _gpioSwitch23;  // Pi4J pin GPIO 3
    private GpioPinDigitalInput _gpioSwitch27;  // Pi4J pin GPIO 2

    private int _selectedIndicator;

    /**
     * Constructor
     *
     * @param isListenerOnly
     * @param labels
     * @throws UnknownHostException
     * @throws IOException
     */
    public TrappyTownDoorSign(boolean isListenerOnly, String[] labels) throws UnknownHostException, IOException {
        _isListenerOnly = isListenerOnly;
        InetAddress mcast_addr = InetAddress.getByName(MCAST_ADDR);
        _sender = new BroadcastSender(PORTNUM, mcast_addr);
        _receiver = new BroadcastReceiver(this, PORTNUM, mcast_addr);

        InitComponents(labels);
        InitSender();
        InitGpio();
    }

    /**
     * Initialize
     */
    private void InitComponents(String[] labels) {
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);

        this._mainpanel = new JPanel();
        _mainpanel.setLayout(new GridLayout(INDICATOR_GRID[0], INDICATOR_GRID[1]));
        _indicators = new StatusLabel[NUM_INDICATORS];
        _status = new boolean[NUM_INDICATORS];
        for (int n = 0; n < _indicators.length; n++) {
            _indicators[n] = new StatusLabel(labels[n], Color.RED, ICON_PATH);
            _mainpanel.add(_indicators[n]);
        }

        this.setUndecorated(true);
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.setLayout(new BorderLayout());
        _titleLabel = new CenteredLabel("Welcome to Trappy Town", Color.GREEN);
        this.add(_titleLabel, BorderLayout.NORTH);
        this.add(_mainpanel, BorderLayout.CENTER);

        _testButton = new JButton("Test");
        _testButton.setBackground(Color.GRAY);
        if(_isListenerOnly) {
          this.add(_testButton, BorderLayout.SOUTH);
        }
        else 
    {
            JPanel southpanel = new JPanel();
            southpanel.setLayout(new GridLayout(1, 5));
            southpanel.add(new CenteredLabel("<-", Color.GRAY));
            southpanel.add(new CenteredLabel("->", Color.GRAY));
            southpanel.add(_testButton);
            southpanel.add(new CenteredLabel("", Color.GRAY));
            southpanel.add(new CenteredLabel("Toggle", Color.GRAY));
            this.add(southpanel, BorderLayout.SOUTH);
        }

        _testButton.addActionListener((ActionEvent ae) -> {
            try {
                _sender.send(new boolean[]{true, true, false, false, true, true});
            } catch (IOException ex) {
                Logger.getLogger(TrappyTownDoorSign.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent we) {
            }

            @Override
            public void windowClosing(WindowEvent we) {
                _receiver.shutdown();
            }

            @Override
            public void windowClosed(WindowEvent we) {
            }

            @Override
            public void windowIconified(WindowEvent we) {
            }

            @Override
            public void windowDeiconified(WindowEvent we) {
            }

            @Override
            public void windowActivated(WindowEvent we) {
            }

            @Override
            public void windowDeactivated(WindowEvent we) {
            }
        });
    }

    /**
     * Initialize sender if used
     */
    private void InitSender() {
        if (_isListenerOnly == false) {
            _resendTimer = new Timer(1000, (ActionEvent ae) -> {
                transmitStatus();
            });
            _resendTimer.setRepeats(true);
            _resendTimer.start();
        }
    }

    /**
     * Initialize GPIO buttons
     */
    private void InitGpio() {
        if(_isListenerOnly)
            return;

        _gpioController = GpioFactory.getInstance();

        // notice the crazy pin numbering Pi4J came up with...does not match the
        // R-Pi docs.  https://pi4j.com/pins/model-2b-rev1.html
        _gpioSwitch17 = _gpioController.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_UP);
        _gpioSwitch22 = _gpioController.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.PULL_UP);
        _gpioSwitch23 = _gpioController.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_UP);
        _gpioSwitch27 = _gpioController.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_UP);

        _gpioSwitch17.setDebounce(DEBOUNCE_TIME);
        _gpioSwitch22.setDebounce(DEBOUNCE_TIME);
        _gpioSwitch23.setDebounce(DEBOUNCE_TIME);
        _gpioSwitch27.setDebounce(DEBOUNCE_TIME);

        _gpioSwitch17.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent gpdsce) -> {
            if (gpdsce.getEdge() == PinEdge.FALLING) {
                selectPrevIndicator();
            }
        });

        _gpioSwitch22.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent gpdsce) -> {
            if (gpdsce.getEdge() == PinEdge.FALLING) {
                selectNextIndicator();
            }
        });
        _gpioSwitch27.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent gpdsce) -> {
            if (gpdsce.getEdge() == PinEdge.FALLING) {
                _status[_selectedIndicator] = !_status[_selectedIndicator];
                transmitStatus();
            }
        });

    }

    /**
     * Change button colors
     *
     * @param status
     */
    public void updateButtons(boolean[] status) {
        for (int n = 0; n < status.length; n++) {
            _status[n] = status[n];
            _indicators[n].setStatus(status[n]);
            _indicators[n].setSelected(n == _selectedIndicator);
        }
    }

    /**
     * get the next indicator
     */
    public void selectNextIndicator() {
        _selectedIndicator++;
        if (_selectedIndicator >= NUM_INDICATORS) {
            _selectedIndicator = 0;
        }

        for (int n = 0; n < NUM_INDICATORS; n++) {
            _indicators[n].setSelected(n == _selectedIndicator);
        }

    }

    /**
     * get the next indicator
     */
    public void selectPrevIndicator() {
        _selectedIndicator--;
        if (_selectedIndicator < 0) {
            _selectedIndicator = NUM_INDICATORS - 1;
        }

        for (int n = 0; n < NUM_INDICATORS; n++) {
            _indicators[n].setSelected(n == _selectedIndicator);
        }

    }

    /**
     * transmit the status
     */
    public void transmitStatus() {

        try {
            _sender.send(_status);
        } catch (IOException ex) {
            Logger.getLogger(TrappyTownDoorSign.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * indicate comm lost
     *
     * @param status
     */
    public void updateTimeout(boolean status) {
        if (status) {
            this._testButton.setBackground(Color.RED);
            this._testButton.setText("Lost COMM");
        } else {
            this._testButton.setBackground(Color.GRAY);
            if(_isListenerOnly)
                this._testButton.setText("Online");
            else
                this._testButton.setText("Online - Master");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                boolean doTransmitter;
                String[] labels = {"650nm", "493nm", "553nm", "1762nm", "614nm", "405nm"};
                doTransmitter = (args.length >= 1) && args[0].equalsIgnoreCase("Transmit");

                TrappyTownDoorSign app = new TrappyTownDoorSign(!doTransmitter, labels);
            } catch (IOException ex) {
                Logger.getLogger(TrappyTownDoorSign.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    /**
     * UDP multicast sender
     */
    protected class BroadcastSender {

        private final MulticastSocket socket;
        private final InetAddress _groupaddr;
        private final int _groupPort;

        /**
         * Constructor
         *
         * @param portnumber
         * @param groupaddress
         * @throws SocketException
         * @throws IOException
         */
        public BroadcastSender(int portnumber, InetAddress groupaddress) throws SocketException, IOException {
            socket = new MulticastSocket(portnumber);
            _groupaddr = groupaddress;
            _groupPort = portnumber;
            socket.joinGroup(groupaddress);
        }

        /**
         * Send message
         *
         * @param status
         * @throws java.io.IOException
         */
        public void send(boolean[] status) throws IOException {
            byte[] buf = encode(status);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, _groupaddr, _groupPort);
            socket.send(packet);
        }

        /**
         * Encode the boolean message
         *
         * @param status
         * @return
         */
        protected byte[] encode(boolean[] status) {
            byte[] result = new byte[status.length];
            for (int n = 0; n < result.length; n++) {
                if (status[n]) {
                    result[n] = 1;
                } else {
                    result[n] = 0;
                }
            }
            return result;
        }
    }

    /**
     * Broadcast message receiver
     */
    protected class BroadcastReceiver implements Runnable {

        /**
         * Kill variable
         */
        private boolean _shutdown = false;
        /**
         * parent object
         */
        final protected TrappyTownDoorSign _parent;

        /**
         * multicast socket
         */
        final protected MulticastSocket _socket;

        final Thread _thread;

        /**
         * Constructor
         *
         * @param parent
         * @param portnumber
         * @param groupaddress
         * @throws java.net.SocketException
         */
        public BroadcastReceiver(TrappyTownDoorSign parent, int portnumber, InetAddress groupaddress) throws SocketException, IOException {
            _socket = new MulticastSocket(portnumber);
            _socket.joinGroup(groupaddress);
            _socket.setSoTimeout(5000);
            _parent = parent;

            _thread = new Thread(this);
            _thread.start();
        }

        /**
         * Thread runner
         */
        @Override
        public void run() {
            byte[] buf = new byte[128];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (_shutdown == false) {
                try {
                    _socket.receive(packet);
                    boolean[] status = decode(packet);
                    _parent.updateButtons(status);
                    _parent.updateTimeout(false);
                } catch (IOException ex) {
                    _parent.updateTimeout(true);
                }
            }
        }

        /**
         * Decode a datagram into boolean status
         *
         * @param dp
         * @return
         */
        protected boolean[] decode(DatagramPacket dp) {
            boolean[] result = new boolean[TrappyTownDoorSign.NUM_INDICATORS];
            byte[] data = dp.getData();
            for (int n = 0; n < result.length; n++) {
                result[n] = data[n] != 0;
            }
            return result;
        }

        /**
         * Kills the thread
         */
        private void shutdown() {
            this._shutdown = true;
        }
    }

    /**
     * Centered Label
     */
    protected class CenteredLabel extends JLabel {

        /**
         * Constructor
         *
         * @param s
         * @param c color
         */
        public CenteredLabel(String s, Color c) {
            super(s, JLabel.CENTER);
            this.setBackground(c);
            this.setOpaque(true);
            this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, true));
            this.setFont(new Font(this.getFont().getName(), Font.PLAIN, 22));
        }

    }

    /**
     * Centered Label
     */
    protected final class StatusLabel extends CenteredLabel {
        protected ImageIcon img = null;
        protected Color activeBackground;
        protected Color inactiveBackground;
        protected Border activeBorder;
        protected boolean flashState;

        protected Timer timer;

        /**
         * Constructor
         *
         * @param s
         * @param c color
         */
        public StatusLabel(String s, Color c) {
            super(s, c);
            this.activeBackground = c;
            this.inactiveBackground = Color.GRAY;

            this.setFont(new Font(this.getFont().getName(), Font.PLAIN, 30));
            timer = new Timer(BLINK_TIME, (ActionEvent e) -> {
                flashState = !flashState;
                if (flashState) {
                    this.setBackground(activeBackground);
		    this.setEnabled(true);
                } else {
                    this.setBackground(inactiveBackground);
		    this.setEnabled(false);
                }
            });
            timer.setRepeats(true);
            this.setStatus(false);
        }

        /**
         * Constructor
         *
         * @param s
         * @param c color
         */
        public StatusLabel(String s, Color c, String IconPath) {
            this(s, c);

	    this.setVerticalTextPosition(JLabel.TOP);
	    this.setHorizontalTextPosition(JLabel.CENTER);
            try {
                img = new ImageIcon( ImageIO.read(new File(IconPath)) );
		setIcon(img);
            } catch (IOException ex) {
                Logger.getLogger(TrappyTownDoorSign.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


        /**
         * set the button status
         *
         * @param active
         */
        public void setStatus(boolean active) {
            if (active) {
                timer.start();
                timer.setRepeats(true);
            } else {
                timer.setRepeats(false);
                timer.stop();
                this.setBackground(inactiveBackground);
		setEnabled(false);
            }
        }

        /**
         * Set the border
         *
         * @param value
         */
        public void setSelected(boolean value) {
            if (value) {
                this.setBorder(new LineBorder(Color.BLACK, 2));
            } else {
                this.setBorder(null);
            }
        }
    }
}
