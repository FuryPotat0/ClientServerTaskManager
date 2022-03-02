package Tasks;

import java.io.Serializable;
import java.util.ArrayList;

public class TaskLog implements Serializable {
    private ArrayList<Task> taskArrayList;

    public TaskLog(){
        taskArrayList = new ArrayList<Task>();
    }

    public Task getTask(int id){
        for (Task task: taskArrayList){
            if (task.getId() == id){
                return task;
            }
        }
        return null;
    }

    public void addTask(Task task, int id){
        task.setId(id);
        taskArrayList.add(task);
    }

    public void deleteTask(int id){
        for (int i = 0; i < taskArrayList.size(); i++){
            if (taskArrayList.get(i).getId() == id){
                taskArrayList.remove(i);
                break;
            }
        }
    }

    public ArrayList<Task> getAllTasks() {
        return taskArrayList;
    }
}