import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.UUID;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String id = UUID.randomUUID().toString();
    private String nickname;
    Scanner scanner = new Scanner(System.in);

    public Client(String host, int port) {
        try {
            this.socket = new Socket(host, port);
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Enter your nickname for the groupchat: ");
            this.nickname = scanner.nextLine();
        } catch (IOException e) {
            System.out.println("Couldn't connect to the server!");
            closeEverything();
        }
    }

    public void sendMessage() {
        try {
            broadcast(id);
            broadcast(nickname);

            while (socket.isConnected()) {
                String message = scanner.nextLine();
                if (!message.isBlank()) {
                    if (message.startsWith("/")) {
                        handleCommand(message);
                    } else {
                        broadcast(nickname + ": " + message);
                    }
                }
            }

        } catch (IOException e) {
            closeEverything();
        }
    }

    public void recieveMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String messageRecieved;

                while (socket.isConnected()) {
                    try {
                        messageRecieved = bufferedReader.readLine();
                        System.out.println(messageRecieved);
                    } catch (IOException e) {
                        closeEverything();
                    }
                }
            }
        }).start();
    }

    public void broadcast(String messsage) {
        try {
            bufferedWriter.write(messsage);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void handleCommand(String message) throws IOException {
        String[] messageSplit = message.split(" ");
        String command = messageSplit[0];
        String[] args = new String[messageSplit.length - 1];
        for (int i = 0; i < messageSplit.length - 1; i++) {
            args[i] = messageSplit[i + 1];
        }

        switch (command) {
            case "/nick":
                if (args.length == 1) {
                    broadcast(nickname + " changes their nickname to " + args[0]);
                    serverCommand("nick " + args[0]);

                    nickname = messageSplit[1];
                    System.out.println("Successfully changed nickname to " + nickname);
                } else {
                    System.out.println("To change nickname use the format - /nick newUsername");
                }
                break;
            case "/quit":
                serverCommand("quit");
                System.out.println("Left the chat succesfully!");
                closeEverything();
                break;
            case "/list":
                serverCommand("list");
        }
    }

    public void serverCommand(String command) {
        broadcast("#srvCommand " + command);
    }

    public void closeEverything() {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client("localhost", 9999);
        client.recieveMessage();
        client.sendMessage();
    }
}