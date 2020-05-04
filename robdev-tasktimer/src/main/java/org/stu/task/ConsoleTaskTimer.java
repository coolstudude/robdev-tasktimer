/*
 * Created on 28 September 2006, 09:58
 */

package org.stu.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.stu.task.util.TextUtilities;

/**
 * Task Timer application which takes console input in order to add, remove and update timed tasks. Tasks are numbered
 * automatically by the app in order to id them for command input.
 *
 * Notes: 1. use a command processor/handler ('task' or 'service') for each command, to process accordingly 2.
 * categories/sub categories?
 *
 * @author Stuart
 */
public class ConsoleTaskTimer {
	// other constants
	private static String FORMAT_DATE = "yyyyMMdd";
	private static String FORMAT_TIME = "yyyy-MM-dd HH:mm:ss";
	private static String PATH_DAILY = "file/daily";
	private static String PATH_ROOT = "file/root.tt";
	private static String PATH_PROPERTIES = "file/taskTimer.properties";
	private static String PATH_HOME = ".";
	private static long AUTO_SAVE_DELAY = 300000; // 5000

	private static final String FILE_TYPE_AUTO = "auto";
	private static final String FILE_TYPE_SAVE = "save";

	// file-related variables
	private DateFormat dateFormat = new SimpleDateFormat(ConsoleTaskTimer.FORMAT_DATE);
	private DateFormat timeFormat = new SimpleDateFormat(ConsoleTaskTimer.FORMAT_TIME);
	private Calendar currentDayAuto = Calendar.getInstance();
	private Calendar currentDaySave = Calendar.getInstance();
	/** the current day's autosave file */
	private File fileAuto;
	/** the current day's save file */
	private File fileSave;
	/** the master task list file */
	private File fileRoot;

	// task-related variables
	private Map taskMap = new HashMap();
	private Task currentTask;
	private int taskCounter;

	// other variables
	private BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
	private boolean shouldRun = true;

	/**
	 * main method
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ConsoleTaskTimer app = null;
		if (args.length > 0) {
			app = new ConsoleTaskTimer(args[0]);
		} else {
			app = new ConsoleTaskTimer();
		}
		app.initialise();
		app.process();
	}

	/**
	 * default Constructor
	 */
	private ConsoleTaskTimer() {
		super();
	}

	/**
	 * @param homePath
	 */
	private ConsoleTaskTimer(String homePath) {
		PATH_HOME = homePath;
		if (!new File(PATH_HOME).isDirectory()) {
			throw new RuntimeException("Invalid home path");
		}
	}

