import java.io.*;
import java.net.*;
import java.util.Observable;
import java.util.Random;
import java.util.Scanner;

public class Client {
	
	static ServerSocket myServer;
	static int myport;
	static Socket socket;
	static BufferedReader fromServer = null, fromNext = null, fromPrev = null;
	static PrintWriter toServer = null, toNext = null, toPrev = null;
	static String prevUsername, nextUsername;
	
	static int myDiceCount, numberOfBidDice, bidDieValue;
	static int[] myDice;
	static int numPlayers;
	
	static boolean startFirst;
	
	static DiceGui gui;
	
	//static boolean active = false;
	
	static String username;
	
	public static void initServerSocket(String host, int myport) throws IOException {
		myServer = new ServerSocket(myport);

		socket = new Socket(host, 10501);
		fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		toServer = new PrintWriter(socket.getOutputStream());
		
		toServer.println("" + myport);
		toServer.flush();
	}
	
	public static void initDice(int diceCount) throws IOException {
		Random rand = new Random();
		myDice = new int[diceCount];
		for (int i = 0; i < myDice.length; i++) {
			myDice[i] = rand.nextInt(6) + 1;
		}
		gui.updateDice(myDice);
		numberOfBidDice = 0;
		bidDieValue = 1;
	}

	public static void printDice() {
		System.out.print("Your dice are: ");
		for (int i=0; i<myDice.length; i++) {
			System.out.print(myDice[i] + " ");
		}
		System.out.println();
	}
	
	public static void main(String[] args) {
		gui = new DiceGui();
		
		String host, message;
		
		message = "";

		try {
			if (args.length < 3) {
				System.out.println("need at least 3 args");
				System.exit(1);
			}
			host = args[0];
			myport = Integer.parseInt(args[1]);
			username = args[2];
			
			initServerSocket(host, myport);
			
			numPlayers = Integer.parseInt(fromServer.readLine());
			System.out.println("Total players: " + numPlayers);
			
			myDiceCount = 5;
			initDice(myDiceCount);
			
			for (int i = 0; i < 2; i++) {
				System.out.println("Connecting to peer...");
				message = fromServer.readLine();
				if (message.equals("ACCEPT")) {
					socket = myServer.accept();
					fromPrev = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					toPrev = new PrintWriter(socket.getOutputStream());
					prevUsername = fromPrev.readLine();
					System.out.println("Connected to user " + prevUsername);
					toPrev.println(username);
					toPrev.flush();
				} else if (message.substring(0, 7).equals("CONNECT")) {
					String[] temp = message.split(" ");
					host = temp[1];
					int port = Integer.parseInt(temp[2]);
					socket = new Socket(host, port);
					fromNext = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					toNext = new PrintWriter(socket.getOutputStream());
					toNext.println(username);
					toNext.flush();
					nextUsername = fromNext.readLine();
					System.out.println("Connected to user " + nextUsername);
				}
			}
			
			System.out.println("Starting game:\n");
			message = fromServer.readLine();
			startFirst = message.equals("START_FIRST");
			printDice();
			mainLoop();
			
			// Inform server that this Client has been eliminated/has won the game. Once all Clients do so, Server will shut
			// everything down.
			send(toServer, "Done");
			
		} catch (Exception e) {
			System.err.println("Game over.");
		}
	}
	
	public static void mainLoop() throws IOException {
		String message;
		while (true) {
			
			if (startFirst) {
				message = "DO_TURN";
				startFirst = false;
			} else {
				message = fromPrev.readLine();
			}
			
			String[] parsedMessage = message.split(" ");
			if (stringContains(parsedMessage, "DO_TURN")) {
				//if (totalDice == myDiceCount) {
				//	System.out.println("You won");
				//	break;
				//}
				doTurn();
				continue;
			}
			else if (stringContains(parsedMessage, "LOSE_DIE") && stringContains(parsedMessage, username)) {
				myDiceCount--;
				nextRound();
				send(toNext, "NEXT_ROUND");
				fromPrev.readLine();
				if (myDiceCount == 0) {
					eliminated(false);
					break;
				} else {
					send(toNext, "DO_TURN");
				}
			}
			else if (stringContains(parsedMessage, "TALLY")) {
				for (int i = 0; i < myDice.length; i++) {
					message += "" + myDice[i] + " ";
				}
				send(toNext, message);
			}
			else if (stringContains(parsedMessage, "NEXT_ROUND")) {
				nextRound();
				printDice();
				send(toNext, "NEXT_ROUND");
			}
			else if (stringContains(parsedMessage, "BID")) {
				System.out.println(parsedMessage[0] + " bid: " + parsedMessage[2] + " " + parsedMessage[3]);
				numberOfBidDice = Integer.parseInt(parsedMessage[2]);
				bidDieValue = Integer.parseInt(parsedMessage[3]);
				send(toNext, message);
			}
			else if (stringContains(parsedMessage, "ACCEPT_NEW")) {
				acceptNew();
			}
			else if (stringContains(parsedMessage, "CONNECT_NEW")) {
				if (stringContains(parsedMessage, username)) {
					connectNew();
				} else {
					send(toNext, message);
				}
			}
			else if (stringContains(parsedMessage, "PLAYER_ELIMINATED")) {
				System.out.println(parsedMessage[1] + " was eliminated!");
				numPlayers--;
				send(toNext, message);
				if (numPlayers == 1) {
					System.out.println("YOU WON");
					gui.win();
					break;
				}
			}
			else {
				//System.out.println(message);
				send(toNext, message);
				continue;
			}
		}
	}
	
