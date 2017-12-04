import java.io.*;
import java.net.*;

public class Server {
	
	public static void main(String[] args) {
		ServerSocket theServer;
		Socket socket = null;
		int count = Integer.parseInt(args[0]);
		
		BufferedReader fromClients[] = new BufferedReader[count];
		PrintWriter toClients[] = new PrintWriter[count];
		InetAddress inetAddrs[] = new InetAddress[count];
		int[] ports = new int[count];
		
		try {
			// Create a server socket to listen for a connection on port 10501 with backlog equal to count
			theServer = new ServerSocket(10501, count);
			for (int i = 0; i < count; i++) {
				System.out.println();
				System.out.println("Server: Waiting for a connection");
				// Wait for a client
				socket = theServer.accept();
				inetAddrs[i] = socket.getLocalAddress();
				System.out.println("Thread: Connection from " + inetAddrs[i].toString());
				// Create client input/output streams
				fromClients[i] = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				toClients[i] = new PrintWriter(socket.getOutputStream());
				
				String newPort = fromClients[i].readLine();
				ports[i] = Integer.parseInt(newPort);
				toClients[i].println("" + count);
				toClients[i].flush();
			}
			for (int i = 0; i < count; i++) {
				int clientID = i;
				int serverID = (i+1) % count;
				toClients[serverID].println("ACCEPT");
				toClients[serverID].flush();
				System.out.println("Sent ACCEPT to " + (serverID+1));
				String message = "CONNECT " + inetAddrs[serverID].getLocalHost().getHostName() + " " + ports[serverID];
				toClients[clientID].println(message);
				toClients[clientID].flush();
				System.out.println("Sent " + message + " to " + (clientID+1));
			}
			
			toClients[0].println("START_FIRST");
			toClients[0].flush();
			for (int i = 1; i < count; i++) {
				toClients[i].println("INACTIVE");
				toClients[i].flush();
			}
			
			// End the game
			for (int i = 0; i < count; i++) {
				fromClients[i].readLine();
				toClients[i].close();
				fromClients[i].close();
			}
			
			socket.close();
			
			System.out.println("All clients exited");
		}
		catch (Exception e) {
			System.err.println(e);
		}
	}
	
}
