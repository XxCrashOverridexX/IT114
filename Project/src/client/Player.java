package client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.io.Serializable;

import core.GameObject;

public class Player extends GameObject implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -6088251166673414031L;
    public static int points = 0;
    Color color = Color.RED;
    Point nameOffset = new Point(-5, -30);
    int ticket = -1;
    boolean isReady = false;
    public int playerNum;

    public void setReady(boolean r) {
	isReady = r;
    }

    public boolean isReady() {
	return isReady;
    }

    public boolean hasTicket() {
	return ticket > -1;
    }

    public void setTicket(int n) {
	ticket = n;
    }

    public int takeTicket() {
	int t = ticket;
	ticket = -1;
	return t;
    }
    

 

    @Override
    public String toString() {
	return String.format("Name: %s, p: (%d,%d), s: (%d, %d), d: (%d, %d), isAcitve: %s", name, position.x,
		position.y, speed.x, speed.y, direction.x, direction.y, isActive);
    }
}