	public static void nextRound() throws IOException {
		//totalDice -= 1;
		numberOfBidDice = 0;
		bidDieValue = 1;
		initDice(myDiceCount);
	}
	
	public static void send(PrintWriter writer, String message) {
		writer.println(message);
		writer.flush();
	}
	
	public static void doTurn() throws IOException {
		Scanner s = new Scanner(System.in);
		printDice();
		System.out.println("Your turn (type 'bid [quantity] [number]' or 'liar')");
		
		while (true) {
			String line = s.nextLine();
			String[] words = line.split(" ");
			if (words[0].equals("bid")) {
				try {
					int numDice = Integer.parseInt(words[1]);
					int value = Integer.parseInt(words[2]);
					
					if (numDice < numberOfBidDice || (numDice == numberOfBidDice && value <= bidDieValue)
							|| value > 6) {
						System.out.println("Invalid bid");
						continue;
					}
					
					send(toNext, username + " BID " + numDice + " " + value);
					fromPrev.readLine();
					
					//end turn
					send(toNext, "DO_TURN");
					break;
				} catch (NumberFormatException e) {
					System.out.println("Invalid bid");
					continue;
				}
			}
			
			if (words[0].equals("liar")) {
				String message = "TALLY ";
				for (int i=0; i<myDice.length; i++) {
					message += myDice[i] + " ";
				}
				
				send(toNext, message);
				
				message = fromPrev.readLine().trim();
				
				String[] tallyValues = message.split(" ");
				int bidDice = 0;
				for (int i=1; i<tallyValues.length; i++) {
					int dieVal = Integer.parseInt(tallyValues[i]);
					if (dieVal == bidDieValue) {
						bidDice++;
					}
				}
				if (bidDice >= numberOfBidDice) {
					// accuser loses a die
					myDiceCount--;
					if (myDiceCount == 0) {
						eliminated(true);
						break;
					}
					nextRound();
					send(toNext, "NEXT_ROUND");
					fromPrev.readLine();
				}
				else {
					// accused loses a die
					send(toNext, "LOSE_DIE " + prevUsername);
					break;
				}
				
			}
			
			System.out.println("Invalid input");
		}
		
		//s.close();
		
	}
	
	public static void eliminated(boolean prevActive) throws IOException {
		System.out.println("YOU WERE ELIMINATED.");
		gui.lose();
		send(toNext, "PLAYER_ELIMINATED " + username);
		fromPrev.readLine();
		numPlayers--;
		
		if (numPlayers == 1) return;
		
		send(toNext, "CONNECT_NEW " + prevUsername);
		send(toNext, "ACCEPT_NEW");
		
		String message = fromNext.readLine();
		send(toPrev, message);
		
		fromNext.readLine();
		fromPrev.readLine();
		
		if (prevActive) {
			send(toPrev, "DO_TURN");
			send(toNext, "DONT_DO_TURN_LOSER");
		} else {
			send(toPrev, "DONT_DO_TURN_LOSER");
			send(toNext, "DO_TURN");
		}
	}
	
	public static void acceptNew() throws IOException {
		System.out.println("\nConnecting to new peer...");
		
		PrintWriter toEliminated = toPrev;
		BufferedReader fromEliminated = fromPrev;
		send(toEliminated, InetAddress.getLocalHost().getHostName() + " " + myport);
		
		socket = myServer.accept();
		fromPrev = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		toPrev = new PrintWriter(socket.getOutputStream());
		prevUsername = fromPrev.readLine();
		send(toPrev, username);
		
		send(toEliminated, "READY");
		startFirst = fromEliminated.readLine().equals("DO_TURN");
		System.out.println("Connected to user " + prevUsername + "\n");
	}
	
	public static void connectNew() throws IOException {
		System.out.println("\nConnecting to new peer...");
		
		PrintWriter toEliminated = toNext;
		BufferedReader fromEliminated = fromNext;
		
		String[] message = fromEliminated.readLine().split(" ");
		String hostname = message[0];
		int port = Integer.parseInt(message[1]);
		
		socket = new Socket(hostname, port);
		fromNext = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		toNext = new PrintWriter(socket.getOutputStream());
		send(toNext, username);
		nextUsername = fromNext.readLine();
		
		send(toEliminated, "READY");
		startFirst = fromEliminated.readLine().equals("DO_TURN");
		System.out.println("Connected to user " + nextUsername + "\n");
	}

	public static boolean stringContains(String[] str1, String str2) {
		for (int i = 0; i < str1.length; i++) {
			if (str1[i].equals(str2)) {
				return true;
			}
		}
		return false;
	}
}
