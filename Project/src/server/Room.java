package server;

import java.awt.Dimension;
import java.lang.*;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.Chair;
import client.Player;
import client.Ticket;
import core.BaseGamePanel;
import core.Helpers;

public class Room extends BaseGamePanel implements AutoCloseable {
    private static SocketServer server;// used to refer to accessible server functions
    private String name;
    private final static Logger log = Logger.getLogger(Room.class.getName());

    // Commands
    private final static String COMMAND_TRIGGER = "/";
    private final static String CREATE_ROOM = "createroom";
    private final static String JOIN_ROOM = "joinroom";
    private final static String START_GAME = "startgame";
    private final static String READY = "ready";
    private final static String GUESS = "guess";
    private final static String SOLVE = "solve";
    private List<ClientPlayer> clients = new ArrayList<ClientPlayer>();
    static Dimension gameAreaSize = new Dimension(800, 800);
    private List<Chair> chairs = new ArrayList<Chair>();
    private List<Ticket> tickets = new ArrayList<Ticket>();
    private Player currentTurn;
    String playerWord ="";
    public String secretWord;
    public char wordAsChar[];

    public Room(String name, boolean delayStart) {
	super(delayStart);
	this.name = name;
	isServer = true;
    }

    public Room(String name) {
	this.name = name;
	// set this for BaseGamePanel to NOT draw since it's server-side
	isServer = true;
    }

    public static void setServer(SocketServer server) {
	Room.server = server;
    }

    public String getName() {
	return name;
    }

    private static Point getRandomStartPosition() {
	Point startPos = new Point();
	startPos.x = (int) (Math.random() * gameAreaSize.width);
	startPos.y = (int) (Math.random() * gameAreaSize.height);
	return startPos;
    }

    private void generateSeats() {
	int players = clients.size();
	final int chairs = Helpers.getNumberBetween(players, (int) (players * 1.5));
	final Dimension chairSize = new Dimension(25, 25);
	final float paddingLeft = .1f;
	final float paddingRight = .9f;
	final float paddingTop = .1f;
	final float chairSpacing = chairSize.height * 1.75f;
	final int chairHalfWidth = (int) (chairSize.width * .5);
	final int screenWidth = gameAreaSize.width;
	final int screenHeight = gameAreaSize.height;
	for (int i = 0; i < chairs; i++) {
	    Chair chair = new Chair("Chair " + (i + 1));
	    Point chairPosition = new Point();
	    if (i % 2 == 0) {
		chairPosition.x = (int) ((screenWidth * paddingRight) - chairHalfWidth);
	    }
	    else {
		chairPosition.x = (int) (screenWidth * paddingLeft);
	    }
	    chairPosition.y = (int) ((screenHeight * paddingTop) + (chairSpacing * (i / 2)));
	    chair.setPosition(chairPosition);
	    chair.setSize(chairSize.width, chairSize.height);
	    chair.setPlayer(null);
	    this.chairs.add(chair);
	}

    }

    private void syncChairs() {
	// fairest way seems to be syncing 1 chair at a time across all players
	Iterator<Chair> chairIter = chairs.iterator();
	while (chairIter.hasNext()) {
	    Chair chair = chairIter.next();
	    if (chair != null) {
		Iterator<ClientPlayer> iter = clients.iterator();
		while (iter.hasNext()) {
		    ClientPlayer cp = iter.next();
		    if (cp != null) {
			cp.client.sendChair(chair.getName(), chair.getPosition(), chair.getSize(), chair.isAvailable());
		    }
		}
	    }
	}
    }

    private void generateTickets() {
	int players = clients.size() + 1;
	final int tickets = Helpers.getNumberBetween(players, (int) (players * 1.5));
	final int screenWidth = gameAreaSize.width;
	final int screenHeight = gameAreaSize.height;
	final float paddingLeft = .3f;
	final float paddingRight = .7f;
	final float paddingTop = .3f;
	final float paddingBottom = .7f;
	Dimension ticketSize = new Dimension(30, 20);
	System.out.println("Tickets to be made: " + tickets);
	for (int i = 0; i < tickets; i++) {
	    Ticket ticket = new Ticket("#" + Helpers.getNumberBetween(1, 10));
	    Point ticketPosition = new Point();
	    ticket.setPlayer(null);
	    ticketPosition.x = Helpers.getNumberBetween((int) (screenWidth * paddingLeft),
		    (int) (screenWidth * paddingRight));
	    ticketPosition.y = Helpers.getNumberBetween((int) (screenHeight * paddingTop),
		    (int) (screenHeight * paddingBottom));
	    ticket.setPosition(ticketPosition);
	    ticket.setSize(ticketSize.width, ticketSize.height);
	    this.tickets.add(ticket);
	}
	System.out.println("Tickets made: " + this.tickets.size());
    }

