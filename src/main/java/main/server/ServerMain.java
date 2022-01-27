package main.server;

import Tasks.Task;
import Tasks.TaskLogHandler;
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
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(7);

        while (!serverSocket.isClosed()) {
            Socket client = serverSocket.accept();
            System.out.println("connected");
            executorService.execute(new Server(client));
            serverSocket.close();
        }
        executorService.shutdown();
    }

    private static class Server implements Runnable {
        private static final String USERS_DATA_FILE = "users_data.bin";
        private static final String SERVER_RESOURCES_PATH = "src/main/java/main/server/resources/";
        private static final Logger LOG = LogManager.getLogger("ServerLogger");
        private final Socket client;
        private final TaskLogHandler taskLogHandler;
        private UserData currentUser;

        public Server(Socket socket) {
            this.client = socket;
            taskLogHandler = new TaskLogHandler();
        }

        @Override
        public void run() {
            try {
                LOG.trace("");
                LOG.trace("Server is connected to client");
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                boolean isAuthenticated = false;
                new File(SERVER_RESOURCES_PATH + USERS_DATA_FILE).createNewFile();

                while (!client.isClosed()) {
                    ServerClientMessage clientRequest = (ServerClientMessage) in.readObject();
                    switch ((ClientRequests) clientRequest.getDescription()) {
                        case USER_AUTHENTICATION_REQUEST -> {
                            currentUser = (UserData) clientRequest.getObject();
                            isAuthenticated = isUserAuthenticated();
                            LOG.info("user {} authentication is {}", currentUser.getUsername(), isAuthenticated);
                            out.writeObject(new ServerClientMessage(ServerRespond.USER_AUTHENTICATION_ANSWER,
                                    isAuthenticated));
                        }
                        case USER_ACCOUNT_CREATION_REQUEST -> {
                            currentUser = (UserData) clientRequest.getObject();
                            if (isUserExist()) {
                                LOG.info("account for {} exist", currentUser.getUsername());
                                out.writeObject(new ServerClientMessage(ServerRespond.USER_ACCOUNT_CREATION_ANSWER, false));
                            } else {
                                writeNewUserData();
                                isAuthenticated = true;
                                LOG.info("account for {} created", currentUser.getUsername());
                                out.writeObject(new ServerClientMessage(ServerRespond.USER_ACCOUNT_CREATION_ANSWER, true));
                            }
                        }
                        case USER_TASK_LOG_HANDLER_REQUEST -> {
                            if (isAuthenticated) {
                                FileInputStream fileInputStream =
                                        new FileInputStream(SERVER_RESOURCES_PATH + currentUser.getUsername() + ".bin");
                                taskLogHandler.deserializeBuilding(fileInputStream);
                                LOG.info("taskLogHandler deserialized and send to client");
                                out.writeObject(new ServerClientMessage(ServerRespond.USER_TASK_LOG_HANDLER_ANSWER,
                                        taskLogHandler));
                            } else {
                                LOG.warn("User wasn't authenticated");
                                out.writeObject(new ServerClientMessage(ServerRespond.USER_TASK_LOG_HANDLER_ANSWER,
                                        new WrongUserDataException("User wasn't authenticated")));
                            }
                        }
                        case NEW_TASK_ADDITION_REQUEST -> {
                            if (isAuthenticated) {
                                taskLogHandler.addTask((Task) clientRequest.getObject());
                                LOG.info("Task added successfully");
                                out.writeObject(new ServerClientMessage(ServerRespond.USER_TASK_LOG_HANDLER_ANSWER,
                                        taskLogHandler));
                            } else {
                                LOG.warn("User wasn't authenticated");
                                out.writeObject(new ServerClientMessage(ServerRespond.NEW_TASK_ADDITION_ANSWER,
                                        new WrongUserDataException("User wasn't authenticated")));
                            }
                        }
                        case TASK_DELETE_REQUEST -> {
                            if (isAuthenticated) {
                                taskLogHandler.deleteTask((int) clientRequest.getObject());
                                LOG.info("Task deleted successfully");
                                out.writeObject(new ServerClientMessage(ServerRespond.TASK_DELETE_ANSWER,
                                        taskLogHandler));
                            } else {
                                LOG.warn("User wasn't authenticated");
                                out.writeObject(new ServerClientMessage(ServerRespond.USER_TASK_LOG_HANDLER_ANSWER,
                                        new WrongUserDataException("User wasn't authenticated")));
                            }
                        }
                        case END_OF_WORK -> {
                            LOG.info("User want to finish work");
                            out.writeObject(new ServerClientMessage(ServerRespond.END_OF_WORK, null));
                            out.flush();
                            client.close();
                            in.close();
                            out.close();
                        }
                    }
                }
                String filename = currentUser.getUsername() + ".bin";
                FileOutputStream fileOutputStream = new FileOutputStream(SERVER_RESOURCES_PATH + filename);
                taskLogHandler.serializeTaskLog(fileOutputStream);
                LOG.info("taskLogHandler serialized successfully");
                LOG.trace("Server finish work");
            } catch (ClassNotFoundException e) {
                LOG.fatal("No correct class ", e);
            } catch (IOException e) {
                LOG.fatal("IOException ", e);
            }

        }

        private boolean isUserAuthenticated() throws IOException, ClassNotFoundException {
            try (FileInputStream fileInputStream = new FileInputStream(SERVER_RESOURCES_PATH + USERS_DATA_FILE);
                 ObjectInputStream inputStream = new ObjectInputStream(fileInputStream)) {
                return searchUserData(inputStream);
            } catch (EOFException e) {
                LOG.warn("No existing user {} with such password", currentUser.getUsername());
            }
            return false;
        }

        private boolean searchUserData(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
            UserData userData = searchUserUsername(inputStream);
            if (Objects.equals(userData.getUsername(), currentUser.getUsername()) &&
                    Objects.equals(userData.getUserPassword(), currentUser.getUserPassword())) {
                inputStream.close();
                return true;
            }
            return false;
        }

        private boolean isUserExist() throws IOException, ClassNotFoundException {
            try (FileInputStream fileInputStream = new FileInputStream(SERVER_RESOURCES_PATH + USERS_DATA_FILE);
                 ObjectInputStream inputStream = new ObjectInputStream(fileInputStream)) {
                searchUserUsername(inputStream);
                return true;
            } catch (EOFException e) {
                LOG.warn("No existing user {} with such password", currentUser.getUsername());
            }
            return false;
        }

        private UserData searchUserUsername(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
            UserData userData;
            while (true) {
                userData = (UserData) inputStream.readObject();
                if (Objects.equals(userData.getUsername(), currentUser.getUsername())) {
                    inputStream.close();
                    return userData;
                }
            }
        }

        private void writeNewUserData() throws IOException {
            FileOutputStream fileOutputStream = new FileOutputStream(SERVER_RESOURCES_PATH + USERS_DATA_FILE);
            ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
            outputStream.writeObject(currentUser);
            outputStream.flush();
            outputStream.close();
            String filename = currentUser.getUsername() + ".bin";
            new File(SERVER_RESOURCES_PATH + filename).createNewFile();
        }
    }
}

