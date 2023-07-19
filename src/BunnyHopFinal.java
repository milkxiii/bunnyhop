// bunnyhop!
// click on tiles to connect a path from the bunny (bunbun) to the lilypad! walk bunbun through the path without bumping into the cows (connor and bonnor) in the shortest time possible!

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;

@SuppressWarnings("serial")
public class BunnyHopFinal extends JPanel implements Runnable, MouseListener, KeyListener, MouseMotionListener{
	// values for game board
	static final int SQUARE_SIZE = 77;
	static final int TOP_OFFSET = 28;
	static final int SIDE_OFFSET = 133;
	static final int EMPTY = -1;
	static final int STRAIGHT = 1;
	static final int L_SHAPE = 2;
	static final int ROTATE_0 = 0;
	static final int ROTATE_90 = 1;
	static final int ROTATE_180 = 2;
	static final int ROTATE_270 = 3;

	// game states
	static final int HOME = 0;
	static final int LEVEL_ONE = 1;
	static final int LEVEL_TWO = 2;
	static final int LEVEL_THREE = 3;
	static final int LEVEL_FOUR = 4;
	static final int ENTER_NAME = 5;
	static final int LEVEL_SELECTION = 6;
	static final int WINNING = 7;

	int leaderboardLevel = 1; // level displayed currently on leaderboard
	int currentLevel = 1; // level that player is currently on
	int gameState = HOME;
	String playerName = "";

	// mini game states (mainly popups)
	boolean paused = false;
	boolean showLeaderboard, showRules, showCredits = false;

	// moving background
	int backgroundX = 0;  // reference point for moving background
	int width = 630;
	int speed = 1;

	// jframe
	static JFrame frame;
	
	// paths + orientations
	static int[][] correctPath; // tracks correct path of each coordinate - 1 = straight, 2 = L-shaped, -1 = empty (no path)
	static int[][] playerPath; // tracks player's given path of each coordinate 
	static int[][] correctOrientation; // tracks correct orientation of each coordinate
	static int [][] playerOrientation; // tracks player's rotated orientation of each coordinate - values 0, 1, 2, 3 indicate 0, 90, 180, 270 degrees clockwise
	static int [][] givenOrientation; // tracks player's given orientation of each coordinate
	int[][] pathColours = new int[8][8]; // 0 for default colour (light), 1 for hovered colour (dark) for each path coordinate

	// times/threads
	int timeout;
	int threadCount = 0;
	int hundredthSeconds = 0; // keeps track of time

	// directions
	boolean up, down, left, right;
	
	// bunny + cow positions
	int bunnyNo = 0;
	int bunnyDir = 0;
	int bunnyCol = 8;
	int bunnyRow = 0;
	int bunnyX = 705;
	int bunnyY = 20;
	int moveState = 0;
	int cowNo = 3;
	int cow1x = 120;
	int cow1y = 180;
	int cow2x = 400;
	int cow2y = 380;

	// more booleans
	boolean pathCompleted = false; // if player successfuly connects the correct path
	boolean bunnyCompleted = false; // if bunny reaches ending lilypad
	boolean bunnyWalking = false; // if bunny is walking
	boolean cowWalking = true; // if cow is walking
	boolean collision = false; // if bunny bumps into cow
	boolean allowedToMove = true; // makes sure player releases a key before trying to make bunny move again
	boolean gameInProgress = false; // if gamestate is 1, 2, 3, or 4 (one of the levels)
	boolean selectOwnLevel = false; // if player clicked star button to choose own level
	
	// keeps track of which button sprite is used (0 is normal, 1 is hovered/dark button)
	int homeButtonOption, levelOneButtonOption, levelTwoButtonOption, levelThreeButtonOption, levelFourButtonOption, winningScreenOption, checkButtonOption, pauseButtonOption, leaderboardButtonOption, levelsButtonOption, xButtonOption, restartButtonOption, rightArrowOption, leftArrowOption, rulesScreenOption, playButtonOption, rulesButtonOption, exitButtonOption, infoButtonOption = 0;

	// leaderboard/ranking
	String[][] leaderboardNames = {{"", "", ""}, {"", "", ""}, {"", "", ""}, {"", "", ""}};
	int[][] leaderboardScores = {{-1, -1, -1}, {-1, -1, -1}, {-1, -1, -1}, {-1, -1, -1}};
	int[] rankings = {1, 1, 1, 1};

	// more shapes + images
	Rectangle bunnyRect, cow1Rect, cow2Rect; // collision hitboxes
	BufferedImage waterBackground, sky; // moving backgrounds
	Image cursor1, homeScreen, enterNameScreen, grassBackground, gameScreen, flowers, carrot, ohno, yay, menu, offScreenImage, blackScreen, whiteBox, leaderboardText, optionsText, credits;
	Image[] cows, rulesScreens, playButtons, rulesButtons, exitButtons, levelsButtons, infoButtons, checkButtons, levelOneButtons, levelTwoButtons, levelThreeButtons, levelFourButtons, winningScreens, numbersImages, letters, homeButtons, pauseButtons, leaderboardButtons, xButtons, restartButtons, rightArrows, leftArrows;
	Image[][] bunnies, lPaths, straightPaths, evilBunnies;
	Graphics offScreenBuffer;
	Clip mainMenuMusic, levelOneMusic, levelTwoMusic, levelThreeMusic, levelFourMusic, clickSound;

	public BunnyHopFinal() {
		// Setting the defaults for the panel
		setPreferredSize (new Dimension (840, 630));

		// SCREENS/BACKGROUNDS
		// main screens/wallpapers
		gameScreen = Toolkit.getDefaultToolkit ().getImage ("screens/gamescreen.png");
		homeScreen = Toolkit.getDefaultToolkit ().getImage ("screens/homepage.png");
		enterNameScreen = Toolkit.getDefaultToolkit().getImage("screens/entername.png");
		grassBackground = Toolkit.getDefaultToolkit().getImage("screens/grassBackground.png");

		// menu background/text
		menu = Toolkit.getDefaultToolkit ().getImage ("screens/menubackground.png");
		leaderboardText = Toolkit.getDefaultToolkit ().getImage ("screens/leaderboardText.png");
		optionsText = Toolkit.getDefaultToolkit ().getImage ("screens/optionsText.png");
		credits = Toolkit.getDefaultToolkit ().getImage ("screens/credits.png");

		// winning screens
		winningScreens = new Image[2];
		winningScreens[0] = Toolkit.getDefaultToolkit ().getImage ("screens/grassbackground.png");
		winningScreens[1] = Toolkit.getDefaultToolkit ().getImage ("screens/grassbackground.png");


		// rules screen
		rulesScreens = new Image[5];
		for (int i = 0; i < 5; i++) {
			rulesScreens[i] = Toolkit.getDefaultToolkit ().getImage ("rules/" + (i+1) + ".png");
		}

		// moving background (water and sky)
		try {
			waterBackground = ImageIO.read(new File("screens/waterBackground.png"));
			sky = ImageIO.read(new File("screens/sky.png"));

			width = waterBackground.getWidth();
		}
		catch (IOException e) {
		}

		// BUTTONS
		// home screen buttons
		playButtons = new Image[2];
		playButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/playbutton.png");
		playButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/playbuttonclicked.png");

		rulesButtons = new Image[2];
		rulesButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/rulesbutton.png");
		rulesButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/rulesbuttonclicked.png");

		exitButtons = new Image[2];
		exitButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/exitbutton.png");
		exitButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/exitbuttonclicked.png");

		infoButtons = new Image[2];
		infoButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/infobutton.png");
		infoButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/infobuttonclicked.png");

		levelsButtons = new Image[2];
		levelsButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/levelsbutton.png");
		levelsButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/levelsbuttonclicked.png");

		checkButtons = new Image[2];
		checkButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/check.png");
		checkButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/checkclicked.png");

		// levels menu buttons
		levelOneButtons = new Image[2];
		levelOneButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/level1button.png");
		levelOneButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/level1buttonclicked.png");

		levelTwoButtons = new Image[2];
		levelTwoButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/level2button.png");
		levelTwoButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/level2buttonclicked.png");

		levelThreeButtons = new Image[2];
		levelThreeButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/level3button.png");
		levelThreeButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/level3buttonclicked.png");

		levelFourButtons = new Image[2];
		levelFourButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/level4button.png");
		levelFourButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/level4buttonclicked.png");

		// pause/leaderboard/rules menu buttons
		homeButtons = new Image[2];
		homeButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/homebutton.png");
		homeButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/homebuttonclicked.png");

		pauseButtons = new Image[2];
		pauseButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/pause.png");
		pauseButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/pauseclicked.png");

		leaderboardButtons = new Image[2];
		leaderboardButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/leaderboardButton.png");
		leaderboardButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/leaderboardclicked.png");

		rightArrows = new Image[2];
		rightArrows[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/rightarrow.png");
		rightArrows[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/rightarrowclicked.png");

		leftArrows = new Image[2];
		leftArrows[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/leftarrow.png");
		leftArrows[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/leftarrowclicked.png");

		xButtons = new Image[2];
		xButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/xbutton.png");
		xButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/xbuttonclicked.png");

		restartButtons = new Image[2];
		restartButtons[0] = Toolkit.getDefaultToolkit ().getImage ("buttons/replaybutton.png");
		restartButtons[1] = Toolkit.getDefaultToolkit ().getImage ("buttons/replaybuttonclicked.png");

		// CHARACTERS
		// bunnies
		bunnies = new Image[4][4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				bunnies[i][j] = Toolkit.getDefaultToolkit ().getImage ("bunnies/" + (i+1) + "_" + (j+1) + ".png");
			}
		}

		// cows
		cows = new Image[5];
		for (int i = 0; i < 5; i++) {
			cows[i] = Toolkit.getDefaultToolkit ().getImage ("cows/" + (i+1) + ".png");
		}

		// PATHS
		// L-shaped paths
		lPaths = new Image[2][4]; // 0 is regular colour, 1 is hovered (dark)
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 4; j++) {
				lPaths[i][j] = Toolkit.getDefaultToolkit ().getImage ("paths/lshape/" + (i+1) + "_" + (j+1) + ".png");
			}
		}

		// straight paths
		straightPaths = new Image[2][4]; // 0 is regular colour, 1 is hovered (dark)
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				straightPaths[i][j] = Toolkit.getDefaultToolkit ().getImage ("paths/straight/" + (i+1) + "_" + (j+1) + ".png");
				straightPaths[i][j+2] = Toolkit.getDefaultToolkit ().getImage ("paths/straight/" + (i+1) + "_" + (j+1) + ".png");
			}
		}

