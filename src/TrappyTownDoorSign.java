
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
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

    private final BroadcastSender _sender;
    private final BroadcastReceiver _receiver;
    private CenteredLabel _titleLabel;
    private JButton _testButton;

    private Timer _resendTimer;
    private final boolean _isListenerOnly;
    private boolean[] _status;

    private GpioController _gpio;
    private GpioPinDigitalInput _gpio17;
    private GpioPinDigitalInput _gpio22;
    private GpioPinDigitalInput _gpio23;
    private GpioPinDigitalInput _gpio27;
    

    /**
     * Constructor
     *
     * @param isListenerOnly
     * @throws UnknownHostException
     * @throws IOException
     */
    public TrappyTownDoorSign(boolean isListenerOnly) throws UnknownHostException, IOException {
        _isListenerOnly = isListenerOnly;
        InetAddress mcast_addr = InetAddress.getByName(MCAST_ADDR);
        _sender = new BroadcastSender(PORTNUM, mcast_addr);
        _receiver = new BroadcastReceiver(this, PORTNUM, mcast_addr);

        InitComponents();
        InitSender();
        InitGpio();
    }

    /**
     * Initialize
     */
    private void InitComponents() {
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);

        this._mainpanel = new JPanel();
        _mainpanel.setLayout(new GridLayout(2, 3));
        _indicators = new StatusLabel[6];
        _status = new boolean[6];
        for (int n = 0; n < _indicators.length; n++) {
            _indicators[n] = new StatusLabel("Button " + n, Color.RED);
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
        this.add(_testButton, BorderLayout.SOUTH);
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
        _gpio = GpioFactory.getInstance();
        _gpio17 = _gpio.provisionDigitalInputPin(pin)

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
            this._testButton.setText("Online");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                boolean doTransmitter;

                doTransmitter = (args.length >= 1) && args[0].equalsIgnoreCase("Transmit");

                TrappyTownDoorSign app = new TrappyTownDoorSign(!doTransmitter);
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
            boolean[] result = new boolean[6];
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
    protected class StatusLabel extends CenteredLabel {

        protected Color activeBackground;
        protected Color inactiveBackground;

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
            this.setStatus(false);

            this.setFont(new Font(this.getFont().getName(), Font.PLAIN, 30));
        }

        /**
         * set the button status
         *
         * @param active
         */
        public void setStatus(boolean active) {
            if (active) {
                this.setBackground(activeBackground);
            } else {
                this.setBackground(inactiveBackground);
            }
        }
    }
}