    private void syncTickets() {
	// fairest way seems to be syncing 1 ticket at a time across all players
	Iterator<Ticket> ticketIter = tickets.iterator();
	while (ticketIter.hasNext()) {
	    Ticket ticket = ticketIter.next();
	    if (ticket != null) {
		Iterator<ClientPlayer> iter = clients.iterator();
		while (iter.hasNext()) {
		    ClientPlayer cp = iter.next();
		    if (cp != null) {
			cp.client.sendTicket(ticket.getName(), ticket.getPosition(), ticket.getSize(),
				ticket.isAvailable());
		    }
		}
	    }
	}
    }

    private void syncGameSize() {
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer cp = iter.next();
	    if (cp != null) {
		cp.client.sendGameAreaSize(gameAreaSize);
	    }
	}
    }

    protected synchronized void addClient(ServerThread client) {
	client.setCurrentRoom(this);
	boolean exists = false;
	// since we updated to a different List type, we'll need to loop through to find
	// the client to check against
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer c = iter.next();
	    if (c.client == client) {
		exists = true;
		if (c.player == null) {
		    log.log(Level.WARNING, "Client " + client.getClientName() + " player was null, creating");
		    Player p = new Player();
		    p.setName(client.getClientName());
		    c.player = p;
		    syncClient(c);
		}
		break;
	    }
	}

	if (exists) {
	    log.log(Level.INFO, "Attempting to add a client that already exists");
	}
	else {
	    // create a player reference for this client
	    // so server can determine position
	    Player p = new Player();
	    p.setName(client.getClientName());
	    // add Player and Client reference to ClientPlayer object reference
	    ClientPlayer cp = new ClientPlayer(client, p);
	    clients.add(cp);// this is a "merged" list of Clients (ServerThread) and Players (Player)
			    // objects
	    // that's so we don't have to keep track of the same client in two different
	    // list locations
	    syncClient(cp);

	}
    }

    private void syncClient(ClientPlayer cp) {
	if (cp.client.getClientName() != null) {
	    cp.client.sendClearList();
	    sendConnectionStatus(cp.client, true, "joined the room " + getName());
	    // calculate random start position
	    Point startPos = Room.getRandomStartPosition();
	    cp.player.setPosition(startPos);
	    cp.client.sendGameAreaSize(gameAreaSize);
	    // tell our client of our server determined position
	    cp.client.sendPosition(cp.client.getClientName(), startPos);
	    // tell everyone else about our server determiend position
	    sendPositionSync(cp.client, startPos);
	    // get the list of connected clients (for ui panel)
	    updateClientList(cp.client);
	    // get dir/pos of existing players
	    updatePlayers(cp.client);

	}
    }

    /***
     * Syncs the existing players in the room with our newly connected player
     * 
     * @param client
     */
    private synchronized void updatePlayers(ServerThread client) {
	// when we connect, send all existing clients current position and direction so
	// we can locally show this on our client
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer c = iter.next();
	    if (c.client != client) {
		boolean messageSent = client.sendDirection(c.client.getClientName(), c.player.getDirection());
		if (messageSent) {
		    messageSent = client.sendPosition(c.client.getClientName(), c.player.getPosition());
		}
	    }
	}
    }

    /**
     * Syncs the existing clients in the room with our newly connected client
     * 
     * @param client
     */
    private synchronized void updateClientList(ServerThread client) {
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer c = iter.next();
	    if (c.client != client) {
		boolean messageSent = client.sendConnectionStatus(c.client.getClientName(), true, null);
	    }
	}
    }

    protected synchronized void removeClient(ServerThread client) {
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer c = iter.next();
	    if (c.client == client) {
		iter.remove();
		log.log(Level.INFO, "Removed client " + c.client.getClientName() + " from " + getName());
	    }
	}
	if (clients.size() > 0) {
	    sendConnectionStatus(client, false, "left the room " + getName());
	}
	else {
	    cleanupEmptyRoom();
	}
    }

    private void cleanupEmptyRoom() {
	// If name is null it's already been closed. And don't close the Lobby
	if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
	    return;
	}
	try {
	    log.log(Level.INFO, "Closing empty room: " + name);
	    close();
	}
	catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    protected void joinRoom(String room, ServerThread client) {
	server.joinRoom(room, client);
    }

    protected void joinLobby(ServerThread client) {
	server.joinLobby(client);
    }

    protected void createRoom(String room, ServerThread client) {
	if (server.createNewRoom(room)) {
	    joinRoom(room, client);
	}
    }

    private ClientPlayer getCP(ServerThread client) {
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer cp = iter.next();
	    if (cp.client == client) {
		return cp;
	    }
	}
	return null;
    }

    /***
     * Helper function to process messages to trigger different functionality.
     * 
     * @param message The original message being sent
     * @param client  The sender of the message (since they'll be the ones
     *                triggering the actions)
     */
    private boolean processCommands(String message, ServerThread client) {
	boolean wasCommand = false;
	try {
	    if (message.indexOf(COMMAND_TRIGGER) > -1) {
		String[] comm = message.split(COMMAND_TRIGGER);
		log.log(Level.INFO, message);
		String part1 = comm[1];
		String[] comm2 = part1.split(" ");
		String command = comm2[0];
		if (command != null) {
		    command = command.toLowerCase();
		}
		String roomName;
		ClientPlayer cp = null;
		switch (command) {
		case CREATE_ROOM:
		    roomName = comm2[1];
		    cp = getCP(client);
		    if (cp != null) {
			createRoom(roomName, cp.client);
		    }
		    wasCommand = true;
		    break;
		case JOIN_ROOM:
		    roomName = comm2[1];
		    cp = getCP(client);
		    if (cp != null) {
			joinRoom(roomName, cp.client);
		    }
		    wasCommand = true;
		    break;
		case READY:
		    cp = getCP(client);
		    if (cp != null) {
			cp.player.setReady(true);
			readyCheck();
		    }
		    break;
		case START_GAME:
		    gameInit();
		   
		    wasCommand = true;
		    break;
		    
		case GUESS:
		    if(checkLetter(comm2[1].charAt(0))) {
		    	Player.points +=5;
		    }
		   
		    wasCommand = true;
		    break;
		case SOLVE:
			solveAttempt(comm2[1]);
			 wasCommand = true;
			    break;
		}
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
	return wasCommand;
    }
    
    
    private void gameInit() {
    	//sets points to 0 in case more than one game was played
    	Iterator<ClientPlayer> iter = clients.iterator();
    	while(iter.hasNext()) { // BROKEN
    		//Player.points = 0; BROKEN
    	}//while
    	//Get word
    	secretWord = Words.Selector();
    	serverBroadcast("Secret Word is "+secretWord);
    	for (int i=0; i<secretWord.length();i++) {
    		wordAsChar[i] = secretWord.charAt(i);
    	}
    	 
    	
    	for (int i=0; i<secretWord.length();i++) {
    		playerWord.concat("_");
    	}    	
    	serverBroadcast(playerWord);
    	serverBroadcast("Welcome to Hangman!");
    }

    private void turns() {
    	Iterator<ClientPlayer> iter = clients.iterator();
    	for (int i=0;i<clients.size();i++) {
    		//clients.Player.playerNum = i;
    	}
    }
    
    private void solveAttempt(String guess) {
		if (guess == secretWord) {
			serverBroadcast("Congrats! you did it!");
		
		}else {
			
		}
    	
    }
    
    private Boolean checkLetter(char guess) {
    	Boolean returnable = false;
    	for (int i=0; i<secretWord.length();i++) {
    		if (secretWord.charAt(i) == guess) {
    			serverBroadcast("Math Found");
    			wordAsChar[i] = secretWord.charAt(i);
    			returnable = true;
    		}else {
    			serverBroadcast("Nope!");
    			returnable = false;
    		}
    	}
		return returnable;
    	
    }
    
    private void readyCheck() {
	Iterator<ClientPlayer> iter = clients.iterator();
	int total = clients.size();
	int ready = 0;
	while (iter.hasNext()) {
	    ClientPlayer cp = iter.next();
	    if (cp != null && cp.player.isReady()) {
		ready++;
	    }
	}
	if (ready >= total && chairs.size() == 0) {
	    // start
	    System.out.println("Everyone's ready, let's do this!");
	    generateSeats();
	    generateTickets();
	    syncChairs();
	    syncTickets();
	}
    }

    protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer c = iter.next();
	    boolean messageSent = c.client.sendConnectionStatus(client.getClientName(), isConnect, message);
	    if (!messageSent) {
		iter.remove();
		log.log(Level.INFO, "Removed client " + c.client.getId());
	    }
	}
    }
    
    protected void serverBroadcast(String message) {
    	Iterator<ClientPlayer> iter = clients.iterator();
    	while (iter.hasNext()) {
    	    ClientPlayer client = iter.next();
    	    client.client.send("Server", message);
    	}
    }

    /***
     * Takes a sender and a message and broadcasts the message to all clients in
     * this room. Client is mostly passed for command purposes but we can also use
     * it to extract other client info.
     * 
     * @param sender  The client sending the message
     * @param message The message to broadcast inside the room
     */
    protected void sendMessage(ServerThread sender, String message) {
	log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
	if (processCommands(message, sender)) {
	    // it was a command, don't broadcast
	    return;
	}
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer client = iter.next();
	    boolean messageSent = client.client.send(sender.getClientName(), message);
	    if (!messageSent) {
		iter.remove();
		log.log(Level.INFO, "Removed client " + client.client.getId());
	    }
	}
    }

    /**
     * Broadcasts this client/player direction to all connected clients/players
     * 
     * @param sender
     * @param dir
     */
    protected void sendDirectionSync(ServerThread sender, Point dir) {
	boolean changed = false;
	// first we'll find the clientPlayer that sent their direction
	// and update the server-side instance of their direction
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer client = iter.next();
	    // update only our server reference for this client
	    // if we don't have this "if" it'll update all clients (meaning everyone will
	    // move in sync)
	    if (client.client == sender) {
		changed = client.player.setDirection(dir.x, dir.y);
		break;
	    }
	}
	// if the direction is "changed" (it should be, but check anyway)
	// then we'll broadcast the change in direction to all clients
	// so their local movement reflects correctly
	if (changed) {
	    iter = clients.iterator();
	    while (iter.hasNext()) {
		ClientPlayer client = iter.next();
		boolean messageSent = client.client.sendDirection(sender.getClientName(), dir);
		if (!messageSent) {
		    iter.remove();
		    log.log(Level.INFO, "Removed client " + client.client.getId());
		}
	    }

	}
    }

    /**
     * Broadcasts this client/player position to all connected clients/players
     * 
     * @param sender
     * @param pos
     */
    protected void sendPositionSync(ServerThread sender, Point pos) {
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer client = iter.next();
	    boolean messageSent = client.client.sendPosition(sender.getClientName(), pos);
	    if (!messageSent) {
		iter.remove();
		log.log(Level.INFO, "Removed client " + client.client.getId());
	    }
	}
    }

    public List<String> getRooms(String search) {
	return server.getRooms(search);
    }

    /***
     * Will attempt to migrate any remaining clients to the Lobby room. Will then
     * set references to null and should be eligible for garbage collection
     */
    @Override
    public void close() throws Exception {
	int clientCount = clients.size();
	if (clientCount > 0) {
	    log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
	    Iterator<ClientPlayer> iter = clients.iterator();
	    Room lobby = server.getLobby();
	    while (iter.hasNext()) {
		ClientPlayer client = iter.next();
		lobby.addClient(client.client);
		iter.remove();
	    }
	    log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
	}
	server.cleanupRoom(this);
	name = null;
	isRunning = false;
	// should be eligible for garbage collection now
    }

    @Override
    public void awake() {
	// TODO Auto-generated method stub

    }

    @Override
    public void start() {
	// TODO Auto-generated method stub
	log.log(Level.INFO, getName() + " start called");
    }

    long frame = 0;

    void checkPositionSync(ClientPlayer cp) {
	// determine the maximum syncing needed
	// you do NOT need it every frame, if you do it could cause network congestion
	// and
	// lots of bandwidth that doesn't need to be utilized
	if (frame % 120 == 0) {// sync every 120 frames (i.e., if 60 fps that's every 2 seconds)
	    // check if it's worth sycning the position
	    // again this is to save unnecessary data transfer
	    if (cp.player.changedPosition()) {
		sendPositionSync(cp.client, cp.player.getPosition());
	    }
	}

    }

    @Override
    public void update() {
	// We'll make the server authoritative
	// so we'll calc movement/collisions and send the action to the clients so they
	// can visually update. Client's won't be determining this themselves
	Iterator<ClientPlayer> iter = clients.iterator();
	while (iter.hasNext()) {
	    ClientPlayer p = iter.next();
	    if (p != null) {
		// have the server-side player calc their potential new position
		p.player.move();
		// determine if we should sync this player's position to all other players
		checkPositionSync(p);
	    }
	}

    }

    // don't call this more than once per frame
    private void nextFrame() {
	// we'll do basic frame tracking so we can trigger events
	// less frequently than each frame
	// update frame counter and prevent overflow
	if (Long.MAX_VALUE - 5 <= frame) {
	    frame = Long.MIN_VALUE;
	}
	frame++;
    }

    @Override
    public void lateUpdate() {
	nextFrame();
    }

    @Override
    public void draw(Graphics g) {
	// this is the server, we won't be using this unless you're adding this view to
	// the Honor's student extra section
    }

    @Override
    public void quit() {
	// don't call close here
	log.log(Level.WARNING, getName() + " quit() ");
    }

    @Override
    public void attachListeners() {
	// no listeners either since server side receives no input
    }

}