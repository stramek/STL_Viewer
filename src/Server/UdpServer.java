package Server;

import java.beans.PropertyChangeEvent;
import java.util.concurrent.ThreadFactory;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.MulticastSocket;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.LinkedList;



/**
 * <p>A robust class for establishing a Server.UDP server and manipulating
 * its listening port and optionally a multicast groups to join.
 * The {@link Event}s and property change events make
 * it an appropriate tool in a threaded, GUI application.
 * It is almost identical in design to the TcpServer class that
 * should have accompanied this class when you downloaded it.</p>
 * 
 * <p>To start a Server.UDP server, create a new Server.UdpServer and call start():</p>
 * 
 * <pre> Server.UdpServer server = new Server.UdpServer();
 * server.start();</pre>
 * 
 * <p>Of course it won't be much help unless you know which port it's
 * listening on and you register as a listener
 * so you'll know when a <tt>java.net.DatagramPacket</tt> has come in:</p>
 * 
 * <pre> server.setPort(1234);
 *  server.addUdpServerListener( new Server.UdpServer.Adapter(){
 *     public void udpServerPacketReceived( Server.UdpServer.Event evt ){
 *         DatagramPacket packet = evt.getPacket();
 *         ...
 *     }   // end packet received
 * });</pre>
 * 
 * <p>The server runs on one thread, and all events are fired on that thread.
 * If you have to offload heavy processing to another thread, be sure to
 * make a copy of the datagram data array since it will be reused the next
 * time around. You may use the {@link Event#getPacketAsBytes}
 * command as a convenient way to make a copy of the byte array.</p>
 * 
 * <p>The full 64KB allowed by the Server.UDP standard is set aside to receive
 * the datagrams, but it's possible that your host platform may truncate that.</p>
 * 
 * <p>The public methods are all synchronized on <tt>this</tt>, and great
 * care has been taken to avoid deadlocks and race conditions. That being said,
 * there may still be bugs (please contact the author if you find any), and
 * you certainly still have the power to introduce these problems yourself.</p>
 * 
 * <p>It's often handy to have your own class extend this one rather than
 * making an instance field to hold a Server.UdpServer where you'd have to
 * pass along all the setPort(...) methods and so forth.</p>
 * 
 * <p>The supporting {@link Event} and {@link Listener}
 * classes are static inner classes in this file so that you have only one
 * file to copy to your project. You're welcome.</p>
 *
 * <p>Since the TcpServer.java, Server.UdpServer.java, and NioServer.java are
 * so similar, and since lots of copying and pasting was going on among them,
 * you may find some comments that refer to TCP instead of Server.UDP or vice versa.
 * Please feel free to let me know, so I can correct that.</p>
 * 
 * <p>This code is released into the Public Domain.
 * Since this is Public Domain, you don't need to worry about
 * licensing, and you can simply copy this Server.UdpServer.java file
 * to your own package and use it as you like. Enjoy.
 * Please consider leaving the following statement here in this code:</p>
 * 
 * <p><em>This <tt>Server.UdpServer</tt> class was copied to this project from its source as
 * found at <a href="http://iharder.net" target="_blank">iHarder.net</a>.</em></p>
 *
 * @author Robert Harder
 * @author rharder@users.sourceforge.net
 * @version 0.1
 * @see UdpServer
 * @see Event
 * @see Listener
 */
public class UdpServer {
    
    private final static Logger LOGGER = Logger.getLogger(UdpServer.class.getName());
    
    /**
     * The port property <tt>port</tt> used with
     * the property change listeners and the preferences,
     * if a preferences object is given.
     */
    public final static String PORT_PROP = "port";
    private final static int PORT_DEFAULT = 8000;
    private int port = PORT_DEFAULT;
    
    
    /**
     * The multicast groups property <tt>groups</tt> used with
     * the property change listeners and the preferences,
     * if a preferences object is given. If the multicast
     * groups is null, then no multicast groups will be joined.
     */
    public final static String GROUPS_PROP = "groups";
    private final static String GROUPS_DEFAULT = null;
    private String groups = GROUPS_DEFAULT;
    
    
    
