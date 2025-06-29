import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class ToDoList extends JFrame {
    DefaultListModel<String> listModel = new DefaultListModel<>();
    JList<String> taskList = new JList<>(listModel);
    JTextField taskField = new JTextField();
    JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"Low", "Medium", "High"});
    JSpinner deadlineSpinner = new JSpinner(new SpinnerDateModel());
    JTextField searchField = new JTextField();
    JButton addButton = new JButton("Add");
    JButton deleteButton = new JButton("Delete");
    JButton editButton = new JButton(" Edit");
    JButton completeButton = new JButton("Complete");
    JButton themeToggle = new JButton("Toggle Theme");
    boolean darkMode = false;

    java.util.List<Task> tasks = new ArrayList<>();
    java.util.List<Task> filteredTasks = new ArrayList<>();
    final String FILE_NAME = "todo_list.txt";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ToDoList::new);
    }

    public ToDoList() {
        super("To-Do List Manager");
        setSize(700, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        loadTasks();
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        inputPanel.add(new JLabel("Task:"));
        inputPanel.add(taskField);
        inputPanel.add(new JLabel("Priority:"));
        inputPanel.add(priorityCombo);
        deadlineSpinner.setEditor(new JSpinner.DateEditor(deadlineSpinner, "dd/MM/yyyy"));
        JPanel controls = new JPanel(new GridLayout(2, 2, 10, 10));
        controls.setBorder(new EmptyBorder(10, 10, 10, 10));
        controls.add(new JLabel(" Deadline:"));
        controls.add(deadlineSpinner);
        controls.add(new JLabel(" Search:"));
        controls.add(searchField);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(editButton);
        buttonPanel.add(completeButton);
        buttonPanel.add(themeToggle);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controls, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value);
            label.setOpaque(true);

            Task task = null;
            if (index >= 0 && index < filteredTasks.size()) {
                task = filteredTasks.get(index);
            }

            if (task != null && task.completed) {
                label.setForeground(Color.GREEN.darker());
            } else {
                label.setForeground(darkMode ? Color.WHITE : Color.BLACK);
            }

            if (isSelected) {
                label.setBackground(darkMode ? new Color(70, 70, 70) : new Color(200, 200, 255));
            } else {
                label.setBackground(darkMode ? new Color(46, 46, 46) : Color.WHITE);
            }

            label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            return label;
        });
        add(new JScrollPane(taskList), BorderLayout.CENTER);
        add(inputPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.SOUTH);

        updateTaskList();
        styleComponents();
        setDarkMode(false);

        addButton.addActionListener(e -> {
            String text = taskField.getText().trim();
            if (!text.isEmpty()) {
                tasks.add(new Task(text, priorityCombo.getSelectedItem().toString(), (Date) deadlineSpinner.getValue(), false));
                saveTasks();
                updateTaskList();
                taskField.setText("");
            }
        });

        deleteButton.addActionListener(e -> {
            int idx = taskList.getSelectedIndex();
            if (idx != -1) {
                tasks.remove(filteredTasks.get(idx));
                saveTasks();
                updateTaskList();
            }
        });

        editButton.addActionListener(e -> {
            int idx = taskList.getSelectedIndex();
            if (idx != -1) {
                Task task = filteredTasks.get(idx);
                JTextField tf = new JTextField(task.description);
                JComboBox<String> pc = new JComboBox<>(new String[]{"Low", "Medium", "High"});
                pc.setSelectedItem(task.priority);
                JSpinner ds = new JSpinner(new SpinnerDateModel(task.deadline, null, null, Calendar.DAY_OF_MONTH));
                ds.setEditor(new JSpinner.DateEditor(ds, "dd/MM/yyyy"));

                JPanel panel = new JPanel(new GridLayout(0, 1));
                panel.add(new JLabel("Task:")); panel.add(tf);
                panel.add(new JLabel("Priority:")); panel.add(pc);
                panel.add(new JLabel("Deadline:")); panel.add(ds);

                if (JOptionPane.showConfirmDialog(this, panel, "Edit Task", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    task.description = tf.getText().trim();
                    task.priority = pc.getSelectedItem().toString();
                    task.deadline = (Date) ds.getValue();
                    saveTasks();
                    updateTaskList();
                }
            }
        });

        completeButton.addActionListener(e -> {
            int idx = taskList.getSelectedIndex();
            if (idx != -1) {
                Task task = filteredTasks.get(idx);
                task.completed = true;
                saveTasks();
                updateTaskList();
            }
        });

        themeToggle.addActionListener(e -> {
            darkMode = !darkMode;
            setDarkMode(darkMode);
        });

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateTaskList(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateTaskList(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateTaskList(); }
        });

        setVisible(true);
    }

    void updateTaskList() {
        listModel.clear();
        filteredTasks.clear();
        String filter = searchField.getText().trim().toLowerCase();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        for (Task task : tasks) {
            if (task.description.toLowerCase().contains(filter)) {
                String status = task.completed ? "[done] " : "";
                listModel.addElement(status + task.description + " (" + task.priority + ") - " + df.format(task.deadline));
                filteredTasks.add(task);
            }
        }
    }

    void loadTasks() {
        tasks.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts.length == 4) {
                    tasks.add(new Task(parts[0], parts[1], new SimpleDateFormat("dd/MM/yyyy").parse(parts[2]), Boolean.parseBoolean(parts[3])));
                }
            }
        } catch (Exception ignored) {}
    }

    void saveTasks() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            for (Task t : tasks) {
                bw.write(t.description + "|" + t.priority + "|" + df.format(t.deadline) + "|" + t.completed);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void styleComponents() {
        Font font = new Font("Segoe UI", Font.PLAIN, 16);
        Component[] comps = {taskField, searchField, addButton, deleteButton, editButton, completeButton, themeToggle};
        for (Component c : comps) c.setFont(font);

        JButton[] buttons = {addButton, deleteButton, editButton, completeButton, themeToggle};
        Color color =new Color(145, 216, 252);

        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setBackground(color);
            buttons[i].setForeground(Color.BLACK);
        }
    }

    void setDarkMode(boolean dark) {
        Color bg = dark ? new Color(46, 46, 46) : Color.WHITE;
        Color fg = dark ? Color.WHITE : Color.BLACK;

        getContentPane().setBackground(bg);
        taskList.setBackground(bg);
        taskList.setForeground(fg);

        Component[] comps = {
            taskField, searchField, priorityCombo,
            deadlineSpinner.getEditor().getComponent(0)
        };
        for (Component c : comps) {
            c.setBackground(bg);
            c.setForeground(fg);
        }

        JButton[] buttons = {addButton, deleteButton, editButton, completeButton, themeToggle};
        for (JButton b : buttons) {
            b.setForeground(fg);
        }

        taskList.repaint();
    }

    static class Task {
        String description;
        String priority;
        Date deadline;
        boolean completed;

        Task(String d, String p, Date dl, boolean c) {
            description = d;
            priority = p;
            deadline = dl;
            completed = c;
        }
    }
}
