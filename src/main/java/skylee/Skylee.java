package skylee;

import skylee.exception.SkyleeException;
import skylee.io.Config;
import skylee.task.Deadline;
import skylee.task.Event;
import skylee.task.Task;
import skylee.task.Todo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import static skylee.io.Command.COMMAND_BYE;
import static skylee.io.Command.COMMAND_LIST;
import static skylee.io.Command.COMMAND_MARK;
import static skylee.io.Command.COMMAND_UNMARK;
import static skylee.io.Command.COMMAND_TODO;
import static skylee.io.Command.COMMAND_DEADLINE;
import static skylee.io.Command.COMMAND_EVENT;
import static skylee.io.Command.COMMAND_DELETE;

import static skylee.io.Message.LINE;
import static skylee.io.Message.PREFIX_MESSAGE;
import static skylee.io.Message.PREFIX_TASK;
import static skylee.io.Message.PREFIX_EXCEPTION;
import static skylee.io.Message.MESSAGE_HELLO;
import static skylee.io.Message.MESSAGE_BYE;
import static skylee.io.Message.MESSAGE_UNKNOWN_COMMAND;
import static skylee.io.Message.MESSAGE_ID_FORMAT;
import static skylee.io.Message.MESSAGE_ID_OUT_OF_RANGE;
import static skylee.io.Message.MESSAGE_LIST;
import static skylee.io.Message.MESSAGE_UNMARK;
import static skylee.io.Message.MESSAGE_MARK;
import static skylee.io.Message.MESSAGE_DELETE;
import static skylee.io.Message.MESSAGE_ADD;
import static skylee.io.Message.MESSAGE_COUNT;
import static skylee.io.Message.MESSAGE_IO_EXCEPTION;

public class Skylee {
    private static ArrayList<Task> tasks = new ArrayList<>();

    private static void showMessages(String... messages) {
        System.out.print(LINE);
        for (String message : messages) {
            System.out.println(PREFIX_MESSAGE + message);
        }
        System.out.println(LINE);
    }

    private static void loadFile() {
        try {
            File file = new File(Config.PATH_SAVE);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                tasks.add(Task.parseTask(scanner.nextLine()));
            }
        } catch (Exception e) {
            tasks = new ArrayList<>();
        }
    }

    private static void saveFile() throws SkyleeException {
        try {
            File file = new File(Config.PATH_SAVE);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            for (Task task: tasks) {
                fileWriter.write(task.show() + "\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            throw new SkyleeException(MESSAGE_IO_EXCEPTION);
        }
    }

    private static void bye() throws SkyleeException {
        saveFile();
        showMessages(MESSAGE_BYE);
        System.exit(0);
    }

    private static String[] addTask(Task task) {
        tasks.add(task);
        return new String[]{MESSAGE_ADD,
                PREFIX_TASK + task,
                String.format(MESSAGE_COUNT, tasks.size(), tasks.size() > 1 ? "s" : "")};
    }

    private static int parseTaskId(String commandArgs) throws SkyleeException {
        int taskId;
        try {
            taskId = Integer.parseInt(commandArgs) - 1;
        } catch (NumberFormatException e) {
            throw new SkyleeException(MESSAGE_ID_FORMAT);
        }
        if (taskId < 0 || taskId >= tasks.size()) {
            throw new SkyleeException(MESSAGE_ID_OUT_OF_RANGE);
        }
        return taskId;
    }

    private static String[] markTask(String commandArgs) throws SkyleeException {
        final int taskId = parseTaskId(commandArgs);
        tasks.get(taskId).markAsDone();
        return new String[]{MESSAGE_MARK,
                PREFIX_TASK + tasks.get(taskId)};
    }

    private static String[] unmarkTask(String commandArgs) throws SkyleeException {
        final int taskId = parseTaskId(commandArgs);
        tasks.get(taskId).unmarkAsNotDone();
        return new String[]{MESSAGE_UNMARK,
                PREFIX_TASK + tasks.get(taskId)};
    }

    private static String[] listTasks() {
        String[] messages = new String[tasks.size() + 1];
        messages[0] = MESSAGE_LIST;
        for (int i = 0; i < tasks.size(); i++) {
            messages[i + 1] = (i + 1) + "." + tasks.get(i);
        }
        return messages;
    }

    private static String[] deleteTask(String commandArgs) throws SkyleeException {
        final int taskId = parseTaskId(commandArgs);
        final Task task = tasks.get(taskId);
        tasks.remove(taskId);
        return new String[]{MESSAGE_DELETE,
                PREFIX_TASK + task,
                String.format(MESSAGE_COUNT, tasks.size(), tasks.size() > 1 ? "s" : "")};
    }

    private static String[] showException(String message) {
        return new String[]{PREFIX_EXCEPTION + message};
    }

    private static String[] splitCommandWordAndArgs(String rawUserInput) {
        final String[] split = rawUserInput.trim().split("\\s+", 2);
        return split.length == 2 ? split : new String[] { split[0] , "" };
    }
    
    private static String[] executeCommand(String userInputString) {
        final String[] commandTypeAndParams = splitCommandWordAndArgs(userInputString);
        final String commandType = commandTypeAndParams[0];
        final String commandArgs = commandTypeAndParams[1];
        try {
            switch (commandType) {
            case COMMAND_BYE:
                bye();
                // Fallthrough
            case COMMAND_LIST:
                return listTasks();
            case COMMAND_MARK:
                return markTask(commandArgs);
            case COMMAND_UNMARK:
                return unmarkTask(commandArgs);
            case COMMAND_TODO:
                return addTask(Todo.parseTodo(commandArgs));
            case COMMAND_DEADLINE:
                return addTask(Deadline.parseDeadline(commandArgs));
            case COMMAND_EVENT:
                return addTask(Event.parseEvent(commandArgs));
            case COMMAND_DELETE:
                return deleteTask(commandArgs);
            default:
                throw new SkyleeException(MESSAGE_UNKNOWN_COMMAND);
            }
        } catch (SkyleeException e) {
            return showException(e.getMessage());
        }
    }

    public static void main(String[] args) {
        loadFile();
        showMessages(MESSAGE_HELLO);
        Scanner scanner = new Scanner(System.in);
        for (;;) {
            final String command = scanner.nextLine();
            final String[] feedback = executeCommand(command);
            showMessages(feedback);
        }
    }
}
