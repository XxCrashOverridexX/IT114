package client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import core.BaseGamePanel;

public class GamePanel extends BaseGamePanel implements Event {

    /**
     * 
     */
    private static final long serialVersionUID = -1121202275148798015L;
    List<Player> players;
    Player myPlayer;
    String playerUsername;// caching it so we don't lose it when room is wiped
    String playerColor;
    List<Chair> chairs;
    List<Ticket> tickets;
    private final static Logger log = Logger.getLogger(GamePanel.class.getName());
    Dimension gameAreaSize = new Dimension();

    public void setPlayerName(String name) {
	playerUsername = name;
	if (myPlayer != null) {
	    myPlayer.setName(playerUsername);
	}
    }
	public void setPlayerColor(String color) {
		playerColor = color;
		if (myPlayer != null) {
		    myPlayer.setColor(playerColor);
		}
    }

    @Override
    public synchronized void onClientConnect(String clientName, String message) {
	// TODO Auto-generated method stub
	System.out.println("Connected on Game Panel: " + clientName);
	boolean exists = false;
	Iterator<Player> iter = players.iterator();
	while (iter.hasNext()) {
	    Player p = iter.next();
	    if (p != null && p.getName().equalsIgnoreCase(clientName)) {
		exists = true;
		break;
	    }
	}
	if (!exists) {
	    Player p = new Player();
	    p.setName(clientName);
	   
	    players.add(p);
	    // want .equals here instead of ==
	    // https://www.geeksforgeeks.org/difference-equals-method-java/
	    if (clientName.equals(playerUsername)) {
		System.out.println("Reset myPlayer");
		myPlayer = p;
	    }
	}
    }

    @Override
    public void onClientDisconnect(String clientName, String message) {
	System.out.println("Disconnected on Game Panel: " + clientName);
	Iterator<Player> iter = players.iterator();
	while (iter.hasNext()) {
	    Player p = iter.next();
	    if (p != null && !p.getName().equals(playerUsername) && p.getName().equalsIgnoreCase(clientName)) {
		iter.remove();
		break;
	    }
	}
    }

    @Override
    public void onMessageReceive(String clientName, String message) {
	// TODO Auto-generated method stub
	System.out.println("Message on Game Panel");

    }

    @Override
    public void onChangeRoom() {
	// don't clear, since we're using iterators to loop, remove via iterator
	// players.clear();
	Iterator<Player> iter = players.iterator();
	while (iter.hasNext()) {
	    iter.next();
	    iter.remove();
	}
	myPlayer = null;
	System.out.println("Cleared players");
    }

    @Override
    public void awake() {
	players = new ArrayList<Player>();
	chairs = new ArrayList<Chair>();
	tickets = new ArrayList<Ticket>();
	GamePanel gp = this;
	// fix the loss of focus when typing in chat
	addMouseListener(new MouseAdapter() {

	    @Override
	    public void mousePressed(MouseEvent e) {
		gp.getRootPane().grabFocus();
	    }
	});
    }

    @Override
    public void start() {
	// TODO goes on server side, here for testing

    }

    @Override
    public void update() {
	applyControls();
	localMovePlayers();
    }

    /**
     * Gets the current state of input to apply movement to our player
     */
    private void applyControls() {
	if (myPlayer != null) {
	    int x = 0, y = 0;
	    if (KeyStates.W) {
		y = -1;
	    }
	    if (KeyStates.S) {
		y = 1;
	    }
	    if (!KeyStates.W && !KeyStates.S) {
		y = 0;
	    }
	    if (KeyStates.A) {
		x = -1;
	    }
	    else if (KeyStates.D) {
		x = 1;
	    }
	    if (!KeyStates.A && !KeyStates.D) {
		x = 0;
	    }
	    boolean changed = myPlayer.setDirection(x, y);
	    if (changed) {
		// only send data if direction changed, otherwise we're creating unnecessary
		// network traffic
		System.out.println("Direction changed");
		SocketClient.INSTANCE.syncDirection(new Point(x, y));
	    }
	}
    }

    /**
     * This is just an estimate/hint until we receive a position sync from the
     * server
     */
    private void localMovePlayers() {
	Iterator<Player> iter = players.iterator();
	while (iter.hasNext()) {
	    Player p = iter.next();
	    if (p != null) {
		p.move();
	    }
	}
    }

    @Override
    public void lateUpdate() {
	// stuff that should happen at a slightly different time than stuff in normal
	// update()

    }