    /**
     * <p>One of four possible states for the server to be in:</p>
     * 
     * <ul>
     *  <li>STARTING</li>
     *  <li>STARTED</li>
     *  <li>STOPPING</li>
     *  <li>STOPPED</li>
     * </ul>
     */
    public static enum State { STARTING, STARTED, STOPPING, STOPPED };
    private State currentState = State.STOPPED;
    public final static String STATE_PROP = "state";
    
    
    private Collection<UdpServer.Listener> listeners = new LinkedList<UdpServer.Listener>();    // Event listeners
    private UdpServer.Event event = new UdpServer.Event(this);                                  // Shared event
    private PropertyChangeSupport propSupport = new PropertyChangeSupport(this);                // Properties
    
    private final UdpServer This = this;                                                // To aid in synchronizing
    private ThreadFactory threadFactory;                                                // Optional thread factory
    private Thread ioThread;                                                            // Performs IO
    private MulticastSocket mSocket;                                                    // The server
    private DatagramPacket packet = new DatagramPacket( new byte[64*1024], 64*1024 );   // Shared datagram


    public final static String LAST_EXCEPTION_PROP = "lastException";
    private Throwable lastException;
    
/* ********  C O N S T R U C T O R S  ******** */
    
    
    /**
     * Constructs a new Server.UdpServer that will listen on the default port 8000
     * (but not until {@link #start} is called).
     * The I/O thread will not be in daemon mode.
     */
    public UdpServer(){
    }
    
    /**
     * Constructs a new Server.UdpServer that will listen on the given port
     * (but not until {@link #start} is called).
     * The I/O thread will not be in daemon mode.
     * @param port The initial port on which to listen
     */
    public UdpServer( int port ){
        this.port = port;
    }
    
    /**
     * Constructs a new Server.UdpServer that will listen on the given port
     * (but not until {@link #start} is called). The provided
     * ThreadFactory will be used when starting and running the server.
     * @param port The initial port on which to listen
     * @param factory The thread factory used to generate a thread to run the server
     */
    public UdpServer( int port, ThreadFactory factory ){
        this.port = port;
        this.threadFactory = factory;
    }
    
    
    
    
/* ********  R U N N I N G  ******** */
    
    
    /**
     * Attempts to start the server listening and returns immediately.
     * Listen for start events to know if the server was
     * successfully started.
     * 
     * @see Listener
     */
    public synchronized void start(){
        if( this.currentState == UdpServer.State.STOPPED ){ // Only if we're stopped now
            assert ioThread == null : ioThread;             // Shouldn't have a thread

            Runnable run = new Runnable() {
                @Override
                public void run() {
                    runServer();                            // This runs for a long time
                    ioThread = null;          
                    setState( UdpServer.State.STOPPED ); // Clear thread
                }   // end run
            };  // end runnable
            
            if( this.threadFactory != null ){               // User-specified threads
                this.ioThread = this.threadFactory.newThread(run);
                
            } else {                                        // Our own threads
                this.ioThread = new Thread( run, this.getClass().getName() );   // Named
            }

            setState( UdpServer.State.STARTING );        // Update state
            this.ioThread.start();                          // Start thread
        }   // end if: currently stopped
    }   // end start
    
    
    /**
     * Attempts to stop the server, if the server is in
     * the STARTED state, and returns immediately.
     * Be sure to listen for stop events to know if the server was
     * successfully stopped.
     * 
     * @see Listener
     */
    public synchronized void stop(){
        if( this.currentState == UdpServer.State.STARTED ){         // Only if already STARTED
            setState( UdpServer.State.STOPPING );                // Mark as STOPPING
            if( this.mSocket != null ){
                this.mSocket.close();
            }   // end if: not null
        }   // end if: already STARTED
    }   // end stop
    
    
    
    
    /**
     * Returns the current state of the server, one of
     * STOPPED, STARTING, or STARTED.
     * @return state of the server
     */
    public synchronized UdpServer.State getState(){
        return this.currentState;
    }
    
    
    /**
     * Records (sets) the state and fires an event. This method
     * does not change what the server is doing, only
     * what is reflected by the currentState variable.
     * @param state The new state of the server
     */
    protected synchronized void setState( UdpServer.State state ){
        State oldVal = this.currentState;
        this.currentState = state;
        firePropertyChange(STATE_PROP,oldVal,state);
    }
    
    
    
