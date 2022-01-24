package main.clientServerConversation;

import java.io.Serializable;

public class ServerClientMessage implements Serializable {
    private Enum description;
    private Object object;

    public ServerClientMessage(Enum description, Object object){
        this.description = description;
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public Enum getDescription() {
        return description;
    }
}

