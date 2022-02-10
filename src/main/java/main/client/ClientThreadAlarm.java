package main.client;

import Tasks.Task;
import Tasks.TaskLogHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

public class ClientThreadAlarm extends WorkingThread {
    private static final Logger LOG = LogManager.getLogger("ClientThreadAlarmLogger");
    private final TaskLogHandler taskLogHandler;

    public ClientThreadAlarm(TaskLogHandler taskLogHandler) {
        super();
        this.taskLogHandler = taskLogHandler;
        LOG.trace("ClientThreadAlarm is running");
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("ClientThreadAlarm is interrupted");
            }

            if (taskLogHandler.getNotPassedTasksNumber() > 0)
                for (Task task : taskLogHandler.getNotPassedTasks()) {
                    if (!task.isPass() && task.getDeadline() != null && new Date().compareTo(task.getDeadline()) >= 0) {
                        task.setPass(true);
                        System.out.println(task.getInfo() + " time to do it");
                        LOG.info("{} the alarm was triggered", task.getInfo());
                    }
                }
        }
        LOG.trace("ClientThreadAlarm is stopped");
    }
}