    /**
     * Resets the server, if it is running, otherwise does nothing.
     * This is accomplished by registering as a listener, stopping
     * the server, detecting the stop, unregistering, and starting
     * the server again. It's a useful design pattern, and you may
     * want to look at the source code for this method to check it out.
     */
    public synchronized void reset(){
        switch( this.currentState ){
            case STARTED:
                this.addPropertyChangeListener(STATE_PROP, new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        State newState = (State)evt.getNewValue();
                        if( newState == State.STOPPED ){
                            UdpServer server = (UdpServer)evt.getSource();
                            server.removePropertyChangeListener(STATE_PROP,this);
                            server.start();
                        }   // end if: stopped
                    }   // end prop change
                });
                stop();
                break;
        }   // end switch
    }
    
    
    /**
     * This method starts up and listens indefinitely
     * for Server.UDP packets. On entering this method,
     * the state is assumed to be STARTING. Upon exiting
     * this method, the state will be STOPPING.
     */
    protected void runServer(){
        try{
            this.mSocket = new MulticastSocket( getPort() );                // Create server
            LOGGER.info("Server.UDP Server established on port " + getPort() );

            try{
                this.mSocket.setReceiveBufferSize( this.packet.getData().length );
                LOGGER.info("Server.UDP Server receive buffer size (bytes): " + this.mSocket.getReceiveBufferSize() );
            } catch( IOException exc ){
                int pl = this.packet.getData().length;
                int bl = this.mSocket.getReceiveBufferSize();
                LOGGER.warning(String.format( "Could not set receive buffer to %d. It is now at %d. Error: %s",
                  pl, bl, exc.getMessage() ) );
            }   // end catch
            
            String gg = getGroups();                                        // Get multicast groups
            if( gg != null ){
                String[] proposed = gg.split("[\\s,]+");                        // Split along whitespace
                for( String p : proposed ){                                     // See which ones are valid
                    try{
                        this.mSocket.joinGroup( InetAddress.getByName(p) );
                        LOGGER.info( "Server.UDP Server joined multicast group " + p );
                    } catch( IOException exc ){
                        LOGGER.warning("Could not join " + p + " as a multicast group: " + exc.getMessage() );
                    }   // end catch
                }   // end for: each proposed
            }   // end if: groups not null

            
            setState( State.STARTED );                                   // Mark as started
            LOGGER.info( "Server.UDP Server listening..." );
            
            while( !this.mSocket.isClosed() ){
                synchronized( This ){
                    if( this.currentState == State.STOPPING ){
                        LOGGER.info( "Stopping Server.UDP Server by request." );
                        this.mSocket.close();
                    }   // end if: stopping
                }   // end sync
                
                if( !this.mSocket.isClosed() ){
                    
                    ////////  B L O C K I N G
                    this.mSocket.receive(packet);
                    ////////  B L O C K I N G
                    
                    if( LOGGER.isLoggable(Level.FINE) ){
                        LOGGER.fine( "Server.UDP Server received datagram: " + packet );
                    }
                    fireUdpServerPacketReceived();
                    
                }   //end if: not closed
            }   // end while: keepGoing
            
        } catch( Exception exc ){
            synchronized( This ){
                if( this.currentState == State.STOPPING ){  // User asked to stop
                    this.mSocket.close();
                    LOGGER.info( "Udp Server closed normally." );
                } else {
                    LOGGER.log( Level.WARNING, "Server closed unexpectedly: " + exc.getMessage(), exc );
                }   // end else
            }   // end sync
            fireExceptionNotification(exc);
        } finally {
            setState( State.STOPPING );
            if( this.mSocket != null ){
                this.mSocket.close();
            }   // end if: not null
            this.mSocket = null;
        }
    }
    
/* ********  P A C K E T  ******** */    
    
    /**
     * Returns the last DatagramPacket received.
     * @return the shared DatagramPacket
     */
    public synchronized DatagramPacket getPacket(){
        return this.packet;
    }


    
    /**
     * Attempts to send a datagram packet on the active
     * server socket.
     * 
     * @param packet the packet to send
     * @throws java.io.IOException if the server throws an exception
     *         or if the server is not running (in which case
     *         there is no underlying server socket to send the datagram)
     */
    public synchronized void send( DatagramPacket packet ) throws IOException {
        if( this.mSocket == null ){
            throw new IOException("No socket available to send packet; is the server running?");
        } else {
            this.mSocket.send( packet );
        }
    }


