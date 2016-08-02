
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class FTPClient {
	static BufferedReader br;
	static String input = null;
	static int portNumber;
	static int connectCall;
	static final String CRLF = "\r\n";
	static Socket clientSocket;
	static ServerSocket welcomeSocket;
	static String response;
	static String send;
	static DataOutputStream outToServer;
	static BufferedReader inFromServer;
	static int fileNumber = 1;
	static Socket connectionSocket;
	
	public static void main(String[] args) throws Exception {

		br = new BufferedReader(new InputStreamReader(System.in));
		portNumber = Integer.parseInt(args[0]);
		connectCall = 0;

		try {

			// this for loops takes each line of the file and prints it out then
			// sends it to my parse method
			for (String input = br.readLine(); input != null; input = br
					.readLine()) {
				System.out.println(input);
				parse(input);
			}
			br.close();

		} catch (IOException e) {
		}

	}
	
	private static void parse(String input) throws Exception, IOException {
		ArrayList<String> tokens = new ArrayList<String>();
		StringTokenizer tokenizedLine = new StringTokenizer(input);

		// adds every token into the ArrayList to be parsed.
		while (tokenizedLine.hasMoreTokens()) {
			tokens.add(tokenizedLine.nextToken());
		}

		String command = tokens.get(0);
		String parameter;

		// this is to verify that connect has been called first and foremost
		if (command.equalsIgnoreCase("CONNECT")) {
			connectCall++;
		}

		if (connectCall > 0) {

			if (command.equalsIgnoreCase("CONNECT")) {
				if (tokens.size() > 1) {
					// this regex checks to see if it is a valid domain in three
					// different type of flavors
					if (domainCheck(tokens.get(1)) && tokens.size() > 2) {
						String serverHost = tokens.get(1);
						if (tokens.size() > 2) {
							Integer serverPort = Integer
									.parseInt(tokens.get(2));
							// this checks to make sure it is in the valid range
							// for a server port
							if (serverPort >= 0 && serverPort < 65536) {
								/*
								 * This block is meant to test if a connection is already open with a server
								 * if it is open it sends over a text cue to shut down the current connection
								 * once it's closed it's read to be set up again through the regular schedule
								 */
								if(clientSocket != null){
									if(clientSocket.isConnected()){
										outToServer.close();
										inFromServer.close();
										clientSocket.close();
									}
								}
								//connecting to the FTPServer
								try{
								clientSocket = new Socket(serverHost, serverPort);
								
								outToServer = new DataOutputStream(
										clientSocket.getOutputStream());
								inFromServer = new BufferedReader(
										new InputStreamReader(clientSocket.getInputStream()));
								}catch(IOException io){
									System.out.printf(
											"CONNECT failed" + "%s", CRLF);
									return;
								}
								
								System.out.printf(
										"CONNECT accepted for FTP server at host "
												+ serverHost + " and port "
												+ serverPort + "%s", CRLF);
								
								//original response from server
								response = inFromServer.readLine();
								parseResponse(response);
								
								
								System.out.printf("USER anonymous%s", CRLF);			
								push("USER anonymous");
																
								
								System.out.printf("PASS guest@%s", CRLF);
								push("PASS guest@");
								
								
								System.out.printf("SYST%s", CRLF);
								push("SYST");
								
								
								System.out.printf("TYPE I%s", CRLF);
								push("TYPE I");
								

							} else {
								System.out.println("ERROR -- server-port");
							}

						} else {
							System.out.println("ERROR -- server-port");
						}
					} else {
						System.out.println("ERROR -- server-host");
					}

				} else {
					System.out.println("ERROR -- server-host");
				}
			} else if (command.equalsIgnoreCase("GET")) {
				// checking to make sure it is a valid command with two
				// parameters.
				if (tokens.size() >= 2) {
					parameter = tokens.get(1);
					// check to make sure it is with then ASCII range
					if (parameter.matches("\\A\\p{ASCII}*\\z")) {
						String pathName = space(input, parameter);
						System.out.printf("GET accepted for " + pathName + "%s", CRLF);
						String myIP;
						InetAddress myInet;
						try {
							myInet = InetAddress.getLocalHost();
							myIP = myInet.getHostAddress();
							String myIPFinal = myIP.replace(".", ",");
							// these equations below are used in order to
							// compute the portNumber the two that are used are
							// more dynamic than the ones in comments
							// int x = 31;
							// int y = (64 + portNumber);
							// int portNumber = (x * 256) + y;
							int x = portNumber / 256;
							int y = portNumber % 256;
							System.out.printf("PORT " + myIPFinal + "," + x
									+ "," + y + "%s", CRLF);
							
							//establish connection with FTPServer
							try{
							welcomeSocket = new ServerSocket(portNumber);

							push("PORT " + myIPFinal + "," + x
									+ "," + y);
							
							
							System.out.printf("RETR " + pathName + "%s", CRLF);
							
							//had to move this here for illegal file testing*
							//this writes the string and EOL onto the wire
							outToServer.writeBytes("RETR " + pathName + CRLF);
							//this pushes the string to the server
							outToServer.flush();
							//incoming message from server
							response = inFromServer.readLine();
							//handles the FTP server code and handles 4xx and 5xx errors
							parseResponse(response);
							
							if(response.equals("550 File not found or access denied.")){
								return;
							}
							
							//this opens the data connection socket
							connectionSocket = welcomeSocket.accept();
							
							int bytesRead;

							DataInputStream clientData = new DataInputStream(connectionSocket.getInputStream());

							File destination = new File("retr_files/file" + fileNumber);
							OutputStream output = new FileOutputStream(destination);
							long size = clientData.readLong();
							byte[] buffer = new byte[1024];
							//Writes the bytes from the FTPServer into a new File on the CLient side it terminates at the EOF (-1).
							while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
							        output.write(buffer, 0, bytesRead);
							        size -= bytesRead;
							 }

							//closing all the connections and streams
							 output.close();
							 clientData.close();
							 connectionSocket.close();
							
							response = inFromServer.readLine();
							parseResponse(response);
							
							
							portNumber++;
							fileNumber++;
							}catch(Exception e){
								connectionSocket.close();
								System.out.printf("GET failed, FTP-data port not allocated." + "%s", CRLF);
								return;
							}
							
						} catch (UnknownHostException e) {
						}

					} else {
						System.out.println("ERROR -- pathname");
					}
				} else {
					System.out.println("ERROR -- pathname");
				}

			} else if (command.equalsIgnoreCase("QUIT")) {
				//this guarantees it isn't a falsified parameter e.g. QUIT123
				if (input.length() == 4) {
					System.out.println("QUIT accepted, terminating FTP client");
					System.out.printf("QUIT%s", CRLF);
					push("QUIT");
					clientSocket.close();
				} else {
					System.out.println("ERROR -- request");
				}
			} else {
				System.out.println("ERROR -- request");
			}
		} else {
			System.out.println("ERROR -- expecting CONNECT");
		}
	}

	// this method is used to detect any spaces that are at the end of a file
	// name along with the spaces between the words if any at all.
	private static String space(String full, String partial) {
		return full.substring(full.indexOf(partial));

	}
	
	private static boolean domainCheck(String s){
		boolean invalid = false;
		if(s.length() < 2){
			return invalid;
		}
		if(!Character.isLetter(s.charAt(0))){
			return invalid;
		}
		for(int i = 0; i < s.length(); i++ ){
			if(s.charAt(i) > 127 || s.charAt(i) < 0){
				return invalid;
			}
		}
		String[] check = s.split(".");
		for(String s2: check){
			if(s2.length() < 2){
				return invalid;
			}
		}
		if(s.charAt(0) == '.' || s.charAt(s.length()-1) == '.'){
			return invalid;
		}
		
		return true;
	}
	private static void parseResponse(String input) {
		ArrayList<String> tokens = new ArrayList<String>();
		StringTokenizer tokenizedLine = new StringTokenizer(input);

		// adds every token into the ArrayList to be parsed.
		while (tokenizedLine.hasMoreTokens()) {
			tokens.add(tokenizedLine.nextToken());
		}

		//this logic block goes on to check the parameters for reply codes and reply text
		String replyCode = tokens.get(0);
		//this check is to ensure it is a 3 digit integer 
		if (replyCode.length() == 3 && isValidNumber(replyCode)) {
			Integer checkDigits = Integer.parseInt(replyCode);
			//this checks the reply code parameter of range
			if (checkDigits >= 100 && checkDigits <= 599) {
				if (tokens.size() >= 2 || input.length() > 4) {
					int check = 0;
					int correct = input.length() - 3;
					String concatString = "";
					// this for loop foes through each token checking to make sure it a valid ASCII character and then concatenates it onto the empty string
					for (int i = 3; i < input.length(); i++) {
						String checker = String.valueOf(input.charAt(i));
						 if (checker.matches("\\A\\p{ASCII}*\\z")) {
							check++;
							concatString += input.charAt(i);
						}
					}

					if (check == correct) {
						// this removes the space on the end from the previous for loop
						String replytext = concatString.substring(0, concatString.length());
						System.out.println("FTP reply " + checkDigits
								+ " accepted. Text is : " + replytext);
					} else {
						System.out.println("ERROR -- reply-text");
					}

				} else {
					System.out.println("ERROR -- reply-text");
				}

			} else {
				System.out.println("ERROR -- reply-code");
			}
		} else {
			System.out.println("ERROR -- reply-code");
		}

	}
	
	private static void push(String s) throws Exception{
		//this writes the string and EOL onto the wire
		outToServer.writeBytes(s + CRLF);
		//this pushes the string to the server
		outToServer.flush();
		//incoming message from server
		response = inFromServer.readLine();
		//handles the FTP server code and handles 4xx and 5xx errors
		parseResponse(response);
	}
	
	private static boolean isValidNumber(String value) {
		// Loop over all characters in the String.
		// ... If isDigit is false, this method too returns false.
		for (int i = 0; i < value.length(); i++) {
		    if (!Character.isDigit(value.charAt(i))) {
			return false;
		    }
		}
		return true;
	    }

}
