package main;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Main extends JFrame {
    private JTextArea codeTextArea;
    private JEditorPane previewPane;
    private JLabel timerLabel;
    private JButton startButton;
    private JButton doneButton;
    private boolean cheatingDetected;
    private boolean codeEditable;

    private long startTime;
    private String userName;
    private String serverIp;
    private int serverPort;

    public Main() {
        setTitle("Web Development Simulator");
        setSize(800, 600);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        cheatingDetected = false;
        codeEditable = true;

        // Solicitar o nome de usuário ao abrir o aplicativo
        userName = JOptionPane.showInputDialog(this, "Digite seu nome de usuário:", "Nome de Usuário", JOptionPane.PLAIN_MESSAGE);

        // Solicitar o endereço de destino (IP e porta) ao usuário
        String serverAddress = JOptionPane.showInputDialog(this, "Digite o endereço IP do servidor e a porta (exemplo: 127.0.0.1:8080):", "Endereço de Destino", JOptionPane.PLAIN_MESSAGE);
        if (serverAddress != null && !serverAddress.isEmpty()) {
            // Extrair o IP e a porta do servidor do texto inserido pelo usuário
            String[] parts = serverAddress.split(":");
            if (parts.length == 2) {
                serverIp = parts[0];
                serverPort = Integer.parseInt(parts[1]);
            }
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (codeEditable) {
                    JOptionPane.showMessageDialog(Main.this,
                            "Você não pode fechar a janela até pressionar 'Pronto'",
                            "Aviso",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    dispose();
                }
            }

            @Override
            public void windowIconified(WindowEvent e) {
                if (codeEditable) {
                    cheatingDetected = true;
                    notifyServerOfCheat();
                    JOptionPane.showMessageDialog(Main.this,
                            "Você trapaceou",
                            "Aviso",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        timerLabel = new JLabel("Tempo: 0s");
        topPanel.add(timerLabel);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        codeTextArea = new JTextArea();
        previewPane = new JEditorPane();
        previewPane.setEditable(false);
        previewPane.setContentType("text/html");
        previewPane.setEditorKit(new HTMLEditorKit());
        centerPanel.add(new JScrollPane(codeTextArea));
        centerPanel.add(new JScrollPane(previewPane));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        startButton = new JButton("Iniciar");
        doneButton = new JButton("Pronto");
        doneButton.setEnabled(false);
        bottomPanel.add(startButton);
        bottomPanel.add(doneButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        this.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // Janela ganhou foco, nada a fazer aqui
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (codeEditable) {
                    if (isProcessRunning("chrome.exe") || isProcessRunning("firefox.exe") || isProcessRunning("msedge.exe")) {
                        cheatingDetected = true;
                        
                        JOptionPane.showMessageDialog(Main.this,
                                "Você trapaceou",
                                "Aviso",
                                JOptionPane.WARNING_MESSAGE);

                        killProcess("chrome.exe");
                        killProcess("firefox.exe");
                        killProcess("msedge.exe");
                    }
                }
            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cheatingDetected) {
                    return;
                }

                if (isProcessRunning("chrome.exe") || isProcessRunning("firefox.exe") || isProcessRunning("msedge.exe")) {
                    cheatingDetected = true;
                    notifyServerOfCheat();
                    killProcess("chrome.exe");
                    killProcess("firefox.exe");
                    killProcess("msedge.exe");

                    return;
                }

                startTime = System.currentTimeMillis();
                codeEditable = true;
                startButton.setEnabled(false);
                doneButton.setEnabled(true);
                updateTimer();
                updatePreview();
            }
        });

        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cheatingDetected) {
                    return;
                }
                long endTime = System.currentTimeMillis();
                long elapsedTime = (endTime - startTime) / 1000;
                codeEditable = false;
                doneButton.setEnabled(false);
                updatePreview();
                JOptionPane.showMessageDialog(Main.this,
                        "Código salvo!\nTempo de escrita: " + elapsedTime + "s",
                        "Informação",
                        JOptionPane.INFORMATION_MESSAGE);

                String codeContent = codeTextArea.getText();
                saveToFile("index.html", codeContent);
                sendIndexHtmlToServer(); // Envie o index.html para o servidor
            }
        });

        codeTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Plain text components do not fire these events
            }
        });

        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTimer();
            }
        });
        timer.start();
    }

    private void updateTimer() {
        if (codeEditable) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = (currentTime - startTime) / 1000;
            timerLabel.setText("Tempo: " + elapsedTime + "s");
        }
    }

    private void updatePreview() {
        String code = codeTextArea.getText();
        previewPane.setText(code);
    }

    private void saveToFile(String fileName, String content) {
        try {
            File file = new File(System.getProperty("user.home") + "/Desktop/" + fileName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isProcessRunning(String processName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("tasklist");
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(processName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void killProcess(String processName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/f", "/im", processName);
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyServerOfCheat() {
        try {
            Socket socket = new Socket(serverIp, serverPort);
            InetAddress localAddress = InetAddress.getLocalHost();
            String ipAddress = localAddress.getHostAddress();

            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            // Envie informações sobre a trapaça, incluindo o nome de usuário
            writer.println("Trapaça detectada por: " + userName + " (IP: " + ipAddress + ")");
            
            // Feche a conexão
            writer.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendIndexHtmlToServer() {
        try {
            Socket socket = new Socket(serverIp, serverPort);
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            // Ler o conteúdo do index.html
            String htmlContent = codeTextArea.getText();

            // Enviar o nome de usuário para o servidor
            writer.println("Nome de Usuário: " + userName);

            // Enviar o conteúdo do index.html para o servidor
            writer.println("Conteúdo do index.html:");
            writer.println(htmlContent);

            // Fechar a conexão
            writer.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Main simulator = new Main();
                simulator.setVisible(true);
            }
        });
    }
}