/* ********  R E C E I V E   B U F F E R  ******** */


    /**
     * Returns the receive buffer for the underlying MulticastSocket
     * if the server is currently running (otherwise there is no
     * MulticastSocket to query). Please see the javadocs for
     * java.net.MulticastSocket for more information.
     * 
     * @return receive buffer size
     * @throws java.net.SocketException
     */
    public synchronized int getReceiveBufferSize() throws SocketException{
        if( this.mSocket == null ){
            throw new SocketException("getReceiveBufferSize() cannot be called when the server is not started.");
        } else {
            return this.mSocket.getReceiveBufferSize();
        }
    }   // end getReceiveBufferSize

    /**
     * Recommends a receive buffer size for the underlying MulticastSocket.
     * Please see the javadocs for
     * java.net.MulticastSocket for more information.
     * 
     * @param size
     * @throws java.net.SocketException
     */
    public synchronized void setReceiveBufferSize(int size) throws SocketException{
        if( this.mSocket == null ){
            throw new SocketException("setReceiveBufferSize(..) cannot be called when the server is not started.");
        } else {
            this.mSocket.setReceiveBufferSize(size);
        }
    }   // end setReceiveBufferSize



    
    
    
/* ********  P O R T  ******** */
    
    /**
     * Returns the port on which the server is or will be listening.
     * @return The port for listening.
     */
    public synchronized int getPort(){
        return this.port;
    }
    
    /**
     * Sets the new port on which the server will attempt to listen.
     * If the server is already listening, then it will attempt to
     * restart on the new port, generating start and stop events.
     * @param port the new port for listening
     * @throws IllegalArgumentException if port is outside 0..65535
     */
    public synchronized void setPort( int port ){
        if( port < 0 || port > 65535 ){
            throw new IllegalArgumentException( "Cannot set port outside range 0..65535: " + port );
        }   // end if: port outside range
        
            
        int oldVal = this.port;
        this.port = port;
        if( getState() == State.STARTED ){
            reset();
        }   // end if: is running

        firePropertyChange( PORT_PROP, oldVal, port  );
    }   
    
    
    
/* ********  M U L T I C A S T   G R O U P  ******** */
    
    /**
     * Returns the multicast groups to which the server has joined.
     * May be null.
     * @return The multicast groups
     */
    public synchronized String getGroups(){
        return this.groups;
    }
    
    /**
     * <p>Sets the new multicast groups to which the server will join.
     * If the server is already listening, then it will attempt to
     * restart, generating start and stop events. </p>
     *
     * <p>The list of groups may be whitespace- and/or comma-separated.
     * When the server starts up (or restarts), the list will be
     * parsed, and only legitimate groups will actually be joined.</p>
     * May be null.
     * 
     * @param group the new groups to join
     */
    public synchronized void setGroups( String group ){

        String oldVal = this.groups;
        this.groups = group;
        if( getState() == State.STARTED ){
            reset();
        }   // end if: is running

        firePropertyChange( GROUPS_PROP, oldVal, this.groups  );
    }   
    
    
/* ********  E V E N T S  ******** */
    
    

    /** Adds a {@link Listener}.
     * @param l the Server.UdpServer.Listener
     */
    public synchronized void addUdpServerListener(UdpServer.Listener l) {
        listeners.add(l);
    }

    /** Removes a {@link Listener}.
     * @param l the Server.UdpServer.Listener
     */
    public synchronized void removeUdpServerListener(UdpServer.Listener l) {
        listeners.remove(l);
    }
    
    
    /** 
     * Fires event on calling thread for a new packet coming in.
     */
    protected synchronized void fireUdpServerPacketReceived() {
        
        UdpServer.Listener[] ll = listeners.toArray(new UdpServer.Listener[ listeners.size() ] );
        for( UdpServer.Listener l : ll ){
            try{
                l.packetReceived(event);
            } catch( Exception exc ){
                LOGGER.warning("Server.UdpServer.Listener " + l + " threw an exception: " + exc.getMessage() );
                fireExceptionNotification(exc);
            }   // end catch
        }   // end for: each listener
     }  // end fireUdpServerPacketReceived
    
    
    
    
    
    
