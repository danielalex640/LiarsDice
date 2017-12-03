import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Client {
	
	static ServerSocket myServer;
	static int myport;
	static Socket socket;
	static BufferedReader fromServer = null, fromNext = null, fromPrev = null;
	static PrintWriter toServer = null, toNext = null, toPrev = null;
	static String prevUsername, nextUsername;
	
	static int totalDice, myDiceCount, numberOfBidDice, bidDieValue;
	static int[] myDice;
	
	static boolean active = false;
	
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
		
		numberOfBidDice = 0;
		bidDieValue = 1;
	}

	public static void main(String[] args) {
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
			
			totalDice = Integer.parseInt(fromServer.readLine());
			
			myDiceCount = 5;
			initDice(myDiceCount);
			
			for (int i = 0; i < 2; i++) {
				message = fromServer.readLine();
				if (message.equals("ACCEPT")) {
					socket = myServer.accept();
					fromPrev = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					toPrev = new PrintWriter(socket.getOutputStream());
					prevUsername = fromPrev.readLine();
					toPrev.println(username);
				} else if (message.substring(0, 7).equals("CONNECT")) {
					String[] temp = message.split(" ");
					host = temp[1];
					int port = Integer.parseInt(temp[2]);
					socket = new Socket(host, port);
					fromNext = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					toNext = new PrintWriter(socket.getOutputStream());
					toNext.println(username);
					nextUsername = fromNext.readLine();
				}
			}

			message = fromServer.readLine();
			if (message.equals("ACTIVE")) {
				active = true;
			}

			mainLoop();
			
			// TODO: Inform server that this Client has been eliminated/has won the game. Once all Clients do so, Server will shut
			// everything down.
		} catch (Exception e) {
			System.err.println(e);
		}
	}
	
	public static void mainLoop() throws IOException {
		String message;
		while (true) {
			if (active) {
				// TODO: Implement turn - make bid/accusation, send messages around ring, eliminate players/restructure ring as
				// necessary
				doTurn();
				if (myDiceCount == 0) break;
				continue;
			}
			message = fromPrev.readLine();
			String[] parsedMessage = message.split(" ");
			if (stringContains(parsedMessage, "SET_ACTIVE")) {
				active = true;
				continue;
			}
			else if (stringContains(parsedMessage, "LOSE_DIE") && stringContains(parsedMessage, username)) {
				myDiceCount--;
				if (myDiceCount == 0) {
					eliminated(false);
					break;
				}
			}
			else if (stringContains(parsedMessage, username)) {
				continue;
			}
			else if (stringContains(parsedMessage, "TALLY")) {
				for (int i = 0; i < myDice.length; i++) {
					message += "" + myDice[i] + " ";
				}
				toNext.println(message);
			}
			else if (stringContains(parsedMessage, "NEXT_ROUND")) {
				totalDice -= 1;
				numberOfBidDice = 0;
				bidDieValue = 1;
				initDice(myDiceCount);
			}
			else if (stringContains(parsedMessage, "BID")) {
				System.out.println(message);
				numberOfBidDice = Integer.parseInt(parsedMessage[2]);
				bidDieValue = Integer.parseInt(parsedMessage[3]);
				toNext.println(message);
			}
			else if (stringContains(parsedMessage, "ACCEPT_NEW")) {
				acceptNew();
			}
			else if (stringContains(parsedMessage, "CONNECT_NEW") && stringContains(parsedMessage, username)) {
				connectNew();
			}
			else {
				System.out.println(message);
				toNext.println(message);
				continue;
			}
		}
	}
	
	public static void doTurn() throws IOException {
		System.out.print("Your dice: ");
		for (int i=0; i<myDice.length; i++) {
			System.out.print(myDice[i] + " ");
		}
		System.out.println();
		
		Scanner s = new Scanner(System.in);
		System.out.println("WHAT DO");
		
		while (true) {
			String line = s.nextLine();
			String[] words = line.split(" ");
			if (words[0].equals("bid")) {
				try {
					int numDice = Integer.parseInt(words[1]);
					int value = Integer.parseInt(words[2]);
					
					if (numDice < numberOfBidDice || (numDice == numberOfBidDice && value <= bidDieValue)
							|| value > 6) {
						System.out.println("Incorrect bid");
						continue;
					}
					
					toNext.println(username + " BID " + numDice + " " + value);
					fromPrev.readLine();
					
					//end turn
					active = false;
					toNext.println("SET_ACTIVE");
					
					break;
				} catch (NumberFormatException e) {
					System.out.println("HOW COULD YOU DO THIS TO ME, HOW COULD YOU BETRAY MY TRUST LIKE THAT? I THOUGHT WE WERE FRIENDS"
							+ " MAN, I THOUGHT WE WERE FRIENDS. ANYWAY TRY AGAIN.");
					continue;
				}
			}
			
			if (words[0].equals("liar")) {
				String message = "TALLY ";
				for (int i=0; i<myDice.length; i++) {
					message += myDice[i] + " ";
				}
				
				toNext.println(message);
				
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
				}
				else {
					// accused loses a die
					toNext.println("LOSE_DIE " + prevUsername);
					active = false;
					break;
				}
				
			}
			
			System.out.println("Invalid input");
		}
		
		
		
	}
	
	public static void eliminated(boolean prevActive) throws IOException {
		toNext.println("CONNECT_NEW " + prevUsername);
		toNext.println("ACCEPT_NEW");
		
		String message = fromNext.readLine();
		toPrev.println(message);
		
		fromNext.readLine();
		fromPrev.readLine();
		
		if (prevActive) {
			toPrev.println("ACTIVE");
			toNext.println("INACTIVE");
		} else {
			toPrev.println("INACTIVE");
			toNext.println("ACTIVE");
		}
	}
	
	public static void acceptNew() throws IOException {
		PrintWriter toEliminated = toPrev;
		BufferedReader fromEliminated = fromPrev;
		toEliminated.println(InetAddress.getLocalHost().getHostName() + " " + myport);
		
		socket = myServer.accept();
		fromPrev = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		toPrev = new PrintWriter(socket.getOutputStream());
		prevUsername = fromPrev.readLine();
		toPrev.println(username);
		
		toEliminated.println("READY");
		
		String message = fromEliminated.readLine();
		active = message.equals("ACTIVE");
	}
	
	public static void connectNew() {
		
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
