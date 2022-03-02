package Tasks;

import java.io.Serializable;
import java.util.Date;

public class Task implements Serializable {
    private int id;
    private String name;
    private String description;
    private Date deadline;
    private String contacts;
    private boolean isPass;

    public Task(int id, String name, String description, Date deadline, String contacts){
        this.id = id;
        this.name = name;
        this.description = description;
        this.deadline = deadline;
        this.contacts = contacts;
        this.isPass = false;
    }

    public Task(String name, String description, Date deadline, String contacts){
        id = 0;
        this.name = name;
        this.description = description;
        this.deadline = deadline;
        this.contacts = contacts;
        this.isPass = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public void setPass(boolean isPass){
        this.isPass = isPass;
    }

    public boolean isPass() {
        return isPass;
    }

    public Date getDeadline() {
        return deadline;
    }

    public String getInfo(){
        return id + " " + name + " " + description + " " + contacts;
    }

    @Override
    public String toString(){
        return id + ", " + name + ", " + description + ", " + deadline + ", " + contacts + ", " +
                (isPass ? "Выполнено": "Не выполнено");
    }
}

