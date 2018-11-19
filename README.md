# DoorSignGUI
Laser Active Door Sign

This project is to develop a graphical user interface (GUI) for a "Laser Active" door sign.  The platform is the Raspberry Pi model B, using the AdaFruit Industries 2.2" PiTFT display.  The development environment is Java using the Swing components and the Pi4J GPIO library.

The system allows a remote display to signal use of high-power lasers to persons before they enter the lab.  Since our lab has several different visible wavelengths, which laser is being used is of importance.  One cannot use a single set of safety goggles to cover all wavelengths simultaneously.  Therefore this notification system also can inform a user which goggles to use.

## System
The system comprises at least two Raspberry Pi units, one functioning as a control master.  All units are "listeners" in that they all display the same graphical information.  The control master can also change functions.  Additionally the control master periodically rebroadcasts status.  All listeners when receiving the status message update their displays.  "Listener only" units cannot make changes and this is intended for e.g. the door panel itself, where changes are not usually made.  Control units who are not listen-only can be placed around the optics table for easy access to make changes.

## Communications
The system is intended to be used on a closed routable network.  Control messages are sent via UDP multicast on group 230.0.0.0 and UDP port 9000.  Multicast messages are not routed, and so the messages are contained in the lab network and do not pass outside the firewall by standard TCP/IP convention.  They are however passed by hubs and switches, thus they can be shared on the internal network.  The control master periodically rebroadcasts the entire status set (a datagram of all active/inactive lasers).  This allows a constant re-sync of all controllers, as well as the ability to detect lost communications.
