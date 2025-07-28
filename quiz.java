// OnlineQuizSystem.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

public class OnlineQuizSystem {
    public static void main(String[] args) {
        new LoginFrame();
    }
}

class DBConnection {
    static Connection getConnection() throws Exception {
        String url = "jdbc:mysql://localhost:3306/quiz_db";
        String user = "root";
        String pass = ""; // update password if set
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, pass);
    }
}

class LoginFrame extends JFrame implements ActionListener {
    JTextField userField;
    JPasswordField passField;
    JButton loginBtn;

    LoginFrame() {
        setTitle("Login - Online Quiz");
        setSize(300, 200);
        setLayout(new GridLayout(3, 2));

        add(new JLabel("Username:"));
        userField = new JTextField();
        add(userField);

        add(new JLabel("Password:"));
        passField = new JPasswordField();
        add(passField);

        loginBtn = new JButton("Login");
        add(new JLabel());
        add(loginBtn);

        loginBtn.addActionListener(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        String username = userField.getText();
        String password = new String(passField.getPassword());

        try (Connection con = DBConnection.getConnection()) {
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                dispose();
                new QuizFrame(userId);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

class QuizFrame extends JFrame implements ActionListener {
    JLabel questionLabel, timerLabel;
    JRadioButton[] options;
    ButtonGroup bg;
    JButton nextBtn;

    ArrayList<Question> questions;
    int index = 0, score = 0, userId;
    Timer timer;
    int timeLeft = 900; // 15 minutes

    QuizFrame(int userId) {
        this.userId = userId;
        questions = getQuestions();
        setTitle("Online Quiz");
        setSize(500, 300);
        setLayout(new GridLayout(7, 1));

        questionLabel = new JLabel();
        add(questionLabel);

        options = new JRadioButton[4];
        bg = new ButtonGroup();

        for (int i = 0; i < 4; i++) {
            options[i] = new JRadioButton();
            bg.add(options[i]);
            add(options[i]);
        }

        timerLabel = new JLabel("Time Left: 15:00");
        add(timerLabel);

        nextBtn = new JButton("Next");
        nextBtn.addActionListener(this);
        add(nextBtn);

        loadQuestion();
        startTimer();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    void loadQuestion() {
        if (index < questions.size()) {
            bg.clearSelection();
            Question q = questions.get(index);
            questionLabel.setText((index + 1) + ". " + q.question);
            options[0].setText("A. " + q.optionA);
            options[1].setText("B. " + q.optionB);
            options[2].setText("C. " + q.optionC);
            options[3].setText("D. " + q.optionD);
        } else {
            timer.stop();
            showResult();
        }
    }

    void startTimer() {
        timer = new Timer(1000, e -> {
            timeLeft--;
            int minutes = timeLeft / 60;
            int seconds = timeLeft % 60;
            timerLabel.setText(String.format("Time Left: %02d:%02d", minutes, seconds));
            if (timeLeft <= 0) {
                timer.stop();
                showResult();
            }
        });
        timer.start();
    }

    public void actionPerformed(ActionEvent e) {
        Question q = questions.get(index);
        String selected = null;

        for (int i = 0; i < 4; i++) {
            if (options[i].isSelected()) {
                selected = String.valueOf((char) ('A' + i));
                break;
            }
        }

        if (selected != null && selected.equals(q.correctOption)) {
            score++;
        }

        index++;
        loadQuestion();
    }

    void showResult() {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO results (user_id, quiz_id, score, date_taken) VALUES (?, ?, ?, NOW())";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, userId);
            pst.setInt(2, 1); // assuming quiz ID = 1
            pst.setInt(3, score);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Quiz Completed!\nScore: " + score + "/" + questions.size());
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    ArrayList<Question> getQuestions() {
        ArrayList<Question> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT * FROM questions WHERE quiz_id = 1 ORDER BY RAND() LIMIT 10";
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Question q = new Question(
                        rs.getString("question_text"),
                        rs.getString("option_a"),
                        rs.getString("option_b"),
                        rs.getString("option_c"),
                        rs.getString("option_d"),
                        rs.getString("correct_option")
                );
                list.add(q);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }
}

class Question {
    String question, optionA, optionB, optionC, optionD, correctOption;

    public Question(String q, String a, String b, String c, String d, String correct) {
        this.question = q;
        this.optionA = a;
        this.optionB = b;
        this.optionC = c;
        this.optionD = d;
        this.correctOption = correct;
    }
}
