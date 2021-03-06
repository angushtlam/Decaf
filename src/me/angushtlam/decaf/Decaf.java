package me.angushtlam.decaf;

import java.awt.Point;

import me.angushtlam.decaf.input.Pointer;
import me.angushtlam.decaf.window.WindowKeyboardListener;
import me.angushtlam.decaf.window.WindowMouseListener;
import me.angushtlam.decaf.window.WindowWrapper;

public abstract class Decaf implements Runnable {
	public static Decaf engine;
	private boolean isRunning; // Used to determine if the game is currently running.
	
	// These are final variables so we don't have to worry about any other ups/fps for now. We can eventually get it working.
	public static final double FPS_TARGET = 60D; // Amount of frame rendering each second.
	public static final double OPTIMAL_FPS_TIME = 1000000000 / FPS_TARGET; // Determines how many ns are in between frame rendering.
	
	public int UPS = 0;
	public int FPS = 0;
	
	private String title;
	private int width, height;
	
	private WindowWrapper window; // Visual-related class helper.
	private Thread loopThread;
	
	// Sets the window's name, width, and height on creation of this object.
	public Decaf(String title, int width, int height) {
		this.title = title;
		this.width = width;
		this.height = height;
	}
	
	// Run this to start the engine.
	public synchronized void start() {
		// Store this class to allow other classes to access it's dynamic functions and variables.
		engine = this;
		
		// Create new publisher objects through the PublisherHandler class.
		PublisherHandler.startup();
		InternalSubscriberHandler.startup();
		
		window = new WindowWrapper(title, width, height); // Create a new window for the program.
		
		// Attach all the listeners to the window's canvas.
		window.getCanvas().addMouseListener(new WindowMouseListener());
		window.getCanvas().addKeyListener(new WindowKeyboardListener());

		// Set this to running.
		isRunning = true;
		
		// Creates a new Thread that runs the run() function in this file.
		loopThread = new Thread(Decaf.this);
		loopThread.start();
	}

	// Ran when the program stops (hopefully ran after a crash).
	public synchronized void stop() {
		isRunning = false;
		
		try {
			loopThread.join(); // Wait for Thread to die.
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
	
	// This function is ran when this class is started as a Thread
	public void run() {
		long prevTime = System.nanoTime();
		long clockCounter = System.currentTimeMillis(); // Used to check what the application's FPS and UPS is.
		
		long lag = 0; // Determine how many ns we're behind and apply additional updating if needed.
		
		int updates = 0, frames = 0;
		
		while (isRunning) { // Running loop.
			long currTime = System.nanoTime(); // Find the new time in this new loop.
			long elapsed = currTime - prevTime; // Find the amount of ns passed since the last check.
			
			prevTime = currTime; // Switch the old time with the current time for the next loop.
			lag += elapsed;
			
			process();
			
			// This updates the game accordingly if the system is falling behind the clock, and applies the needed updates.
			// This shouldn't be a problem in smaller games.
			while (lag >= OPTIMAL_FPS_TIME) {
				update();
				lag -= OPTIMAL_FPS_TIME;
				updates++;
			}
			
			render();
			frames++;
			
			// Updates the class's UPS and FPS internal timer.
			long clockNow = System.currentTimeMillis();
			if (clockNow >= clockCounter + 1000) { // Checks if a second has passed.
				clockCounter = clockNow;
				
				FPS = frames;
				UPS = updates;
				
				frames = 0;
				updates = 0;
			}
			
			// Prevent the program from running too fast.
			try {
				Thread.sleep(Math.max(0, (long) ((System.nanoTime() - prevTime + OPTIMAL_FPS_TIME) / 1000000)));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		
	}

	// This function should not be overwritten by any subclasses, as they handle all of the events that are called. 
	// This function fires custom input events that the program should use to detect changes.
	private final void process() {
		// Keyboard and mouse inputs are detected by their respective listeners by Java.
		
		// Update the location of the mouse cursor in the static Pointer class.
		Point pos = window.getCanvas().getMousePosition();
		
		if (pos == null) {
			Pointer.setOffScreen(true);
		} else {
			Pointer.setOffScreen(false);
			Pointer.setX(pos.x);
			Pointer.setY(pos.y);
		}
		
	}

	private final void update() {
		PublisherHandler.UPDATE_TICK.alert();
	}
	
	private final void render() {
		PublisherHandler.RENDER_TICK.alert(this.window.getGraphics(), this.window.getBuffer());
	}
	

	public String getTitle() {
		return title;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
}
