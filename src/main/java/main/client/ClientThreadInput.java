package main.client;

import Tasks.Task;
import Tasks.TaskLogHandler;
import main.clientServerConversation.ClientRequests;
import main.clientServerConversation.ServerClientMessage;
import main.clientServerConversation.ServerRespond;
import main.clientServerConversation.UserData;
import main.server.WrongUserDataException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;

public class ClientThreadInput extends WorkingThread {
    private static final Logger LOG = LogManager.getLogger("ClientThreadInputLogger");
    private final Scanner in;
    private final DateFormat format = new SimpleDateFormat("yyyy M d kk:mm");
    private ObjectOutputStream outSocket;
    private ObjectInputStream inSocket;
    private ClientThreadAlarm threadAlarm;
    private ServerClientMessage serverAnswer;
    private UserData userData;
    private TaskLogHandler taskLogHandler;

    public ClientThreadInput() {
        super();
        in = new Scanner(System.in);
        LOG.trace("ClientThreadInput is running");
    }

    @Override
    public void run() {
        LOG.trace("");
        try (Socket socket = new Socket("localhost", 7)) {
            LOG.trace("Connection to the server is successful");
            DataOutputStream outData = new DataOutputStream(socket.getOutputStream());
            outSocket = new ObjectOutputStream(outData);
            DataInputStream inData = new DataInputStream(socket.getInputStream());
            inSocket = new ObjectInputStream(inData);

            boolean isAuthenticated = false;
            int userAction;
            while (!isAuthenticated) {
                userAction = inputStartInfo();
                if (userAction == 1) {
                    LOG.info("user want to authenticate existing account");
                    try {
                        tryToAuthenticate();
                        isAuthenticated = true;
                        LOG.info("{} authenticated successfully", userData.getUsername());
                    } catch (WrongUserDataException e) {
                        LOG.warn("Wrong username or password");
                    }
                } else if (userAction == 2) {
                    LOG.info("user want to create new account");
                    try {
                        tryToCreateAccount();
                        isAuthenticated = true;
                        LOG.info("{} account created successfully", userData.getUsername());
                    } catch (WrongUserDataException e) {
                        LOG.warn("User with this username already exist");
                    }
                }
            }
            setTaskLogHandler();

            threadAlarm = new ClientThreadAlarm(taskLogHandler);
            threadAlarm.start();

            int number;
            running = true;
            while (running) {
                try {
                    Thread.sleep(INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.fatal("ThreadInput is interrupted");
                }

                writeUserScript();
                try {
                    number = Integer.parseInt(in.nextLine());
                } catch (NumberFormatException e) {
                    number = -1;
                }
                LOG.info("Next action is " + number);
                switch (number) {
                    case 0 -> finishWork();
                    case 1 -> addTask();
                    case 2 -> deleteTask();
                    case 3 -> System.out.println(taskLogHandler.toString());
                }
            }

            socket.close();
            inSocket.close();
            outSocket.close();
            LOG.trace("Client finish work");
        } catch (UnknownHostException e) {
            System.err.println("Такого хоста не существует");
            LOG.fatal("Don't know about host, ", e);
        } catch (IOException e) {
            System.err.println("Ошибка соединения с сервером");
            LOG.fatal("Server connection exception, ", e);
        } catch (ClassNotFoundException e) {
            System.err.println("Внутренняя ошибка программы");
            LOG.fatal("No correct class, ", e);
        } catch (WrongUserDataException e) {
            System.err.println("Попытка обращения к серверу без авторизации");
            LOG.fatal("User wasn't authenticated, ", e);
        }
    }

    private int inputStartInfo() {
        System.out.println("Введите 1, если хотите войти в существующий аккаунт, " +
                "или 2, если хотите создать новый аккаунт:");
        try {
            int number = Integer.parseInt(in.nextLine());
            if (number == 1 || number == 2)
                return number;
            else return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void inputUserInfo() {
        System.out.println("Введите имя пользователя:");
        String username = in.nextLine();
        System.out.println("Введите пароль пользователя:");
        String userPassword = in.nextLine();
        userData = new UserData(username, userPassword);
    }

    private void tryToAuthenticate() throws IOException, ClassNotFoundException, WrongUserDataException {
        inputUserInfo();
        outSocket.writeObject(new ServerClientMessage(ClientRequests.USER_AUTHENTICATION_REQUEST, userData));
        serverAnswer = (ServerClientMessage) inSocket.readObject();
        if (Objects.equals(serverAnswer.getDescription(), ServerRespond.USER_AUTHENTICATION_ANSWER) &&
                (boolean) serverAnswer.getObject()) {
            System.out.println("Вы успешно вошли");
        } else {
            System.out.println("Неверное имя пользователя или пароль");
            throw new WrongUserDataException("Wrong username or password");
        }
    }

    private void tryToCreateAccount() throws IOException, ClassNotFoundException, WrongUserDataException {
        inputUserInfo();
        outSocket.writeObject(new ServerClientMessage(ClientRequests.USER_ACCOUNT_CREATION_REQUEST, userData));
        serverAnswer = (ServerClientMessage) inSocket.readObject();
        if (Objects.equals(serverAnswer.getDescription(), ServerRespond.USER_ACCOUNT_CREATION_ANSWER) &&
                (boolean) serverAnswer.getObject()) {
            System.out.println("Ваш аккаунт успешно создан");
        } else {
            System.out.println("Пользователь с таким именем уже существует");
            throw new WrongUserDataException("User with this username already exist");
        }
    }

    private void writeUserScript() {
        System.out.println("Введите номер действия:");
        System.out.println("0 - выход из программы");
        System.out.println("1 - ввод новой задачи");
        System.out.println("2 - удаление задачи из списка");
        System.out.println("3 - вывод всех задач на экран");
    }

    private Task readTask(Scanner in) {
        System.out.println("Введите название задачи:");
        String name = in.nextLine();

        System.out.println("Введите описание задачи:");
        String description = in.nextLine();

        System.out.println("Введите дату выполнения задачи в формате: гггг ММ дд чч:мм");
        Date date = new Date();
        try {
            date = format.parse(in.nextLine());
        } catch (ParseException e) {
            LOG.warn("Input date format was wrong");
            System.out.println("Введён неверный формат даты, " +
                    "дедлайн будет установлени на один час от текущего времени");
            date = addHourToDate(date);
        }

        System.out.println("Введите контакты задачи:");
        String contacts = in.nextLine();
        LOG.info("Task read successfully");
        return new Task(name, description, date, contacts);
    }

    public Date addHourToDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        return calendar.getTime();
    }

    private int readIdToDeleteTask(Scanner in) {
        System.out.println("Введите id удаляемой задачи:");
        int id;
        try {
            id = Integer.parseInt(in.nextLine());
            LOG.info("Id to delete task read successfully");
        } catch (NumberFormatException e) {
            id = -1;
            LOG.warn("Id to delete task is wrong");
        }
        return id;
    }

    private void setTaskLogHandler() throws IOException, ClassNotFoundException, WrongUserDataException {
        outSocket.writeObject(new ServerClientMessage(ClientRequests.USER_TASK_LOG_HANDLER_REQUEST, null));
        serverAnswer = (ServerClientMessage) inSocket.readObject();
        if (serverAnswer.getDescription() == ServerRespond.USER_NOT_AUTHENTICATED)
            throw new WrongUserDataException((String) serverAnswer.getObject());
        taskLogHandler = (TaskLogHandler) serverAnswer.getObject();
    }

    private void finishWork() throws IOException, ClassNotFoundException {
        outSocket.writeObject(new ServerClientMessage(ClientRequests.END_OF_WORK, null));
        serverAnswer = (ServerClientMessage) inSocket.readObject();
        threadAlarm.interrupt();
        running = false;
        LOG.info("User want to finish work");
    }

    private void addTask() throws IOException, ClassNotFoundException, WrongUserDataException {
        Task task = readTask(in);
        taskLogHandler.addTask(task);
        outSocket.writeObject(new ServerClientMessage(ClientRequests.NEW_TASK_ADDITION_REQUEST, task));
        serverAnswer = (ServerClientMessage) inSocket.readObject();
        if (serverAnswer.getDescription() == ServerRespond.USER_NOT_AUTHENTICATED)
            throw new WrongUserDataException((String) serverAnswer.getObject());
        LOG.info("User want to add new task, id= {}", task.getId());
    }

    private void deleteTask() throws IOException, ClassNotFoundException, WrongUserDataException {
        int id = readIdToDeleteTask(in);
        taskLogHandler.deleteTask(id);
        outSocket.writeObject(new ServerClientMessage(ClientRequests.TASK_DELETE_REQUEST, id));
        serverAnswer = (ServerClientMessage) inSocket.readObject();
        if (serverAnswer.getDescription() == ServerRespond.USER_NOT_AUTHENTICATED)
            throw new WrongUserDataException((String) serverAnswer.getObject());
        LOG.info("User want to delete task, number= {}", id);
    }
}

