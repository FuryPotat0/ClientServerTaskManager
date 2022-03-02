package Tasks;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TaskLogHandler implements Serializable {
    private TaskLog taskLog;
    private int maxId;

    public TaskLogHandler(){
        taskLog = new TaskLog();
        maxId = 1;
    }

    public void addTask(Task task){
        taskLog.addTask(task, maxId++);
    }

    public void deleteTask(int id){
        taskLog.deleteTask(id);
    }

    public int getNotPassedTasksNumber(){
        return getNotPassedTasks().size();
    }

    public List<Task> getNotPassedTasks(){
        ArrayList<Task> tasks = new ArrayList<>();
        for (Task task: taskLog.getAllTasks()){
            if (!task.isPass()){
                tasks.add(task);
            }
        }
        return tasks;
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder();
        s.append('\n');
        for (Task task: taskLog.getAllTasks())
            s.append(task.toString()).append('\n');
        return s.toString();
    }

    public void serializeTaskLog(OutputStream out){
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
            objectOutputStream.writeInt(maxId);
            objectOutputStream.writeObject(taskLog);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deserializeBuilding(InputStream in) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(in);
            maxId = objectInputStream.readInt();
            taskLog = (TaskLog) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}