	/**
	 *
	 */
	private void initialise() throws Exception {
		Properties props = new Properties();
		// 1. Load up taskTimer.properties
		FileInputStream propFileStr = new FileInputStream(new File(PATH_HOME + File.separatorChar + PATH_PROPERTIES).getAbsoluteFile());
		props.load(propFileStr);
		// TODO load props
		// props.getProperty("");

		// 2. load root.tt --> all tasks
		fileRoot = new File(PATH_HOME + File.separatorChar + PATH_ROOT).getAbsoluteFile();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileRoot)));
		int index = 1; // TODO implement later - use as reference for max ID in file
		String line = reader.readLine();
		while (line != null) {
			if (!line.startsWith("#")) { // filter out comments
				// 1. check for numerical first character - TODO implement later
				// 2. add task
				String[] tokens = line.split("\t");
				int id = Integer.parseInt(tokens[0]);
				Task task = new Task(tokens[1], id);
				if (tokens.length > 4) {
					task.setTimeInHHMMss(tokens[4].substring(0, 8));
				}
				taskMap.put(new Integer(id), task);
				if (index < id) {
					index = id;
				}
			}
			line = reader.readLine();
		}
		reader.close();

		taskCounter = index;
	}

	/**
	 * @return
	 */
	private String getVersion() {
		return "\nTask Timer Console Edition v 0.8\n\tauthor\t:\tStuart M. Robinson\n\tdate\t:\t2007-03-15";
	}

	private void displayUsage() {
		// TODO roll thro each command handler which describes its own usage
		System.out.println();
		System.out.println("\tUsage:");
		System.out.println("\t new <name/description of task> - create new task with description; returns the unique task number");
		System.out.println("\t start <task number> - start a task");// etc...
		System.out.println("\t stop <task number> - stop a task");// etc...
		System.out.println("\t edit <task number | a | s | n> - (edit | add | subtract | new name) the time for a task (HH:MM:ss)");// etc...
		System.out.println("\t time <task number> - get a task's elapsed time");// etc...
		System.out.println("\t reset <task number | all> - reset the time for a task or all tasks");// etc...
		System.out.println("\t del <task number> - delete a task");
		System.out.println("\t list - list all tasks - (id, elapsed time, description) - a * indicates the current task");// etc...
		System.out.println("\t save - save all tasks and their current state");
		// System.out.println("\t setdelay - set auto save delay in millisecs (defaults to 300000ms (5min))");
		System.out.println("\t exit - stop the app");// etc...
	}

	/**
	 *
	 */
	private void process() {
		System.out.println(getVersion()); // put into prop file
		displayUsage();

		Runnable autoSaveRunnable = new Runnable() {
			@Override
			public void run() {
				// Calendar currentDay = Calendar.getInstance();

				try {
					// first make sure the folder exists on startup (called once per startup)
					File dir = new File(PATH_HOME + File.separatorChar + PATH_DAILY + File.separatorChar).getAbsoluteFile();
					if (!dir.isDirectory()) {
						if (!dir.mkdir()) {
							throw new RuntimeException("Unable to create required folder in user working directory '.\file\'");
						}
					}

					// now set up autosave for today
					while (shouldRun) {
						writeToFile(FILE_TYPE_AUTO);
						try {
							Thread.sleep(ConsoleTaskTimer.AUTO_SAVE_DELAY);
						} catch (InterruptedException ie) {
							/**/ }
					}

				} catch (IOException ioe) {
					System.out.println("Error writing to file: " + ioe.getMessage());
				}
			}
		};
		Thread autosaver = new Thread(autoSaveRunnable, "Autosave Thread");
		autosaver.setDaemon(true);
		autosaver.start();

		while (shouldRun) {
			System.out.print(">");
			processCommand(prompt());
		}
	}

	/**
	 * writes to the current day's file, overwriting the previous contents
	 */
	private synchronized void writeToFile(String type) throws IOException {
		File file = getFile(type);
		FileWriter writer = null;
		if (file != null) {
			writer = new FileWriter(file);
			writer.write(getList().toString());
			writer.flush();
			writer.close();
		}

		writeToRoot();
	}

	/**
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeToRoot() throws FileNotFoundException, IOException {
		// 1. first read any comments into memory
		StringBuffer comments = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileRoot)));
		String line = reader.readLine();
		while (line != null) {
			if (line.startsWith("#")) {
				comments.append(line + "\n");
			}
			line = reader.readLine();
		}
		reader.close();

		// 2. now write the comments back
		FileWriter writer = new FileWriter(fileRoot);
		writer.write(comments.toString());
		writer.flush();

		// 3. now write the tasks
		StringBuffer tasks = new StringBuffer();
		Collection taskList = new TreeSet(getListDescComparator());
		taskList.addAll(taskMap.values());
		for (Iterator it = taskList.iterator(); it.hasNext();) {
			Task task = (Task) it.next();
			tasks.append(task.getId());
			tasks.append("\t");
			tasks.append(task.getName());
			tasks.append("\t");
			tasks.append("false");
			tasks.append("\t");
			tasks.append("default");
			tasks.append("\t");
			tasks.append(task.getTimeInHHMMss());
			tasks.append("\n");
		}
		writer.write(tasks.toString());
		writer.flush();

		writer.close();
	}

	/**
	 * @return the file object for the current day for the specified type, rolling over if necessary
	 */
	private File getFile(String type) {
		String filename = PATH_HOME + File.separatorChar + PATH_DAILY + File.separatorChar;
		File file = null;
		Calendar currentDay = null;
		if (FILE_TYPE_AUTO.equals(type)) {
			file = fileAuto;
			currentDay = currentDayAuto;
		} else {
			file = fileSave;
			currentDay = currentDaySave;
		}
		try {
			Calendar today = Calendar.getInstance();
			if (file == null ||
					today.get(Calendar.DATE) != currentDay.get(Calendar.DATE)) {
				String dateString = dateFormat.format(today.getTime());
				filename += dateString + ".tt" + type.substring(0, 1);
				file = new File(filename).getAbsoluteFile();
				filename = file.getAbsolutePath();
				if (!file.createNewFile()) {
					// file already exists, so create a backup for it:
					int index = 1;
					while (!file.renameTo(new File(filename + index))) {
						System.out.println("WARNING: Unable to create " + type + " file '" + filename + " ' - it already exists. \n\tCreating backup '"
								+ filename + index + "'");
						index++;
					}
					System.out.print(">");
				}

				if (FILE_TYPE_AUTO.equals(type)) {
					fileAuto = file;
					currentDayAuto = today;
				} else {
					fileSave = file;
					currentDaySave = today;
				}
			}
			return file;
		} catch (Exception e) {
			throw new RuntimeException("Problem creating a new file", e);
		}
	}

	/**
	 * @param input
	 */
	private void processCommand(String input) {
		String command = "";
		try {
			int index = input.indexOf(" ");
			String params = "";
			if (index != -1) {
				command = input.substring(0, index);
				params = input.substring(index + 1);
			} else {
				command = input;
			}

			String cmd = command.trim().toUpperCase();
			Class clazz = Class.forName(this.getClass().getName());
			Method method = clazz.getDeclaredMethod(
					"processCommand" + cmd,
					new Class[] { String.class });
			method.invoke(this, new Object[] { params.trim() });

		} catch (NoSuchMethodException nsme) {
			respond("Command not recognized - '" + command + "'");
			displayUsage();
		} catch (Exception e) {
			respond("Exception encountered: " +
					(e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
			displayUsage();
		}

	}

	/**
	 * @param response
	 */
	private void respond(String response) {
		System.out.println("\t" + response);
	}

	/**
	 * @param params
	 */
	private void processCommandEXIT(final String params) {
		// >exit
		respond("Exiting application...");
		shouldRun = false;
	}

	/**
	 * @param params
	 */
	private void processCommandLIST(final String params) {
		// >list
		System.out.print(getList().toString());
	}

	/**
	 * @return
	 */
	private synchronized StringBuffer getList() {
		StringBuffer buffer = new StringBuffer("\nTask List:\n\tElapsed Time\tID\tDescription\n");
		buffer.append("---------------------------------------------------------------\n");
		long totalTime = 0;

		Collection taskList = new TreeSet(getListDescComparator());
		taskList.addAll(taskMap.values());
		for (Iterator it = taskList.iterator(); it.hasNext();) {
			Task task = (Task) it.next();
			long time = task.getTimeInMillis();
			totalTime += time;
			buffer.append(
					(task.equals(currentTask) && (currentTask != null && currentTask.isStarted()) ? " *\t" : "\t") +
							TextUtilities.formatTimeInHHMMss(time) +
							"\t" +
							task.getId() +
							"\t" +
							task.getName() +
							"\n");
		}
		buffer.append("---------------------------------------------------------------\n");
		buffer.append("TOTAL\t" +
				TextUtilities.formatTimeInHHMMss(totalTime) +
				"\n");
		buffer.append("---------------------------------------------------------------\n");
		buffer.append("Current Time : " + timeFormat.format(new Date()) + "\n\n");
		return buffer;
	}

	/**
	 * @return
	 */
	private Comparator getListDescComparator() {
		Comparator c = new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				Task task1 = (Task) o1;
				Task task2 = (Task) o2;
				return task1.getName().compareTo(task2.getName());
			}
		};
		return c;
	}

	/**
	 * @param params
	 * @throws NumberFormatException
	 */
	private void processCommandEDIT(final String params) throws NumberFormatException {
		// >edit 1
		// edit a 1 - add time to 1
		// edit s 2 - subtract time from 2
		// edit a 1 2 - add time to 1 and subtract from 2
		// edit s 2 1 - subtract time from 1 and add to 2
		// edit n 1 - edit name of task

		String[] split = params.split(" ");
		int count = split.length;
		if (count == 0) {
			throw new RuntimeException("Invalid parameters.");
		} else if (count == 1) {
			// first chk for default condition:
			Task task = getTask(split[0]);
			if (task != null) {
				// default condition - edit a specific task directly
				respond("'" + task.getName() + "' - edit time : ");
				String newTime = prompt("\t" + task.getTimeInHHMMss() + " --> ");
				if (newTime.length() != 8) {
					respond("Invalid time. Please try again.");
				} else {
					task.setTimeInHHMMss(newTime.trim());
				}
			}
		} else {
			// now chk whether param is default or an add/subtract:
			String add = "a";
			String sub = "s";
			String name = "n";
			Task task = getTask(split[1]);
			if (count == 2) {
				// from only
				if (split[0].equals(add)) {
					String newTime = prompt("\tAdd to '" + task.getName() + "' (current elapsed time : " + task.getTimeInHHMMss() + ") : ");
					task.setTimeInMillis(addTime(task, newTime.trim()));
				} else if (split[0].equals(sub)) {
					String newTime = prompt("\tSubtract from '" + task.getName() + "' (current elapsed time : " + task.getTimeInHHMMss() + ") : ");
					task.setTimeInMillis(subtractTime(task, newTime.trim()));
					// new name
				} else if (split[0].equals(name)) {
					String newName = prompt("\tRename '" + task.getName() + "' to : ");
					task.setName(newName);
				} else {
					throw new RuntimeException("Invalid parameters.");
				}
			} else if (count == 3) {
				// from and to
				Task toTask = getTask(split[2]);
				if (split[0].equals(add)) {
					respond("Add to '" + task.getName() + "' (current elapsed time : " + task.getTimeInHHMMss() + ") and... ");
					String newTime = prompt("\tSubtract from '" + toTask.getName() + "' (current elapsed time : " + toTask.getTimeInHHMMss() + ") : ");
					toTask.setTimeInMillis(subtractTime(toTask, newTime.trim()));
					task.setTimeInMillis(addTime(task, newTime.trim()));
				} else if (split[0].equals(sub)) {
					respond("Subtract from '" + task.getName() + "' (current elapsed time : " + task.getTimeInHHMMss() + ") and... ");
					String newTime = prompt("\tAdd to '" + toTask.getName() + "' (current elapsed time : " + toTask.getTimeInHHMMss() + ") : ");
					task.setTimeInMillis(subtractTime(task, newTime.trim()));
					toTask.setTimeInMillis(addTime(toTask, newTime.trim()));
				} else {
					throw new RuntimeException("Invalid parameters.");
				}
			} else {
				throw new RuntimeException("Invalid parameters.");
			}
		}
	}

	/**
	 * @param task
	 * @param timeHHMMss
	 * @return
	 */
	private long addTime(Task task, String timeHHMMss) {
		long t = TextUtilities.formatTimeInMillis(timeHHMMss);
		t += task.getTimeInMillis();
		return t;
	}

	/**
	 * @param task
	 * @param timeHHMMss
	 * @return
	 */
	private long subtractTime(Task task, String timeHHMMss) {
		long t = TextUtilities.formatTimeInMillis(timeHHMMss);
		t -= task.getTimeInMillis(); // should be -ve
		if (t > 0) {
			throw new RuntimeException("The time entered is greater than the current elapsed time.");
		}
		return Math.abs(t);
	}

	/**
	 * @param params
	 * @throws NumberFormatException
	 * @throws RuntimeException
	 */
	private void processCommandRESET(final String params) throws NumberFormatException, RuntimeException {
		// >reset 1
		// >reset all

		if (params != null && params.trim().length() > 0) {
			if (params.trim().equalsIgnoreCase("all")) {
				// run through all and reset
				for (Iterator it = taskMap.keySet().iterator(); it.hasNext();) {
					Integer id = (Integer) it.next();
					Task task = (Task) taskMap.get(id);
					task.reset();
				}
				respond(taskMap.size() + " tasks reset");
			} else {
				// individual
				Task task = getTask(params);
				if (task != null) {
					task.reset();
					respond("'" + task.getName() + "' - reset");
				}
			}
		} else {
			throw new RuntimeException("Invalid parameters supplied - either 'all' or specific task");
		}
	}

	/**
	 * @param params
	 */
	private void processCommandSAVE(final String params) {
		try {
			writeToFile(FILE_TYPE_SAVE);
			respond("file saved successfully");
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	/**
	 * @param params
	 * @throws NumberFormatException
	 */
	private void processCommandDEL(final String params) throws NumberFormatException {
		// >del 1
		getTask(params);
		Task task = (Task) taskMap.remove(new Integer(params.trim()));
		if (task != null) {
			respond("'" + task.getName() + "' - removed - final elapsed time: " + task.getTimeInHHMMss());
		}
	}

	/**
	 * @param params
	 * @throws NumberFormatException
	 */
	private void processCommandTIME(final String params) throws NumberFormatException {
		// >time 1
		Task task = getTask(params);
		if (task != null) {
			respond("'" + task.getName() + "' - elapsed time: " + task.getTimeInHHMMss());
		}
	}

	/**
	 * @param params
	 * @throws NumberFormatException
	 */
	private void processCommandSTOP(final String params) throws NumberFormatException {
		// >stop 1
		Task task = getTask(params);
		if (task != null) {
			task.stop();
			respond("'" + task.getName() + "' - stopped at : " + task.getTimeInHHMMss());
		}
	}

	/**
	 * @param params
	 * @throws NumberFormatException
	 */
	private void processCommandSTART(final String params) throws NumberFormatException {
		// >start 1
		Task task = getTask(params);
		if (task != null) {
			if (currentTask != null && currentTask.isStarted()) {
				currentTask.stop();
				respond("'" + currentTask.getName() + "' - stopped for new task at : " + currentTask.getTimeInHHMMss());
			}
			currentTask = task;
			task.start();
			respond("'" + task.getName() + "' - started at : " + task.getTimeInHHMMss());
		}
	}

	/**
	 * @param params
	 */
	private void processCommandNEW(final String params) {
		// >new Test task for Stu
		Task task = new Task(params, ++taskCounter);
		taskMap.put(new Integer(task.getId()), task);
		respond("Task created - id = " + task.getId());
	}

	/**
	 * Prompts the user for input using the promptText, and returns the response
	 *
	 * @param promptText the text to be prompted to the user
	 * @return the user response
	 */
	/**
	 * @param promptText
	 * @return
	 */
	private String prompt(String promptText) {
		System.out.print(promptText);
		try {
			return consoleReader.readLine();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe.getMessage());
		}
	}

	/**
	 * Prompts the user for input with no promptText, and returns the response
	 *
	 * @return the user response
	 */
	/**
	 * @return
	 */
	private String prompt() {
		return prompt("");
	}

	/**
	 * @param param
	 * @return
	 */
	private Task getTask(String param) {
		Task task = (Task) taskMap.get(new Integer(param.trim()));
		if (task == null) {
			throw new RuntimeException("No task found with ID '" + param.trim() + "'");
		}
		return task;
	}

}