		// TEXT BUBBLES + DECORATIONS
		ohno = Toolkit.getDefaultToolkit ().getImage ("decor/ohno.png");
		yay = Toolkit.getDefaultToolkit ().getImage ("decor/yay.png");
		carrot = Toolkit.getDefaultToolkit ().getImage ("decor/carrot.png");
		flowers = Toolkit.getDefaultToolkit ().getImage ("decor/flowers.png");
		blackScreen = Toolkit.getDefaultToolkit().getImage("screens/black.png");
		whiteBox = Toolkit.getDefaultToolkit().getImage("screens/whitebox.png");


		// NUMBERS + SYMBOLS + TEXT
		// numbers/symbols for timer
		numbersImages = new Image[11];
		for (int i = 0; i < 10; i++) {
			numbersImages[i] = Toolkit.getDefaultToolkit ().getImage ("times/" + i + ".png");
		}
		numbersImages[10] = Toolkit.getDefaultToolkit ().getImage ("times/colon.png");

		// letters/symbols for leaderboard names
		letters = new Image[26];
		int current = 97;
		for (int i = 0; i < 26; i++) {
			letters[i] = Toolkit.getDefaultToolkit ().getImage ("letters/" + Character.toString(current) + ".png");
			current++;
		}

		// CUSTOM CURSOR
		cursor1 = Toolkit.getDefaultToolkit().getImage("decor/mouse1.png");

		// Defining the hotspot to the centre of the object
		Point hotspot = new Point (0, 0);
		Toolkit toolkit = Toolkit.getDefaultToolkit ();
		Cursor cursor = toolkit.createCustomCursor (cursor1, hotspot, "decor/cursor1");
		frame.setCursor (cursor);


		// SOUNDS
		try {
			AudioInputStream sound;

			// background music
			sound = AudioSystem.getAudioInputStream(new File ("sounds/mainmenu.wav"));
			mainMenuMusic = AudioSystem.getClip();
			mainMenuMusic.open(sound);

			// level one music
			sound = AudioSystem.getAudioInputStream(new File ("sounds/levelone.wav"));
			levelOneMusic = AudioSystem.getClip();
			levelOneMusic.open(sound);

			// level two music
			sound = AudioSystem.getAudioInputStream(new File ("sounds/leveltwo.wav"));
			levelTwoMusic = AudioSystem.getClip();
			levelTwoMusic.open(sound);

			// level three music
			sound = AudioSystem.getAudioInputStream(new File ("sounds/levelthree.wav"));
			levelThreeMusic = AudioSystem.getClip();
			levelThreeMusic.open(sound);

			// level four music
			sound = AudioSystem.getAudioInputStream(new File ("sounds/levelfour.wav"));
			levelFourMusic = AudioSystem.getClip();
			levelFourMusic.open(sound);

			// clicking sound
			sound = AudioSystem.getAudioInputStream(new File ("sounds/clicksound.wav"));
			clickSound = AudioSystem.getClip();
			clickSound.open(sound);
		}
		catch (Exception e) {

		}

		// start main menu music + keep looping
		mainMenuMusic.setFramePosition (0);
		mainMenuMusic.start();
		mainMenuMusic.loop(Clip.LOOP_CONTINUOUSLY);

		// LISTENERS
		addMouseListener(this);
		addKeyListener(this);
		addMouseMotionListener(this);

		this.setFocusable(true);

		// THREAD
		Thread thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {

		while(true) {
			threadCount++;
			update();
			repaint();

			try {
				Thread.sleep(9); // refreshes every approx. 1/100 of a second
			} 
			catch(Exception e) {

			}
		}
	}

	// Description: updates animations, timer, and collisions (every time it is re-called in run method)
	// Parameters: none
	// Return: none (void)
	public void update() {

		// move sky/water background
		if (threadCount % 10 == 0) {
			backgroundX -= speed;
			if(backgroundX < -width)
				backgroundX = 0;
			else if(backgroundX > width)
				backgroundX = 0;
		}

		if (gameState == LEVEL_ONE || gameState == LEVEL_TWO || gameState == LEVEL_THREE || gameState == LEVEL_FOUR) {
			gameInProgress = true;
		}
		else {
			gameInProgress = false;
		}

		// move 35 frames to get to next square
		if (bunnyWalking && moveState < 35) {
			moveState++;
		}

		// if reached 35, stop moving and reset moveState to 0
		else {
			moveState = 0;
			left = false;
			right = false;
			up = false;
			down = false;
			bunnyWalking = false;
		}


		// move bunny
		if (!paused)
			bunnyMove();

		// game in progress and not paused
		if (gameInProgress && !paused) {
			// check if bunny just reached end
			if (!bunnyCompleted && checkBunnyComplete()) {
				bunnyCompleted = true;
				timeout = 0;
				
				// update the file with the current player's name/time
				try {
					updateFile();
				} catch (IOException e1) {
				}

				// update the leaderboard to include new player's stats
				try {
					updateLeaderboard();
				} catch (FileNotFoundException e1) {
				}
			}

			// bunny finished path - wait a second before showing winning screen
			if (bunnyCompleted) {
				timeout++; // keeps track of how long it has been waiting
				if (timeout > 100) {
					gameState = WINNING; // go to winning screen
					showLeaderboard = true;
					leaderboardLevel = currentLevel; // show current level's leaderboard
					timeout = 0; // resets timeout
				}

			}
			// bunny not finished path - add to the timer
			else {
				hundredthSeconds++;
			}

			// if bunny moving is in progress
			if (!bunnyCompleted && pathCompleted) {
				cowMove(); // move the cow

				// check collision between bunny and cow 1
				bunnyRect = new Rectangle(bunnyX+15, bunnyY+16, 25, 40);
				cow1Rect = new Rectangle(cow1x+5, cow1y+5, 70, 40);

				checkCollision(cow1Rect);

				// check collision between bunny and cow 2 in level 3 and 4 
				if (gameState == LEVEL_THREE || gameState == LEVEL_FOUR) {
					cow2Rect = new Rectangle(cow2x+5, cow2y+5, 70, 40);
					checkCollision(cow2Rect);
				}

			}

		}

	}

	// Description: draws all the graphics you see on the window
	// Parameters: Graphics g
	// Return: void
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Set up the offscreen buffer the first time paint() is called
		if (offScreenBuffer == null)
		{
			offScreenImage = createImage (this.getWidth (), this.getHeight ());
			offScreenBuffer = offScreenImage.getGraphics ();
		}