/* ********  P R O P E R T Y   C H A N G E  ******** */
    
    
    /**
     * Fires property chagne events for all current values
     * setting the old value to null and new value to the current.
     */
    public synchronized void fireProperties(){
        firePropertyChange( PORT_PROP, null, getPort()  );      // Port
        firePropertyChange( GROUPS_PROP, null, getGroups()  );    // Multicast groups
        firePropertyChange( STATE_PROP, null, getState()  );      // State
    }
    
    
    /**
     * Fire a property change event on the current thread.
     * 
     * @param prop      name of property
     * @param oldVal    old value
     * @param newVal    new value
     */
    protected synchronized void firePropertyChange( final String prop, final Object oldVal, final Object newVal ){
        try{
            propSupport.firePropertyChange(prop,oldVal,newVal);
        } catch( Exception exc ){
            LOGGER.log(Level.WARNING,
                    "A property change listener threw an exception: " + exc.getMessage()
                    ,exc);
            fireExceptionNotification(exc);
        }   // end catch
    }   // end fire
    
    
    
    /**
     * Add a property listener.
     * @param listener the property change listener
     */
    public synchronized void addPropertyChangeListener( PropertyChangeListener listener ){
        propSupport.addPropertyChangeListener(listener);
    }

    
    /**
     * Add a property listener for the named property.
     * @param property the sole property name for which to register
     * @param listener the property change listener
     */
    public synchronized void addPropertyChangeListener( String property, PropertyChangeListener listener ){
        propSupport.addPropertyChangeListener(property,listener);
    }
    
    
    /**
     * Remove a property listener.
     * @param listener the property change listener
     */
    public synchronized void removePropertyChangeListener( PropertyChangeListener listener ){
        propSupport.removePropertyChangeListener(listener);
    }

    
    /**
     * Remove a property listener for the named property.
     * @param property the sole property name for which to stop receiving events
     * @param listener the property change listener
     */
    public synchronized void removePropertyChangeListener( String property, PropertyChangeListener listener ){
        propSupport.removePropertyChangeListener(property,listener);
    }
    


/* ********  E X C E P T I O N S  ******** */


    /**
     * Returns the last exception (Throwable, actually)
     * that the server encountered.
     * @return last exception
     */
    public synchronized Throwable getLastException(){
        return this.lastException;
    }

    /**
     * Fires a property change event with the new exception.
     * @param t
     */
    protected void fireExceptionNotification( Throwable t ){
        Throwable oldVal = this.lastException;
        this.lastException = t;
        firePropertyChange( LAST_EXCEPTION_PROP, oldVal, t );
    }

    
    
    
    
/* ********  L O G G I N G  ******** */
    
    /**
     * Static method to set the logging level using Java's
     * <tt>java.util.logging</tt> package. Example:
     * <code>Server.UdpServer.setLoggingLevel(Level.OFF);</code>.
     * 
     * @param level the new logging level
     */
    public static void setLoggingLevel( Level level ){
        LOGGER.setLevel(level);
    }
    
    /**
     * Static method returning the logging level using Java's
     * <tt>java.util.logging</tt> package.
     * @return the logging level
     */
    public static Level getLoggingLevel(){
        return LOGGER.getLevel();
    }
    
    
    
    
    
    
    
    
/* ********                                                          ******** */
/* ********                                                          ******** */    
/* ********   S T A T I C   I N N E R   C L A S S   L I S T E N E R  ******** */
/* ********                                                          ******** */
/* ********                                                          ******** */    
    
    
    
    
    
    /**
     * An interface for listening to events from a {@link UdpServer}.
     * A single {@link Event} is shared for all invocations
     * of these methods.
     * 
     * <p>This code is released into the Public Domain.
     * Since this is Public Domain, you don't need to worry about
     * licensing, and you can simply copy this Server.UdpServer.java file
     * to your own package and use it as you like. Enjoy.
     * Please consider leaving the following statement here in this code:</p>
     * 
     * <p><em>This <tt>Server.UdpServer</tt> class was copied to this project from its source as
     * found at <a href="http://iharder.net" target="_blank">iHarder.net</a>.</em></p>
     *
     * @author Robert Harder
     * @author rharder@users.sourceforge.net
     * @version 0.1
     * @see UdpServer
     * @see Event
     */
    public static interface Listener extends java.util.EventListener {

        /**
         * Called when a packet is received. This is called on the IO thread,
         * so don't take too long, and if you want to offload the processing
         * to another thread, be sure to copy the data out of the datagram
         * since it will be clobbered the next time around.
         * 
         * @param evt the event
         * @see Event#getPacket
         */
        public abstract void packetReceived( UdpServer.Event evt );


    }   // end inner static class Listener

    
    

