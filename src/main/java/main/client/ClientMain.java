package main.client;

import main.clientServerConversation.ClientRequests;
import main.clientServerConversation.ServerClientMessage;
import main.clientServerConversation.ServerRespond;
import main.clientServerConversation.UserData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class ClientMain {
    private static class Client {
        private ServerClientMessage serverAnswer;
        private UserData userData;
        private final Scanner in;
        private static final Logger LOG = LogManager.getLogger("ClientThreadInputLogger");

        ObjectOutputStream outSocket;
        ObjectInputStream inSocket;


        public Client(){
            LOG.trace("");
            in = new Scanner(System.in);
            try(Socket socket = new Socket("localhost", 7)) {
                LOG.trace("Connection to the server is successful");
                DataOutputStream outData = new DataOutputStream(socket.getOutputStream());
                outSocket = new ObjectOutputStream(outData);
                DataInputStream inData = new DataInputStream(socket.getInputStream());
                inSocket = new ObjectInputStream(inData);

                boolean answer = false;
                int userAction;
                while (!answer){
                    userAction = inputStartInfo();
                    if (userAction == 1){
                        LOG.info("user want to authenticate existing account");
                        try {
                            tryToAuthenticate();
                            answer = true;
                            LOG.info("{} authenticated successfully", userData.getUsername());
                        }
                        catch (IllegalArgumentException e){
                            LOG.warn("wrong username or password");
                        }
                    }
                    else if (userAction == 2){
                        LOG.info("user want to create new account");
                        try {
                            tryToCreateAccount();
                            answer = true;
                            LOG.info("{} account created successfully", userData.getUsername());
                        }
                        catch (IllegalArgumentException e){
                            LOG.warn("User with this username already exist");
                        }
                    }
                }


                socket.close();
                inSocket.close();
                outSocket.close();
            } catch (UnknownHostException e){
                System.err.println("Такого хоста не существует");
                LOG.fatal("Don't know about host, {}", Arrays.toString(e.getStackTrace()));
            } catch (IOException e) {
                System.err.println("Ошибка соединения с сервером");
                LOG.fatal("Server connection exception, {}", Arrays.toString(e.getStackTrace()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private int inputStartInfo(){
            System.out.println("Введите 1, если хотите войти в существующий аккаунт, " +
                    "или 2, если хотите создать новый аккаунт:");
            try{
                int number = Integer.parseInt(in.nextLine());
                if (number == 1 || number == 2)
                    return number;
                else return 0;
            }catch (NumberFormatException e){
                return 0;
            }
        }

        private void inputUserInfo(){
            System.out.println("Введите имя пользователя:");
            String username = in.nextLine();
            System.out.println("Введите пароль пользователя:");
            String userPassword = in.nextLine();
            userData = new UserData(username, userPassword);
        }

        private void tryToAuthenticate() throws IOException, ClassNotFoundException {
            inputUserInfo();
            outSocket.writeObject(new ServerClientMessage(ClientRequests.USER_AUTHENTICATION_REQUEST, userData));
            serverAnswer = (ServerClientMessage)inSocket.readObject();
            if (Objects.equals(serverAnswer.getDescription(), ServerRespond.USER_AUTHENTICATION_ANSWER) &&
                    (boolean)serverAnswer.getObject()){
                System.out.println("Вы успешно вошли");
            }
            else{
                System.out.println("Неверное имя пользователя или пароль");
                throw new IllegalArgumentException("Wrong username or password");
            }
        }

        private void tryToCreateAccount() throws IOException, ClassNotFoundException {
            inputUserInfo();
            outSocket.writeObject(new ServerClientMessage(ClientRequests.USER_ACCOUNT_CREATION_REQUEST, userData));
            serverAnswer = (ServerClientMessage)inSocket.readObject();
            if (Objects.equals(serverAnswer.getDescription(), ServerRespond.USER_ACCOUNT_CREATION_ANSWER) &&
                    (boolean)serverAnswer.getObject()){
                System.out.println("Ваш аккаунт успешно создан");
            }
            else{
                System.out.println("Пользователь с таким именем уже существует");
                throw new IllegalArgumentException("User with this username already exist");
            }
        }
    }


    public static void main(String[] args) {
        new Client();
    }
}