		// home screen - draw sky/grass background, walking bunny, and buttons
		if (gameState == HOME) {
			drawMovingBackground(sky);


			offScreenBuffer.drawImage(homeScreen, 0, 0, 840, 630, this);
			offScreenBuffer.drawImage(bunnies[bunnyDir][bunnyNo], bunnyX, 330, 84, 96, this);

			offScreenBuffer.drawImage(playButtons[playButtonOption], 75, 300, 270, 81, this);
			offScreenBuffer.drawImage(rulesButtons[rulesButtonOption], 75, 400, 270, 81, this);
			offScreenBuffer.drawImage(exitButtons[exitButtonOption], 75, 500, 270, 81, this);
			offScreenBuffer.drawImage(infoButtons[infoButtonOption], 780, 20, 40, 45, this);
			offScreenBuffer.drawImage(leaderboardButtons[leaderboardButtonOption], 620, 500, 78, 78, this);
			offScreenBuffer.drawImage(levelsButtons[levelsButtonOption], 720, 500, 78, 78, this);
		}

		// enter name screen - draw sky, enter name screen, letters as the user types their name
		if (gameState == ENTER_NAME) { 
			drawMovingBackground(sky);


			offScreenBuffer.drawImage(homeButtons[homeButtonOption], 10, 10, 44, 48, this);

			offScreenBuffer.drawImage(enterNameScreen, 0, 0, 840, 630, this);
			for (int i = 0; i < playerName.length(); i++) { // loop through each character in name
				if (playerName.charAt(i) != ' ') // if not a space
					offScreenBuffer.drawImage(letters[playerName.charAt(i)-65],  343 + 35*i, 308, 35, 35, this); // display the letter corresponding to that character in name
			}
		}

		// level selection screen - draw sky and background, buttons for each level
		if (gameState == LEVEL_SELECTION) {
			drawMovingBackground(sky);
			offScreenBuffer.drawImage(grassBackground, 0, 0, 840, 630, this);
			offScreenBuffer.drawImage(bunnies[bunnyDir][bunnyNo], bunnyX, 330, 84, 96, this);

			offScreenBuffer.drawImage(levelOneButtons[levelOneButtonOption], 100, 150, 100, 100, this);
			offScreenBuffer.drawImage(levelTwoButtons[levelTwoButtonOption], 280, 150, 100, 100, this);
			offScreenBuffer.drawImage(levelThreeButtons[levelThreeButtonOption], 460, 150, 100, 100, this);
			offScreenBuffer.drawImage(levelFourButtons[levelFourButtonOption], 640, 150, 100, 100, this);

			offScreenBuffer.drawImage(homeButtons[homeButtonOption], 10, 10, 44, 48, this);
		}
		
		// while playing game 
		if (gameInProgress) {
			// draw water background, game screen, timer
			drawMovingBackground(waterBackground);

			offScreenBuffer.drawImage(gameScreen, 0, 0, 840, 630, this);
			offScreenBuffer.drawImage(carrot, 0, 0, 840, 630, this);

			drawTimes(hundredthSeconds, 710, 10, 15);

			// draw paths according to the playerPath and playerOrientation array
			for (int row = 0 ; row < 8 ; row++)
				for (int column = 0 ; column < 8 ; column++)
				{
					// Find the x and y positions for each row and column
					int xPos = (int)(SIDE_OFFSET + (column) * 70);
					int yPos = (int)(TOP_OFFSET + row * 70);

					// Draw each piece, depending on the value in board					
					if (playerPath [row] [column] == 1) {
						offScreenBuffer.drawImage (straightPaths[pathColours[row][column]][playerOrientation[row][column]], xPos, yPos, SQUARE_SIZE, SQUARE_SIZE, this);
					}
					else if (playerPath[row] [column] == 2) {
						offScreenBuffer.drawImage (lPaths[pathColours[row][column]][playerOrientation[row][column]], xPos, yPos, SQUARE_SIZE, SQUARE_SIZE, this);
					}
				}

			// draw flowers
			offScreenBuffer.drawImage(flowers, 0, 0, 840, 630, this);

			// draw bunny (controlled by player)
			offScreenBuffer.drawImage(bunnies[bunnyDir][bunnyNo], bunnyX, bunnyY, 56, 64, this); 

			// text bubbles if collision or if bunny completes path
			if (collision)
				offScreenBuffer.drawImage(ohno, bunnyX+50, bunnyY-10, 128, 48, this); 
			if (bunnyCompleted) 
				offScreenBuffer.drawImage(yay, bunnyX+50, bunnyY-10, 128, 48, this); 

			// draw cows if path is complete
			if (pathCompleted) {
				offScreenBuffer.drawImage(cows[cowNo], cow1x, cow1y, 81, 54, this); // first cow in all levels

				// second cow in level 3 and 4
				if (gameState == LEVEL_THREE || gameState == LEVEL_FOUR) {
					offScreenBuffer.drawImage(cows[cowNo], cow2x, cow2y+5, 78, 45, this);
				}
			}

			// draw pause menu with text/buttons if paused
			if (paused) {
				offScreenBuffer.drawImage(blackScreen, 0, 0, 840, 630, this);
				offScreenBuffer.drawImage(menu, 180, 130, 480, 350, this);
				offScreenBuffer.drawImage(optionsText, 210, 130, 480, 350, this);				
				offScreenBuffer.drawImage(leaderboardButtons[leaderboardButtonOption], 230, 320, 100, 100, this);
				offScreenBuffer.drawImage(restartButtons[restartButtonOption], 510, 320, 100, 100, this);
				offScreenBuffer.drawImage(homeButtons[homeButtonOption], 370, 320, 100, 100, this);
				offScreenBuffer.drawImage(xButtons[xButtonOption], 610, 150, 30, 30, this);

			}

			// draw pause button when playing
			offScreenBuffer.drawImage(pauseButtons[pauseButtonOption], 10, 10, 44, 48, this);

		}

		// winning screen - draw sky/grass background, check button
		if (gameState == WINNING) {
			drawMovingBackground(sky);
			offScreenBuffer.drawImage(grassBackground, 0, 0, 840, 630, this);
			offScreenBuffer.drawImage(checkButtons[checkButtonOption], 680, 480, 120, 120, this);
		}

		// showing leaderboard (either by clicking on leaderboard button or shown automatically when player wins)
		if (showLeaderboard) {
			int textSize = 24;

			// SHOW LEADERBOARD FOR LEVEL
			offScreenBuffer.drawImage(menu, 180, 130, 480, 350, this);
			offScreenBuffer.drawImage(leaderboardText, 170, 110, 480, 350, this);

			// not winning screen - player clicked leaderboard
			if (gameState != WINNING) {
				offScreenBuffer.drawImage(xButtons[xButtonOption], 610, 150, 30, 30, this);
				offScreenBuffer.drawImage(rightArrows[rightArrowOption], 430, 430, 30, 30, this);
				offScreenBuffer.drawImage(numbersImages[leaderboardLevel], 408, 438, 15, 15, this);
				offScreenBuffer.drawImage(leftArrows[leftArrowOption], 370, 430, 30, 30, this);
			}

			// winning screen - player just completed a level
			else {
				offScreenBuffer.drawImage(whiteBox, 194, 354, 452, 350, this);
			}


			// display top 3 names + times on leaderboard
			for (int i = 0; i < 3; i++) {
				String playerName = leaderboardNames[leaderboardLevel-1][i];
				int playerTime = leaderboardScores[leaderboardLevel-1][i];

				// check if there is a valid player in this rank
				if (playerTime == -1) { 
					continue;
				}
				// display rank
				if (!playerName.equals("")) 
					offScreenBuffer.drawImage(numbersImages[i+1], 210, 250 + 50*i, textSize, textSize, this);

				// display name and time
				drawLeaderboardNames (playerName, 250, 250 + 50*i, 24);
				drawTimes(playerTime, 430, 250 + 50*i, 24);
			}


			// if winning screen, need to display player's time on that level
			if (gameState == WINNING) {
				// player rank
				int playerRank = rankings[leaderboardLevel-1];

				// if 2 digit 
				if (playerRank > 9 && playerRank < 100) {
					// display first digit
					offScreenBuffer.drawImage(numbersImages[playerRank/10], 198, 420, textSize, textSize, this);
					// display second digit
					offScreenBuffer.drawImage(numbersImages[playerRank%10], 198 + textSize, 420, textSize, textSize, this);
				}
				// if one digit
				else
					offScreenBuffer.drawImage(numbersImages[playerRank], 210, 420, textSize, textSize, this);

				// player name
				drawLeaderboardNames (playerName, 250, 420, 24);
				// player time
				drawTimes(hundredthSeconds, 430, 420, 24);

			}
		}

		// rules screen - show menu with arrows with the rules screen texts
		if (showRules) {
			offScreenBuffer.drawImage(menu, 180, 130, 480, 350, this);
			offScreenBuffer.drawImage(rulesScreens[rulesScreenOption], 200, 137, 432, 315, this);	

			if (gameState != WINNING) {
				offScreenBuffer.drawImage(xButtons[xButtonOption], 610, 150, 30, 30, this);
				offScreenBuffer.drawImage(rightArrows[rightArrowOption], 430, 430, 30, 30, this);
				offScreenBuffer.drawImage(leftArrows[leftArrowOption], 370, 430, 30, 30, this);
			}
		}

