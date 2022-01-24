package main.server;

import main.clientServerConversation.ClientRequests;
import main.clientServerConversation.ServerClientMessage;
import main.clientServerConversation.ServerRespond;
import main.clientServerConversation.UserData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static ExecutorService executorService = Executors.newFixedThreadPool(5);

    private static class Server implements Runnable{
        private Socket client;
        private ServerClientMessage clientRequest;
        private static final String USERS_DATA_FILE = "users_data.bin";
        private UserData currentUser;
        private static final Logger LOG = LogManager.getLogger("ServerLogger");

        public Server(Socket socket){
            this.client = socket;
        }

        @Override
        public void run(){
            try {
                LOG.trace("");
                LOG.trace("Server is connected to client");
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());

                boolean isAuthenticated = false;
                while (!isAuthenticated){
                    clientRequest = (ServerClientMessage) in.readObject();
                    currentUser = (UserData) clientRequest.getObject();
                    if (Objects.equals(clientRequest.getDescription(), ClientRequests.USER_AUTHENTICATION_REQUEST)) {
                        isAuthenticated = isUserAuthenticated();
                        out.writeObject(new ServerClientMessage(ServerRespond.USER_AUTHENTICATION_ANSWER,
                                isAuthenticated));
                        LOG.info("user {} authentication is {}", currentUser.getUsername(), isAuthenticated);
                    }
                    else if (Objects.equals(clientRequest.getDescription(), ClientRequests.USER_ACCOUNT_CREATION_REQUEST)){
                        if (isUserExist()){
                            out.writeObject(new ServerClientMessage(ServerRespond.USER_ACCOUNT_CREATION_ANSWER, false));
                            LOG.info("account for {} exist", currentUser.getUsername());
                        }
                        else{
                            writeNewUserData();
                            isAuthenticated = true;
                            LOG.info("account for {} created", currentUser.getUsername());
                            out.writeObject(new ServerClientMessage(ServerRespond.USER_ACCOUNT_CREATION_ANSWER, true));
                        }

                    }
                }

                out.flush();
                client.close();
                in.close();
                out.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private boolean isUserAuthenticated() throws IOException, ClassNotFoundException {
            FileInputStream fileInputStream = new FileInputStream(USERS_DATA_FILE);
            ObjectInputStream inputStream = new ObjectInputStream(fileInputStream);
            try {
                return searchUserData(inputStream);
            }
            catch (EOFException e){
                LOG.warn("No existing user {} with such password", currentUser.getUsername());
            }
            inputStream.close();
            return false;
        }

        private boolean searchUserData(ObjectInputStream inputStream) throws IOException, ClassNotFoundException, EOFException {
            UserData userData;
            while (true){
                userData = (UserData) inputStream.readObject();
                if (Objects.equals(userData.getUsername(), currentUser.getUsername()) &&
                        Objects.equals(userData.getUserPassword(), currentUser.getUserPassword())){
                    inputStream.close();
                    return true;
                }
            }
        }

        private boolean isUserExist() throws IOException, ClassNotFoundException {
            FileInputStream fileInputStream = new FileInputStream(USERS_DATA_FILE);
            ObjectInputStream inputStream = new ObjectInputStream(fileInputStream);
            try {
                return searchUserUsername(inputStream);
            }
            catch (EOFException e){
                LOG.warn("No existing user {} with such password", currentUser.getUsername());
            }
            inputStream.close();
            return false;
        }

        private boolean searchUserUsername(ObjectInputStream inputStream) throws IOException, ClassNotFoundException, EOFException {
            UserData userData;
            while (true){
                userData = (UserData) inputStream.readObject();
                if (Objects.equals(userData.getUsername(), currentUser.getUsername())){
                    inputStream.close();
                    return true;
                }
            }
        }

        private void writeNewUserData() throws IOException {

            FileOutputStream fileOutputStream = new FileOutputStream(USERS_DATA_FILE);
            ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
            outputStream.writeObject(currentUser);
            outputStream.flush();
            outputStream.close();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(7);

        while (!serverSocket.isClosed()){
            Socket client = serverSocket.accept();
            System.out.println("connected");
            executorService.execute(new Server(client));
            serverSocket.close();
        }
        executorService.shutdown();
    }
}