/* ********                                                        ******** */
/* ********                                                        ******** */    
/* ********   S T A T I C   I N N E R   C L A S S   A D A P T E R  ******** */
/* ********                                                        ******** */
/* ********                                                        ******** */    
    



    /**
     * A helper class that implements all methods of the
     * {@link UdpServer.Listener} interface with empty methods.
     * 
     * <p>This code is released into the Public Domain.
     * Since this is Public Domain, you don't need to worry about
     * licensing, and you can simply copy this Server.UdpServer.java file
     * to your own package and use it as you like. Enjoy.
     * Please consider leaving the following statement here in this code:</p>
     * 
     * <p><em>This <tt>Server.UdpServer</tt> class was copied to this project from its source as
     * found at <a href="http://iharder.net" target="_blank">iHarder.net</a>.</em></p>
     *
     * @author Robert Harder
     * @author rharder@users.sourceforge.net
     * @version 0.1
     * @see UdpServer
     * @see Listener
     * @see Event
     */
//    public class Adapter implements Server.UdpServer.Listener {


        /**
         * Empty call for {@link UdpServer.Listener#udpServerPacketReceived}.
         * @param evt the event
         */
//        @Override
//        public void udpServerPacketReceived( Server.UdpServer.Event evt ) {}

//    }   // end static inner class Adapter
    
    
/* ********                                                    ******** */
/* ********                                                    ******** */    
/* ********   S T A T I C   I N N E R   C L A S S   E V E N T  ******** */
/* ********                                                    ******** */
/* ********                                                    ******** */    
    
    

    /**
     * An event representing activity by a {@link UdpServer}.
     * 
     * <p>This code is released into the Public Domain.
     * Since this is Public Domain, you don't need to worry about
     * licensing, and you can simply copy this Server.UdpServer.java file
     * to your own package and use it as you like. Enjoy.
     * Please consider leaving the following statement here in this code:</p>
     * 
     * <p><em>This <tt>Server.UdpServer</tt> class was copied to this project from its source as
     * found at <a href="http://iharder.net" target="_blank">iHarder.net</a>.</em></p>
     *
     * @author Robert Harder
     * @author rharder@users.sourceforge.net
     * @version 0.1
     * @see UdpServer
     * @see Listener
     */
    public static class Event extends java.util.EventObject {

        private final static long serialVersionUID = 1;

        /**
         * Creates a Event based on the given {@link UdpServer}.
         * @param src the source of the event
         */
        public Event( UdpServer src ){
            super(src);
        }

        /**
         * Returns the source of the event, a {@link UdpServer}.
         * Shorthand for <tt>(Server.UdpServer)getSource()</tt>.
         * @return the server
         */
        public UdpServer getUdpServer(){
            return (UdpServer)getSource();
        }

        /**
         * Shorthand for <tt>getUdpServer().getState()</tt>.
         * @return the state of the server
         * @see UdpServer.State
         */
        public UdpServer.State getState(){
            return getUdpServer().getState();
        }


        /**
         * Returns the most recent datagram packet received
         * by the {@link UdpServer}. Shorthand for
         * <tt>getUdpServer().getPacket()</tt>.
         * @return the most recent datagram
         */
        public DatagramPacket getPacket(){
            return getUdpServer().getPacket();
        }

        /**
         * Copies and returns the bytes in the most recently
         * received packet, or null if not available.
         * @return a copy of the datagram's byte array
         */
        public byte[] getPacketAsBytes(){
            DatagramPacket packet = getPacket();
            if( packet == null ){
                return null;
            } else {
                byte[] data = new byte[ packet.getLength() ];
                System.arraycopy(
                  packet.getData(), packet.getOffset(),
                  data, 0, data.length );
                return data;
            }   // end else
        }   // end getPacketAsBytes


        /**
         * Returns the data in the most recently-received
         * packet as if it were a String
         * or null if not available.
         * @return The datagram as a string
         */
        public String getPacketAsString(){
            DatagramPacket packet = getPacket();
            if( packet == null ){
                return null;
            } else {
                String s = new String(
                  packet.getData(), 
                  packet.getOffset(),
                  packet.getLength() );
                return s;
            }   // end else
        }


        /**
         * Convenience method for sending datagram packets,
         * intended to be used for replying to the sender but
         * could be used for anything. Equivalent to
         * <code>evt.getUdpServer.send( packet )</code>.
         * 
         * @param packet the packet to send
         * @throws java.io.IOException if the server throws an exception
         *         or if the server is not running (in which case
         *         there is no underlying server socket to send the datagram)
         */
        public void send( DatagramPacket packet ) throws IOException {
            this.getUdpServer().send( packet );
        }

    }   // end static inner class Event

    

}   // end class Server.UdpServer