    @Override
    public synchronized void draw(Graphics g) {
	setBackground(Color.WHITE);
	((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	drawChairs(g);
	drawTickets(g);
	drawPlayers(g);
	drawText(g);
	drawUI((Graphics2D) g);
    }

    private synchronized void drawChairs(Graphics g) {
	Iterator<Chair> iter = chairs.iterator();
	while (iter.hasNext()) {
	    Chair c = iter.next();
	    if (c != null) {
		c.draw(g);
	    }
	}
    }

    private synchronized void drawTickets(Graphics g) {
	Iterator<Ticket> iter = tickets.iterator();
	while (iter.hasNext()) {
	    Ticket t = iter.next();
	    if (t != null) {
		t.draw(g);
	    }
	}
    }

    private synchronized void drawPlayers(Graphics g) {
	Iterator<Player> iter = players.iterator();
	while (iter.hasNext()) {
	    Player p = iter.next();
	    if (p != null) {
		p.draw(g);
	    }
	}
    }

    private void drawText(Graphics g) {
	g.setColor(Color.BLACK);
	g.setFont(new Font("Monospaced", Font.PLAIN, 12));
	if (myPlayer != null) {
	   g.drawString(myPlayer.toString(), 10, 20);
	}

    }

    private void drawUI(Graphics2D g2) {
	Stroke oldStroke = g2.getStroke();
	g2.setStroke(new BasicStroke(2));
	g2.drawRect(0, 0, gameAreaSize.width, gameAreaSize.height);
	g2.setStroke(oldStroke);
    }
    

    @Override
    public void quit() {
	log.log(Level.INFO, "GamePanel quit");
	this.removeAll();
    }

 

    @Override
    public void onGetRoom(String roomName) {
	// TODO Auto-generated method stub

    }

    @Override
    public void onResize(Point p) {
	// TODO Auto-generated method stub
	gameAreaSize = new Dimension(p.x, p.y);
	this.setPreferredSize(gameAreaSize);
	this.setMinimumSize(gameAreaSize);
	this.setMaximumSize(gameAreaSize);
	this.setSize(gameAreaSize);
	System.out.println(this.getSize());
	this.invalidate();
	this.repaint();
    }

    @Override
    public void onGetChair(String chairName, Point position, Point dimension, boolean isAvailable) {
	// TODO Auto-generated method stub
	boolean exists = false;
	System.out.println("Available " + (isAvailable ? "true" : "false"));
	Iterator<Chair> iter = chairs.iterator();
	while (iter.hasNext()) {
	    Chair c = iter.next();
	    if (c.getName().equalsIgnoreCase(chairName)) {
		exists = true;
		// for now will fill in player as empty player so it's !null
		// the player set only matters for the server
		if (isAvailable) {
		    c.setPlayer(null);
		}
		else {
		    c.setPlayer(new Player());
		}
		break;
	    }
	}
	if (!exists) {
	    Chair c = new Chair(chairName);
	    c.setPosition(position);
	    c.setSize(dimension.x, dimension.y);
	    if (isAvailable) {
		c.setPlayer(null);
	    }
	    else {
		c.setPlayer(new Player());
	    }
	    chairs.add(c);
	}
    }

    @Override
    public void onResetChairs() {
	// TODO Auto-generated method stub
	Iterator<Chair> iter = chairs.iterator();
	while (iter.hasNext()) {
	    Chair c = iter.next();
	    c.setPlayer(null);
	    iter.remove();
	}
    }

    @Override
    public void onGetTicket(String ticketName, Point position, Point dimension, boolean isAvailable) {
	// TODO Auto-generated method stub
	boolean exists = false;
	Iterator<Ticket> iter = tickets.iterator();
	while (iter.hasNext()) {
	    Ticket t = iter.next();
	    if (t.getName().equalsIgnoreCase(ticketName)) {
		exists = true;
		// for now will fill in player as empty player so it's !null
		// the player set only matters for the server
		if (isAvailable) {
		    t.setPlayer(null);
		}
		else {
		    t.setPlayer(new Player());
		}
		break;
	    }
	}
	if (!exists) {
	    Ticket t = new Ticket(ticketName);
	    t.setPosition(position);
	    t.setSize(dimension.x, dimension.y);
	    if (isAvailable) {
		t.setPlayer(null);
	    }
	    else {
		t.setPlayer(new Player());
	    }
	    tickets.add(t);
	}
    }

    @Override
    public void onResetTickets() {
	// TODO Auto-generated method stub
	Iterator<Ticket> iter = tickets.iterator();
	while (iter.hasNext()) {
	    Ticket t = iter.next();
	    if (t.holder != null) {
		t.holder.takeTicket();
	    }
	    t.setPlayer(null);
	    iter.remove();
	}
    }
    
    

	@Override
	public void onSyncDirection(String clientName, Point direction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSyncPosition(String clientName, Point position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void attachListeners() {
		// TODO Auto-generated method stub
		
	}
}