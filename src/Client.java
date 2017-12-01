import java.io.*;
import java.net.*;
import java.util.Random;

public class Client {

	public static void main(String[] args) {
		String host, message, username;
		boolean active = false;
		int totalDice, myDiceCount, numberOfBidDice, bidDieValue;
		int[] myDice;
		Socket socket;
		ServerSocket myServer;
		BufferedReader fromServer = null, fromNext = null, fromPrev = null;
		PrintWriter toServer = null, toNext = null, toPrev = null;

		message = "";

		try {
			if (args.length < 3) {
				System.out.println("need at least 3 args");
				System.exit(1);
			}
			host = args[0];
			int myport = Integer.parseInt(args[1]);
			username = args[2];
			myServer = new ServerSocket(myport);

			socket = new Socket(host, 10501);
			fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			toServer = new PrintWriter(socket.getOutputStream());

			toServer.println("" + myport);
			toServer.flush();

			message = fromServer.readLine();
			totalDice = Integer.parseInt(message);
			myDiceCount = 5;
			
			Random rand = new Random();
			myDice = new int[5];
			for (int i = 0; i < myDice.length; i++) {
				myDice[i] = rand.nextInt(6) + 1;
			}
			
			numberOfBidDice = 0;
			bidDieValue = 1;

			for (int i = 0; i < 2; i++) {
				message = fromServer.readLine();
				if (message.equals("ACCEPT")) {
					socket = myServer.accept();
					fromPrev = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					toPrev = new PrintWriter(socket.getOutputStream());
				} else if (message.substring(0, 7).equals("CONNECT")) {
					String[] temp = message.split(" ");
					host = temp[1];
					int port = Integer.parseInt(temp[2]);
					socket = new Socket(host, port);
					fromNext = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					toNext = new PrintWriter(socket.getOutputStream());
				}
			}

			message = fromServer.readLine();
			if (message.equals("ACTIVE")) {
				active = true;
			}

			while (true) {
				if (active) {
					// TODO: Implement turn - make bid/accusation, send messages around ring, eliminate players/restructure ring as
					// necessary
					continue;
				}
				message = fromPrev.readLine();
				String[] parsedMessage = message.split(" ");
				if (stringContains(parsedMessage, "SET_ACTIVE")) {
					active = true;
					continue;
				}
				else if (stringContains(parsedMessage, username)) {
					continue;
				}
				else if (stringContains(parsedMessage, "TALLY")) {
					for (int i = 0; i < myDice.length; i++) {
						message += "" + myDice[i] + " ";
					}
					toNext.println(message.trim());
				}
				else if (stringContains(parsedMessage, "NEXT_ROUND")) {
					totalDice -= 1;
					numberOfBidDice = 0;
					bidDieValue = 1;
				}
				else {
					System.out.println(message);
					toNext.println(message);
					continue;
				}
			}
			// TODO: Inform server that this Client has been eliminated/has won the game. Once all Clients do so, Server will shut
			// everything down.
		} catch (Exception e) {
			System.err.println(e);
		}
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