		// credits screen - show menu with credits text
		if (showCredits) {
			offScreenBuffer.drawImage(menu, 180, 130, 480, 350, this);
			offScreenBuffer.drawImage(credits, 200, 137, 432, 315, this);	

			offScreenBuffer.drawImage(xButtons[xButtonOption], 610, 150, 30, 30, this);
		}

		// put the buffered image onto the main screen
		g.drawImage (offScreenImage, 0, 0, this);//	g.drawImage(pic,  200, 150, this);

	}

	// Description: draws the background that moves slowly
	// Parameters: Image background is the background picture
	// Return: none (void)
	public void drawMovingBackground(Image background) {
		offScreenBuffer.drawImage(background, backgroundX, 0, null);
		offScreenBuffer.drawImage(background, backgroundX + width, 0, null);
		offScreenBuffer.drawImage(background, backgroundX - width, 0, null);
	}

	// Description: displays a name in the leaderboard
	// Parameters: String name is the name, int x is starting x coordinate (first letter), int y is y coordinate of name, int textSize is the font size
	// Return: none (void)
	public void drawLeaderboardNames (String name, int x, int y, int textSize) {
		for (int j = 0; j < name.length(); j++){
			if (name.charAt(j) != ' ') {
				offScreenBuffer.drawImage(letters[name.charAt(j)-65], x + j*textSize, y, textSize, textSize, this);
			}
		}
	}

	// Description: displays a time
	// Parameters: int time is the time (hundredth-seconds), int x is starting x coordinate (first digit), int y is y coordinate of time, int textSize is the font size
	// Return: none (void)
	public void drawTimes(int time, int x, int y, int textSize) {
		int[] timeDigits = convertTimeDigits(time);

		// need to display minutes
		if (timeDigits[1] > 0 || timeDigits[0] > 0) {
			if (timeDigits[0] > 0)
				offScreenBuffer.drawImage(numbersImages[timeDigits[0]], x, y, textSize, textSize, this);
			offScreenBuffer.drawImage(numbersImages[timeDigits[1]], x + textSize, y, textSize, textSize, this);
			offScreenBuffer.drawImage(numbersImages[10], x + 2*textSize, y, textSize, textSize, this);
		}

		// display seconds/hundredth-seconds
		offScreenBuffer.drawImage(numbersImages[timeDigits[2]], x + 3*textSize, y, textSize, textSize, this);
		offScreenBuffer.drawImage(numbersImages[timeDigits[3]], x + 4*textSize, y, textSize, textSize, this);
		offScreenBuffer.drawImage(numbersImages[10], x + 5*textSize, y, 20, 20, this);
		offScreenBuffer.drawImage(numbersImages[timeDigits[4]], x + 6*textSize, y, textSize, textSize, this);
		offScreenBuffer.drawImage(numbersImages[timeDigits[5]], x + 7*textSize, y, textSize, textSize, this);
	}

	// Description: resets entire level back to how it was at the start
	// Parameters: none
	// Return: none (void)
	public void resetLevel() {
		resetBunny();

		cowNo = 3;
		cow1x = 120;
		cow1y = 180;
		bunnyWalking = false;
		bunnyCompleted = false;
		pathCompleted = false;
		hundredthSeconds = 0;
		paused = false;

	}

	// Description: puts bunny back to original position (start of board facing left)
	// Parameters: none
	// Return: none (void)
	public void resetBunny() {
		bunnyNo = 0;
		bunnyDir = 0;
		bunnyCol = 8;
		bunnyRow = 0;
		bunnyX = 705;
		bunnyY = 20;
		moveState = 0;
		collision = false;
	}

	// Description: stops all music and resets frame position to 0 (beginning)
	// Parameters: none
	// Return: none (void)
	public void stopMusic() {
		mainMenuMusic.stop();
		levelOneMusic.stop();
		levelTwoMusic.stop();
		levelThreeMusic.stop();
		levelFourMusic.stop();
		mainMenuMusic.setFramePosition (0);
		levelOneMusic.setFramePosition (0);
		levelTwoMusic.setFramePosition (0);
		levelThreeMusic.setFramePosition (0);
		levelFourMusic.setFramePosition (0);
	}

	// Description: resets all paths and orientations
	// Parameters: none
	// Return: none (void)
	public void newLevel() {
		// make new paths/orientations arrays
		correctPath = new int[8][8];
		correctOrientation = new int[8][8];
		playerPath = new int[8][8];
		playerOrientation = new int[8][8];
		givenOrientation = new int[8][8];

		// fill array with all -1's (EMPTY) at start
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				playerPath[i][j] = EMPTY;
				playerOrientation[i][j] = EMPTY;
				correctPath[i][j] = EMPTY;
				correctOrientation[i][j] = EMPTY;
			}
		}
		resetLevel();
	}

	// Description: gets everything ready for level one (correct + player paths, music, etc.)
	// Parameters: none
	// Return: none (void)
	public void levelOne() {
		newLevel();
		levelOneMusic.start();
		levelOneMusic.loop(Clip.LOOP_CONTINUOUSLY);	

		// locations of each square in the path
		int[][] pathIndex = {{0, 7}, {0, 6}, {0, 5}, {1, 5}, {2, 5}, {3, 5}, {3, 4}, {3, 3}, {3, 2}, {4, 2}, {5, 2}, {5, 1}, {6, 1}, {7, 1}, {7, 0}};

		setCorrectPaths(pathIndex);
		setRandomPaths(14, 16);
	}

	// Description: gets everything ready for level two (correct + player paths, music, etc.)
	// Parameters: none
	// Return: none (void)
	public void levelTwo() {
		newLevel();
		levelTwoMusic.start();
		levelTwoMusic.loop(Clip.LOOP_CONTINUOUSLY);
		
		// locations of each square in the path
		int[][] pathIndex = {{0, 7}, {0, 6}, {1, 6}, {2, 6}, {2, 5}, {2, 4}, {1, 4}, {1, 3}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, {4, 3}, {4, 4}, {5, 4},
				{6, 4}, {6, 3}, {6, 2}, {6, 1}, {6, 0}, {7, 0}};

		setCorrectPaths(pathIndex);
		setRandomPaths(14, 16);

	}

	// Description: gets everything ready for level three (correct + player paths, music, etc.)
	// Parameters: none
	// Return: none (void)
	public void levelThree() {
		newLevel();
		cow2y = 450;

		levelThreeMusic.start();
		levelThreeMusic.loop(Clip.LOOP_CONTINUOUSLY);

		// locations of each square in the path
		int[][] pathIndex = {{0, 7}, {0, 6}, {1, 6}, {2, 6}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {5, 6}, {5, 5}, {6, 5}, 
				{6, 4}, {6, 3}, {5, 3}, {4, 3}, {3, 3}, {3, 4}, {2, 4}, {1, 4}, {1, 3}, {1, 2}, {2, 2}, {2, 1},
				{3, 1}, {4, 1}, {5, 1}, {6, 1}, {6, 0}, {7, 0}};


		setCorrectPaths(pathIndex);
		setRandomPaths(17, 19);
	}

	// Description: gets everything ready for level four (correct + player paths, music, etc.)
	// Parameters: none
	// Return: none (void)
	public void levelFour() {
		newLevel();
		cow2y = 380;
		levelFourMusic.start();
		levelFourMusic.loop(Clip.LOOP_CONTINUOUSLY);
		
		// locations of each square in the path
		int[][] pathIndex = {{0, 7}, {0, 6}, {1, 6}, {1, 7}, {2, 7}, {2, 6}, {2, 5}, {1, 5}, {1, 4}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, {4, 4}, {4, 5},
				{4, 6}, {5, 6}, {5, 5}, {6, 5}, {6, 4}, {6, 3}, {6, 2}, {5, 2}, {4, 2}, {3, 2}, {2, 2}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {6, 1}, {7, 1}, {7, 0}};


		setCorrectPaths(pathIndex);
		setRandomPaths(14, 16);
	}

	// Description: sets correct paths and orientations based on where the tiles are
	// Parameters: int[][] pathIndexes is the array of coordinates for each square where there is a path
	// Return: none (void)
	public void setCorrectPaths(int[][] pathIndexes) {

		// special case for first and last squares

		// horizontal for first tile
		if (pathIndexes[1][0] == 0) { 
			correctPath[0][7] = STRAIGHT;
			correctOrientation[0][7] = ROTATE_90;
		}

		// L shape going down/left for first tile
		else { 
			correctPath[0][7] = L_SHAPE;
			correctOrientation[0][7] = ROTATE_90;
		}

		// horizontal for last tile
		if (pathIndexes[pathIndexes.length-2][0] == 7) {
			correctPath[7][0] = STRAIGHT;
			correctOrientation[7][0] = ROTATE_90;
		}

		// L shape going left/down for last tile
		else {
			correctPath[7][0] = L_SHAPE;
			correctOrientation[7][0] = ROTATE_270;
		}

		// for remaining tiles in between
		for (int i = 1; i < pathIndexes.length-1; i++) {
			// vertical straight: column (x coordinate) of tile above and below must be equal
			if (pathIndexes[i-1][0] == pathIndexes[i][0] && pathIndexes[i+1][0] == pathIndexes[i][0]) {
				correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = STRAIGHT;
				correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_90;
			}
			// horizontal straight: row (y coordinate) of tile left and right must be equal
			else if (pathIndexes[i-1][1] == pathIndexes[i][1] && pathIndexes[i+1][1] == pathIndexes[i][1]) {
				correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = STRAIGHT;
				correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_0;
			}


			// horizontal then vertical
			else if (pathIndexes[i-1][0] == pathIndexes[i][0] && pathIndexes[i+1][1] == pathIndexes[i][1]) {
				// left then down
				if (pathIndexes[i-1][1] > pathIndexes[i][1] && pathIndexes[i+1][0] > pathIndexes[i][0]) {
					correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = L_SHAPE;
					correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_90;
				}

				// left then up
				else if (pathIndexes[i-1][1] > pathIndexes[i][1] && pathIndexes[i+1][0] < pathIndexes[i][0]) {
					correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = L_SHAPE;
					correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_0;
				}

				// right then up
				else if (pathIndexes[i-1][1] < pathIndexes[i][1] && pathIndexes[i+1][0] < pathIndexes[i][0]) {
					correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = L_SHAPE;
					correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_270;
				}

				// right then down
				else if (pathIndexes[i-1][1] < pathIndexes[i][1] && pathIndexes[i+1][0] > pathIndexes[i][0]) {
					correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = L_SHAPE;
					correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_180;
				}
			}

			// vertical then horizontal
			else if (pathIndexes[i-1][1] == pathIndexes[i][1] && pathIndexes[i+1][0] == pathIndexes[i][0]) {

				// down then right
				if (pathIndexes[i+1][1] > pathIndexes[i][1] && pathIndexes[i-1][0] < pathIndexes[i][0]) {
					correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = L_SHAPE;
					correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_0;
				}

				// up then right
				else if (pathIndexes[i+1][1] > pathIndexes[i][1] && pathIndexes[i-1][0] > pathIndexes[i][0]) {
					correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = L_SHAPE;
					correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_90;
				}

				// down then left
				else if (pathIndexes[i+1][1] < pathIndexes[i][1] && pathIndexes[i-1][0] < pathIndexes[i][0]) {
					correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = L_SHAPE;
					correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_270;
				}

				// up then left
				else if (pathIndexes[i+1][1] < pathIndexes[i][1] && pathIndexes[i-1][0] > pathIndexes[i][0]) {
					correctPath[pathIndexes[i][0]][pathIndexes[i][1]] = L_SHAPE;
					correctOrientation[pathIndexes[i][0]][pathIndexes[i][1]] = ROTATE_180;
				}
			}
		}
	}

	// Description: gives additional random paths to player on top of the correct paths and scrambles orientation
	// Parameters: int chance1 is the probability (out of 20) of no tile on a square, int chance2 is the probability of getting a straight path on a tile
	// Return: none (void)
	public void setRandomPaths(int chance1, int chance2) {
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {

				// if the tile is part of the actual path, put that path onto the given path
				if (correctPath[row][col] != -1) {
					playerPath[row][col] = correctPath[row][col];
				}

				// if the tile isn't part of actual path, generate a (fake) path randomly
				else {
					int random = (int)(Math.random() * 20) + 1;

					// no path
					if (random <= chance1) {
						playerPath[row][col] = EMPTY;
					}

					// straight path
					else if (random <= chance2) {
						playerPath[row][col] = STRAIGHT;
					}

					// L shaped path
					else {
						playerPath[row][col] = L_SHAPE;
					}
				}

				// generate random orientations
				int randomNum = (int)(Math.random() * 4);
				playerOrientation[row][col] = randomNum;
				givenOrientation[row][col] = randomNum;
			}
		}
	}

	// Description: rotates a piece 90 degrees inside the array
	// Parameters: int row, int col are the coordinates (row and column) of the rotated path
	// Return: none (void)
	public void pieceTurned(int row, int col) {
		playerOrientation[row][col]++; // rotate 90 degrees means increase orientation by 1
		playerOrientation[row][col] %= 4; // mod 4 since every four 90 degrees is equal to full circle
	}

	// Description: checks if path is completely connected
	// Parameters: int row, int col is the coordinates of the last piece that was rotated
	// Return: boolean (true if path complete, false if incomplete)
	boolean checkPath(int row, int col) {

		// if the piece that was just moved isn't correct, then the path is definitely not complete -> return false
		if (playerPath[row][col] == STRAIGHT) {
			if (playerOrientation[row][col]%2 != correctOrientation[row][col]%2) { // orientation is equal in mod 2 (straight paths are "equal" every 180 degrees)
				return false;
			}
		}
		else if (playerPath[row][col] == L_SHAPE) {
			if (playerOrientation[row][col] != correctOrientation[row][col]) { // orientation must be equal if L-shaped
				return false;
			}
		}

		// if the piece that was just moved is correct, check rest of board to see if everything is correct
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				// straight path - orientation must be equal in mod 2 (since there's only 2 directions)
				if (correctPath[i][j] == STRAIGHT) {
					if (playerOrientation[i][j]%2 != correctOrientation[i][j]%2) {
						return false;
					}
				}

				// L shaped path - orientation must be equal
				else if (correctPath[i][j] == L_SHAPE) {
					if (playerOrientation[i][j] != correctOrientation[i][j]) {
						return false;
					}
				}
			}
		}
		return true;
	}

	// Description: checks if bunny has reached end of path
	// Parameters: none
	// Return: boolean (true if bunny has reached end, false if not)
	boolean checkBunnyComplete() {
		if (bunnyRow == 7 && bunnyCol == -1) {
			return true;
		}
		return false;
	}

	// Description: checks if bunny is able to move left
	// Parameters: none
	// Return: boolean (true if bunny is able to move left, false if not)
	boolean goLeft() {
		// in leftmost column
		if (bunnyCol == 0) {
			// in last row - moving left will go to last lilypad which is correct
			if (bunnyRow == 7) 
				return true;

			// not in last row but in first column - cannot go left
			else
				return false;
		}

		// not in leftmost column
		else {
			// square to the left is straight horizontal
			if (correctPath[bunnyRow][bunnyCol-1] == STRAIGHT && correctOrientation[bunnyRow][bunnyCol-1] == ROTATE_90) 
				return true;

			// square to the left is L shape with opening on right side
			else if (correctPath[bunnyRow][bunnyCol-1] == L_SHAPE && (correctOrientation[bunnyRow][bunnyCol-1] == ROTATE_0 || correctOrientation[bunnyRow][bunnyCol-1] == ROTATE_90)) 
				return true;

			else 
				return false;

		}
	}

	// Description: checks if bunny is able to move up
	// Parameters: none
	// Return: boolean (true if bunny is able to move up, false if not)
	boolean goUp() {
		// in first row - cannot go up
		if (bunnyRow == 0) 
			return false;

		// square above is straight vertical
		else if (correctPath[bunnyRow-1][bunnyCol] == STRAIGHT && correctOrientation[bunnyRow-1][bunnyCol] == ROTATE_0) 
			return true;

		// square above is L shape with opening at bottom
		else if (correctPath[bunnyRow-1][bunnyCol] == L_SHAPE && (correctOrientation[bunnyRow-1][bunnyCol] == ROTATE_90 || correctOrientation[bunnyRow-1][bunnyCol] == ROTATE_180)) 
			return true;

		else 
			return false;

	}

	// Description: checks if bunny is able to move right
	// Parameters: none
	// Return: boolean (true if bunny is able to move right, false if not)
	boolean goRight() {
		// in starting position or in last column - cannot go right
		if (bunnyCol == 8 || bunnyCol >= 7)
			return false;

		// square to the right is straight horizontal
		else if (correctPath[bunnyRow][bunnyCol+1] == STRAIGHT && correctOrientation[bunnyRow][bunnyCol+1] == ROTATE_90)
			return true;

		// square to the right is L shape with opening on left side
		else if (correctPath[bunnyRow][bunnyCol+1] == L_SHAPE && (correctOrientation[bunnyRow][bunnyCol+1] == ROTATE_180 || correctOrientation[bunnyRow][bunnyCol+1] == ROTATE_270))
			return true;

		else 
			return false;

	}

	// Description: checks if bunny is able to move down
	// Parameters: none
	// Return: boolean (true if bunny is able to move down, false if not)
	boolean goDown() {
		// at starting position or in last row - cannot go down
		if (bunnyCol == 8 || bunnyRow >= 7) 
			return false;

		// square below is straight vertical
		else if (correctPath[bunnyRow+1][bunnyCol] == STRAIGHT && correctOrientation[bunnyRow+1][bunnyCol] == ROTATE_0) 
			return true;

		// square below is L shape with opening on top
		else if (correctPath[bunnyRow+1][bunnyCol] == L_SHAPE && (correctOrientation[bunnyRow+1][bunnyCol] == ROTATE_0 || correctOrientation[bunnyRow+1][bunnyCol] == ROTATE_270))
			return true;

		else 
			return false;

	}

	// Description: checks if bunny has bumped into cow and stops bunny from moving if it bumps
	// Parameters: rectangle of hitbox of obstacle
	// Return: none (void)
	void checkCollision(Rectangle obstacle) {
		//check if rect touches wall
		if(bunnyRect.intersects(obstacle) || collision) {
			collision = true;
			timeout++; // time out for bunny for a bit before restarting
			bunnyWalking = false;
			cowWalking = false;

			if (timeout > 80) { // resume after timeout
				resetBunny(); // bunny back to start
				timeout = 0;
				cowWalking = true;
			}

		}
	}

	// Description: controls bunny sprite animations
	// Parameters: none
	// Return: none (void)
	void bunnyMove() {
		if (gameState == HOME || gameState == LEVEL_SELECTION) {
			// move right
			if(bunnyDir == 1) {
				bunnyX += 1;
			}
			// move left
			else {
				bunnyX -= 1;
			}

			// reached right border -> start going left
			if (bunnyX > 770) {
				bunnyDir = 0;
			}
			// reached left border -> start going right
			if (bunnyX < 360) {
				bunnyDir = 1;
			}
			
			// change bunny sprite
			if(threadCount % 20 == 0) {
				bunnyNo++;
			}

			// loop through 3 bunny sprites
			if (bunnyNo >= 4) {
				bunnyNo = 1;
			}
		}

		if (gameInProgress) {			
			if (bunnyWalking) {
				// when bunny is moving (walking through path), loop last two bunny sprites

				// change bunny sprite every 16 frame refreshes
				if (threadCount % 16 == 0) {
					bunnyNo++;
				}
				
				// loop through 2 bunny sprites
				if (bunnyNo >= 4) 
					bunnyNo = 2;

				// directions bunny is moving in
				if (right) 
					bunnyX += 2;

				else if (left) 
					bunnyX -= 2;

				else if (up) 
					bunnyY -= 2;

				else if (down) 
					bunnyY += 2;
			}


			else if (!bunnyWalking) {
				// when bunny is not moving (ie. building path), loop first two bunny sprites
				if (bunnyNo >= 2)
					bunnyNo = 0;
				if (threadCount % 60 == 0) // change bunny sprite every 60 frame refreshes
					bunnyNo++;				
			}
		}
	}

	// Description: controls cow sprite animations
	// Parameters: none
	// Return: none (void)
	void cowMove() {
		if (cowWalking) {
			cow1x += 2;
			cow2x += 2;
		}
		
		// change cow sprite every 16 frame refreshes
		if(threadCount % 16 == 0) {
			cowNo++;

			// loop through 2 cow sprites
			if (cowNo == 5) {
				cowNo = 3;
			}
			
			// reach right border -> go back to left side
			if (cow1x > 650) {
				cow1x = 120;
			}
			
			if (cow2x > 650) {
				cow2x = 120;
			}
		}
	}

	@Override

	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();

		if (gameState == HOME) {
			// escape key pressed when showing leaderboard, credits, or rules -> stop displying this menu
			if (showLeaderboard) {
				if (key == KeyEvent.VK_ESCAPE) {
					showLeaderboard = false;
				}
			}
			if (showCredits) {
				if (key == KeyEvent.VK_ESCAPE) {
					showCredits = false;
				}
			}
			if (showRules) {
				if (key == KeyEvent.VK_ESCAPE) {
					showRules = false;
				}
			}
		}

		else if (gameState == LEVEL_SELECTION) {
			// escape key pressed - go back to homepage
			if (key == KeyEvent.VK_ESCAPE) {
				gameState = HOME;
				selectOwnLevel = false;
			}
		}

		else if (gameState == ENTER_NAME) {
			// escape key pressed - go back to homepage
			if (key == KeyEvent.VK_ESCAPE) {
				gameState = HOME;
				selectOwnLevel = false;
			}
			// letter pressed - add to name
			else if (Character.isLetter(key) && playerName.length() < 10) {
				playerName += Character.toString(key).toUpperCase();
			}
			// space pressed - add to name
			else if (key == ' ') {
				playerName += " ";
			}
			// backspace pressed - remove last letter from name
			else if (key == 8) { // backspace
				if(playerName.length() > 0)
					playerName = playerName.substring(0, playerName.length()-1);
			}
			// enter pressed - go to selected level
			else if (key == 10 && playerName.length() != 0) { // enter pressed
				gameState = currentLevel;
				stopMusic();
				resetLevel();
				if (currentLevel == 1)
					levelOne();
				if (currentLevel == 2)
					levelTwo();
				if (currentLevel == 3)
					levelThree();
				if (currentLevel == 4)
					levelFour();
			}

		}

		else if (allowedToMove && gameInProgress && !collision) {
			// escape key pressed - toggle on/off pause menu
			if (key == KeyEvent.VK_ESCAPE) {
				if (paused) {
					paused = false;
					showLeaderboard = false;
					pauseButtonOption = 0;
				}
				else {
					paused = true;
					pauseButtonOption = 1;
				}
			}
			if (!paused && pathCompleted && !bunnyCompleted) {
				// left
				if((key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) && !bunnyWalking) {
					if (goLeft()) {
						up = false;
						left = true;
						right = false;
						down = false;
						bunnyDir = 0; // face left
						bunnyCol--;
						bunnyWalking = true;
						allowedToMove = false;
					}
				}

				// right
				else if((key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) && !bunnyWalking) {
					if (goRight()) {
						up = false;
						right = true;
						left = false;
						down = false;
						bunnyDir = 1; // face right
						bunnyCol++;
						bunnyWalking = true;
						allowedToMove = false;
					}
				}

				// down 
				else if((key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) && !bunnyWalking) {
					if (goDown()) {
						up = false;
						down = true;
						right = false;
						left = false;
						bunnyDir = 3; // face down
						bunnyRow++;
						bunnyWalking = true;
						allowedToMove = false;
					}
				}

				// up
				else if((key == KeyEvent.VK_W || key == KeyEvent.VK_UP) && !bunnyWalking) {
					if (goUp()) {
						up = true;
						down = false;
						right = false;
						left = false;
						bunnyDir = 2; // face up
						bunnyRow--;
						bunnyWalking = true;
						allowedToMove = false;
					}
				}
			}

		}

	}

	@Override
	public void keyReleased(KeyEvent e) {
		// allowed to move if current key is released (makes sure player cannot hold key and bunny still moves)
		if (gameInProgress){
			allowedToMove = true;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {

		if(gameState == HOME){
			// play button
			if(e.getX() >= 75 && e.getY() >= 300 && e.getX() <= 345 && e.getY() <= 381) {
				gameState = ENTER_NAME;
				currentLevel = 1;
				clickSound.setFramePosition(0);
				clickSound.start();
			}
			// rules button
			else if(e.getX() >= 75 && e.getY() >= 400 && e.getX() <= 345 && e.getY() <= 481) {
				showRules = true;
				rulesScreenOption = 0;
				clickSound.setFramePosition(0);
				clickSound.start();
			}
			// exit button
			else if(e.getX() >= 75 && e.getY() >= 500 && e.getX() <= 345 && e.getY() <= 582) {
				clickSound.setFramePosition(0);
				clickSound.start();
				System.exit(0);
			}
			// levels button
			else if(e.getX() >= 720 && e.getY() >= 500 && e.getX() <= 798 && e.getY() <= 578) {
				gameState = LEVEL_SELECTION;
				selectOwnLevel = true;
				clickSound.setFramePosition(0);
				clickSound.start();
			}
			// info button
			else if (e.getX() >= 780 && e.getY() >= 20 && e.getX() <= 820 && e.getY() <= 65) {
				showCredits = true;
				clickSound.setFramePosition(0);
				clickSound.start();
			}
			// leaderboard button
			else if(e.getX() >= 620 && e.getY() >= 500 && e.getX() <= 698 && e.getY() <= 578) {
				showLeaderboard = true;
				leaderboardLevel = 1;
				clickSound.setFramePosition(0);
				clickSound.start();

				try {
					updateLeaderboard();
				} catch (FileNotFoundException e1) {
				}
			}

		}

		// if level is selected during level selection, go to enter name screen
		if (gameState == LEVEL_SELECTION) {
			// home button
			if (e.getX() >= 10 && e.getX() <= 54 && e.getY() >= 10 && e.getY() <= 58) {
				gameState = HOME;
				playerName = "";
				selectOwnLevel = false;
				clickSound.setFramePosition(0);
				clickSound.start();
			}

			// level one clicked
			if (e.getX() >= 100 && e.getX() <= 200 && e.getY() >= 150 && e.getY() <= 250) {
				currentLevel = 1;
				gameState = ENTER_NAME;
				clickSound.setFramePosition(0);
				clickSound.start();
			}

			// level two clicked
			else if (e.getX() >= 280 && e.getX() <= 380 && e.getY() >= 150 && e.getY() <= 250) {
				currentLevel = 2;
				gameState = ENTER_NAME;
				clickSound.setFramePosition(0);
				clickSound.start();
			}

			// level three clicked
			else if (e.getX() >= 460 && e.getX() <= 560 && e.getY() >= 150 && e.getY() <= 250) {
				currentLevel = 3;
				gameState = ENTER_NAME;
				clickSound.setFramePosition(0);
				clickSound.start();
			}

			// level four clicked
			else if (e.getX() >= 640 && e.getX() <= 740 && e.getY() >= 150 && e.getY() <= 250) {
				currentLevel = 4;
				gameState = ENTER_NAME;
				clickSound.setFramePosition(0);
				clickSound.start();
			}
		}


		else if (gameState == ENTER_NAME) {
			// home button
			if (e.getX() >= 10 && e.getX() <= 54 && e.getY() >= 10 && e.getY() <= 58) {
				gameState = HOME;
				playerName = "";
				selectOwnLevel = false;
				clickSound.setFramePosition(0);
				clickSound.start();
			}
		}

		// game in progress and not paused
		else if (gameInProgress && !paused) {

			// pause button clicked
			if (e.getX() >= 10 && e.getX() <= 54 && e.getY() >= 10 && e.getY() <= 58) {
				paused = true;
				clickSound.setFramePosition(0);
				clickSound.start();
			}

			// turn the pieces if it is clicked
			if (!pathCompleted) {
				int col = (e.getX()- SIDE_OFFSET)/70 ;
				int row = (e.getY()- TOP_OFFSET)/70;

				if (row < 8 && row >= 0 && col < 8 && col >= 0) {
					clickSound.setFramePosition(0);
					clickSound.start();
					pieceTurned(row, col);
					
					if (correctPath[row][col] != 0) {
						if(checkPath(row, col)) {
							pathCompleted = true;
						}
					}
				}
			}
		}

		else if (gameState == WINNING) {
			// check button clicked
			if (e.getX() >= 680 && e.getX() <= 800 && e.getY() >= 480 && e.getY() <= 600) {
				stopMusic();
				clickSound.setFramePosition(0);
				clickSound.start();
				showLeaderboard = false;
				// if you selected your own level or you are already level 4, go to homepage
				if (selectOwnLevel || currentLevel == 4) {
					gameState = HOME;
					mainMenuMusic.setFramePosition (0);
					mainMenuMusic.start();
					mainMenuMusic.loop(Clip.LOOP_CONTINUOUSLY);
					selectOwnLevel = false;
					playerName = "";
				}

				// if you didn't select your own level and aren't level 4, go to next level
				else {
					currentLevel++;
					gameState = currentLevel;
					if (currentLevel == 2)
						levelTwo();
					else if (currentLevel == 3)
						levelThree();
					else if (currentLevel == 4)
						levelFour();
				}
			}
		}
		else if (paused) {
			// leaderboard button clicked
			if (e.getX() >= 230 && e.getX() <= 330 && e.getY() >= 320 && e.getY() <= 420) {
				showLeaderboard = true;
				leaderboardLevel = currentLevel;
				clickSound.setFramePosition(0);
				clickSound.start();

				try {
					updateLeaderboard();
				} catch (FileNotFoundException e1) {
				}
			}
			// home button clicked
			else if (e.getX() >= 370 && e.getX() <= 470 && e.getY() >= 320 && e.getY() <= 420) {
				gameState = HOME;
				playerName = "";
				resetLevel();
				stopMusic();
				clickSound.setFramePosition(0);
				clickSound.start();
				mainMenuMusic.start();
				mainMenuMusic.loop(Clip.LOOP_CONTINUOUSLY);
			}
			// x button clicked
			else if (!showLeaderboard &&  e.getX() >= 580 && e.getX() <= 630 && e.getY() >= 150 && e.getY() <= 200) {
				paused = false;
			}
			// restart button clicked
			else if (e.getX() >= 510 && e.getX() <= 610 && e.getY() >= 320 && e.getY() <= 420) {
				clickSound.setFramePosition(0);
				clickSound.start();
				for (int i = 0; i < 8; i++) {
					for (int j = 0; j < 8; j++) {
						playerOrientation[i][j] = givenOrientation[i][j];
					}
				}
				resetLevel();
				paused = false;
			}

		}

		// leaderboard shown in home screen or pause menu
		if (gameState != WINNING && showLeaderboard) {
			// x button clicked -> close leaderboard
			if (e.getX() >= 610 && e.getX() <= 640 && e.getY() >= 150 && e.getY() <= 180) {
				showLeaderboard = false;
			}
			// right arrow clicked
			if (e.getX() >= 430 && e.getX() <= 460 && e.getY() >= 430 && e.getY() <= 460) {
				clickSound.setFramePosition(0);
				clickSound.start();
				if (leaderboardLevel != 4) { // level 4 is last page
					leaderboardLevel++;
				}
			}
			// left arrow clicked
			else if (e.getX() >= 370 && e.getX() <= 400 && e.getY() >= 430 && e.getY() <= 460) {
				clickSound.setFramePosition(0);
				clickSound.start();
				if (leaderboardLevel != 1) { // level 1 is first page
					leaderboardLevel--;
				}
			}
		}

		if (showRules) {
			// x button -> close rules
			if (e.getX() >= 610 && e.getX() <= 640 && e.getY() >= 150 && e.getY() <= 180) {
				showRules = false;
			}
			// right arrow
			if (e.getX() >= 430 && e.getX() <= 460 && e.getY() >= 430 && e.getY() <= 460) {
				clickSound.setFramePosition(0);
				clickSound.start();
				if (rulesScreenOption != 4) { // last page
					rulesScreenOption++;
				}
				else { // close rules
					showRules = false;
				}
			}
			// left arrow
			else if (e.getX() >= 370 && e.getX() <= 400 && e.getY() >= 430 && e.getY() <= 460) {
				clickSound.setFramePosition(0);
				clickSound.start();
				if (rulesScreenOption != 0) { // first page
					rulesScreenOption--;
				}
			}
		}

		if (showCredits) {
			// x button -> close credits
			if (e.getX() >= 610 && e.getX() <= 640 && e.getY() >= 150 && e.getY() <= 180) {
				showCredits = false;
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMoved(MouseEvent e) {

		// hover effects (when hovered, turn dark)
		if(gameState == HOME){

			// play button hovered
			if(e.getX() >= 75 && e.getY() >= 300 && e.getX() <= 345 && e.getY() <= 381) {
				playButtonOption = 1;
			}
			else {
				playButtonOption = 0;
			}

			// rules button hovered
			if(e.getX() >= 75 && e.getY() >= 400 && e.getX() <= 345 && e.getY() <= 481) {
				rulesButtonOption = 1;
			}
			else {
				rulesButtonOption = 0;
			}

			// exit button hovered
			if(e.getX() >= 75 && e.getY() >= 500 && e.getX() <= 345 && e.getY() <= 582) {
				exitButtonOption = 1;
			}
			else {
				exitButtonOption = 0;
			}

			// levels button hovered
			if(e.getX() >= 720 && e.getY() >= 500 && e.getX() <= 798 && e.getY() <= 578) {
				levelsButtonOption = 1;
			}
			else {
				levelsButtonOption = 0;
			}

			// leaderboard button hovered
			if(e.getX() >= 620 && e.getY() >= 500 && e.getX() <= 698 && e.getY() <= 578) {
				leaderboardButtonOption = 1;
			}
			else {
				leaderboardButtonOption = 0;
			} 

			// info button hovered
			if (e.getX() >= 780 && e.getY() >= 20 && e.getX() <= 820 && e.getY() <= 65) {
				infoButtonOption = 1;
			}
			else {
				infoButtonOption = 0;
			}
		}

		else if (gameState == LEVEL_SELECTION) {

			// home button hovered
			if (e.getX() >= 10 && e.getX() <= 54 && e.getY() >= 10 && e.getY() <= 58) {
				homeButtonOption = 1;
			}
			else {
				homeButtonOption = 0;
			}

			// level one button hovered
			if (e.getX() >= 100 && e.getX() <= 200 && e.getY() >= 150 && e.getY() <= 250) {
				levelOneButtonOption = 1;
			}
			else {
				levelOneButtonOption = 0;
			}

			// level two button option
			if (e.getX() >= 280 && e.getX() <= 380 && e.getY() >= 150 && e.getY() <= 250) {
				levelTwoButtonOption = 1;
			}
			else {
				levelTwoButtonOption = 0;
			}

			// level three button option
			if (e.getX() >= 460 && e.getX() <= 560 && e.getY() >= 150 && e.getY() <= 250) {
				levelThreeButtonOption = 1;
			}
			else {
				levelThreeButtonOption = 0;
			}

			// level four button option
			if (e.getX() >= 640 && e.getX() <= 740 && e.getY() >= 150 && e.getY() <= 250) {
				levelFourButtonOption = 1;
			}
			else {
				levelFourButtonOption = 0;
			}
		}

		else if (gameState == ENTER_NAME) {
			// home button hovered
			if (e.getX() >= 10 && e.getX() <= 54 && e.getY() >= 10 && e.getY() <= 58) {
				homeButtonOption = 1;
			}
			else {
				homeButtonOption = 0;
			}
		}

		else if (gameInProgress && !paused) {
			// pause button hovered
			if (e.getX() >= 10 && e.getX() <= 54 && e.getY() >= 10 && e.getY() <= 58) {
				pauseButtonOption = 1;
			}
			else {
				pauseButtonOption = 0;
			}

			// path hovered
			int col = (e.getX()- SIDE_OFFSET)/70 ;
			int row = (e.getY()- TOP_OFFSET)/70;

			// set all path colours to 0 (default) except for the hovered one which is 1 (dark)
			if (row < 8 && row >= 0 && col < 8 && col >= 0) {
				for(int i = 0; i < 8; i++) {
					for (int j = 0; j < 8; j++) {
						pathColours[i][j] = 0;
					}
				}
				pathColours[row][col] = 1;
			}

		}

		else if (gameState == WINNING) {
			// check button hovered
			if (e.getX() >= 680 && e.getX() <= 800 && e.getY() >= 480 && e.getY() <= 600) {
				checkButtonOption = 1;
			}
			else {
				checkButtonOption = 0;
			}

		}
		if (paused) {
			// leaderboard button hovered
			if (e.getX() >= 230 && e.getX() <= 330 && e.getY() >= 320 && e.getY() <= 420) {
				leaderboardButtonOption = 1;
			}
			else {
				leaderboardButtonOption = 0;
			}

			// home button hovered
			if (e.getX() >= 370 && e.getX() <= 470 && e.getY() >= 320 && e.getY() <= 420) {
				homeButtonOption = 1;
			}
			else {
				homeButtonOption = 0;
			}

			// restart button hovered
			if (e.getX() >= 510 && e.getX() <= 610 && e.getY() >= 320 && e.getY() <= 420) {
				restartButtonOption = 1;
			}
			else {
				restartButtonOption = 0;
			}

			// x button hovered
			if (e.getX() >= 610 && e.getX() <= 640 && e.getY() >= 150 && e.getY() <= 180) {
				xButtonOption = 1;
			}
			else {
				xButtonOption = 0;
			}
		}

		// leaderboard/rules menu open
		if (showLeaderboard || showRules) {
			// right arrow hovered
			if (e.getX() >= 430 && e.getX() <= 460 && e.getY() >= 420 && e.getY() <= 450) {
				rightArrowOption = 1;
			}
			else {
				rightArrowOption = 0;
			}

			// left arrow hovered
			if (e.getX() >= 370 && e.getX() <= 400 && e.getY() >= 420 && e.getY() <= 450) {
				leftArrowOption = 1;
			}
			else {
				leftArrowOption = 0;
			}
		}
		
		// leaderboard/rules/credits menu open
		if (showLeaderboard || showRules || showCredits) {
			// x button hovered
			if (e.getX() >= 610 && e.getX() <= 640 && e.getY() >= 150 && e.getY() <= 180) {
				xButtonOption = 1;
			}
			else {
				xButtonOption = 0;
			}
		}
	}

	// Description: updates top 3 names/scores in leaderboard
	// Parameters: none
	// Return: none (void)
	public void updateLeaderboard() throws FileNotFoundException {
		for (int i = 0; i < 4; i++) {

			// get score and name files for that level
			Scanner scoresInFile = new Scanner(new File("scores/level" + (i+1) + "scores.txt"));
			Scanner namesInFile = new Scanner(new File("scores/level" + (i+1) + "names.txt"));

			// get top 3 names and scores (first 3 lines of the files since it is sorted already)
			for (int j = 0; j < 3; j++) {
				if (scoresInFile.hasNextLine()) {
					// save to leaderboard to update
					leaderboardScores[i][j] = Integer.parseInt(scoresInFile.nextLine());
					leaderboardNames[i][j] = namesInFile.nextLine();
				}
			}
			scoresInFile.close();
			namesInFile.close();

		}
	}

	// Description: converts number of hundredth-seconds to its digits in minutes:seconds:hundredth-seconds
	// Parameters: int hundredths is number of hundredth-seconds
	// Return: int[] of each digit
	public int[] convertTimeDigits(int hundredths) {
		int tenMinutes = hundredths/60000; // 60000 hundredths-seconds in 10 minutes
		hundredths %= 60000;
		int minutes = hundredths/6000; // 6000 hundredths-seconds in 1 minute
		hundredths %= 6000;
		int tenSeconds = hundredths/1000; // 1000 hundredths-seconds in 10 seconds
		hundredths %= 1000;
		int oneSeconds = hundredths/100; // 100 hundredths-seconds in 1 second
		hundredths %= 100;
		int tenHundredths = hundredths/10; // 10 hundredths-seconds in 0.1 seconds
		hundredths %= 10;

		int[] converted = {tenMinutes, minutes, tenSeconds, oneSeconds, tenHundredths, hundredths};
		return converted;
	}

	// Description: adds a new score (time) + name into the corresponding level's file in increasing order of time taken
	// Parameters: none
	// Return: none (void)
	public void updateFile() throws IOException {

		int numScores = 0;

		// scores
		Scanner inFile = new Scanner(new File("scores/level" + currentLevel + "scores.txt"));

		// get number of players
		while (inFile.hasNextLine()) {
			if (inFile.nextLine() != "") {
				numScores++;
			}
		}
		inFile.close();

		// scores and names are arrays (in increasing order) of the player's times and names, including the current (newest) player
		int[] scores = new int[numScores+1];
		String[] names = new String[numScores+1];

		// open the txt files that store scores and names
		Scanner scoresInFile = new Scanner(new File("scores/level" + currentLevel + "scores.txt"));
		Scanner namesInFile = new Scanner(new File("scores/level" + currentLevel + "names.txt"));

		// add already existing scores and names to the array
		for (int j = 0; j < numScores; j++) {
			scores[j] = Integer.parseInt(scoresInFile.nextLine());
			names[j] = namesInFile.nextLine();

		}
		scoresInFile.close();
		namesInFile.close();

		// find the first line where the current player's time is lower than it
		int position;
		for (position = 0; position < numScores; position++) {
			if (scores[position] > hundredthSeconds) {
				break;
			}
		}

		// shift remaining scores + names after this line one position later
		for(int k = numScores - 1; k >= position; k--){
			scores[k+1] = scores[k];
			names[k+1] = names[k];           
		}

		// save the current player's name and score in that found position
		scores[position] = hundredthSeconds;
		names[position] = playerName;
		rankings[currentLevel-1] = position+1; // ranking of current player

		// output the updated names/scores in txt file
		PrintWriter scoresOutFile = new PrintWriter(new FileWriter("scores/level" + currentLevel + "scores.txt"));
		PrintWriter namesOutFile = new PrintWriter(new FileWriter("scores/level" + currentLevel + "names.txt"));

		// print out scores + names line by line onto txt files
		for (int i = 0; i < numScores + 1; i++) {
			scoresOutFile.println(scores[i]);
			namesOutFile.println(names[i]);
		}
		scoresOutFile.close();
		namesOutFile.close();

	}

	public static void main(String[] args) {

		frame = new JFrame("bunnyhop!");
		BunnyHopFinal myPanel = new BunnyHopFinal();

		frame.add(myPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

	}
}


