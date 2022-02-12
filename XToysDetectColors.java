import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class XToysDetectColors {
    //Tweak this number for the color accuracy: Lower = more accurate, High = less accurate
    private int MAX_DISTANCE = 5000;
    private final static int UPDATE_RATE_SECONDS = 1;

    public static void main(String[] args) {
        XToysDetectColors xToysDetectColors = new XToysDetectColors();
        xToysDetectColors.start();
    }

    JFrame frame;
    String webhookId = "";
    List<Color> colors = new ArrayList<>();
    JPanel pane = new JPanel(new GridBagLayout());
    List<Integer> data = new ArrayList<>();

    public void start() {
        frame = new JFrame("XToys Detect Colors");
        loadProperties();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setVisible(true);
        final JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(pane, BorderLayout.NORTH);
        final JScrollPane jScrollPane = new JScrollPane(jPanel);

        jScrollPane.setPreferredSize(new Dimension(500, 500));
        frame.getContentPane().add(jScrollPane, BorderLayout.CENTER);

        updateUI();

        final List<Integer>[] previousData = new List[]{null};
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable toRun = () -> {
            try {
                final List<Color> myColors = new ArrayList<>(colors);
                updateLabels();
                data = countMatchingPixels(myColors);
                if (!Objects.equals(data, previousData[0])) {
                    previousData[0] = data;
                    try {
                        webhook(data, myColors);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(toRun, 1, UPDATE_RATE_SECONDS, TimeUnit.SECONDS);
        frame.revalidate();
    }


    private List<JLabel> colorLabels = new ArrayList<>();

    public void updateLabels() {
        for (int i = 0; i < colorLabels.size(); i++) {
            String percentText = "";
            if (data.size() > i) {
                final Integer integer = data.get(i);
                percentText = " " + integer + "%";
            }
            String hexCode = "";
            if(colors.size()>i) {
                hexCode = " (#" + Integer.toHexString(colors.get(i).getRGB()).substring(2).toUpperCase() + ")";
            }
            String name = "Color " + (i + 1) + hexCode + ": " + percentText;
            colorLabels.get(i).setText(name);
        }
    }

    public void updateUI() {
        pane.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel headerLabel = new JLabel("Web hook Id");
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = 0;
        pane.add(headerLabel, c);
        c.gridx = 1;
        c.gridwidth = 1;
        c.gridy = 0;
        colorLabels.clear();
        final JTextField jTextField = new JTextField(webhookId);
        jTextField.setPreferredSize(new Dimension(200, 20));
        jTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                webhookId = jTextField.getText();

            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                webhookId = jTextField.getText();

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                webhookId = jTextField.getText();

            }
        });
        pane.add(jTextField, c);
        JLabel headerLabel2 = new JLabel("Color accuracy: ");
        headerLabel2.setToolTipText("Tweak this number for the color accuracy: Lower = more accurate, High = less accurate. Default value is 5000");
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = 1;
        pane.add(headerLabel2, c);
        c.gridx = 1;
        c.gridwidth = 1;
        c.gridy = 1;
        JSlider slider = new JSlider(0, 20000, MAX_DISTANCE);
        slider.setMajorTickSpacing(2000);
        slider.setMinorTickSpacing(1000);
        slider.setPaintTicks(true);

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                MAX_DISTANCE = slider.getValue();
            }
        });
        pane.add(slider, c);

        int y = 2;
        int colorIndex = 1;
        colorLabels.clear();

        if (colors != null) {
            for (Color color : colors) {
                c.gridx = 0;
                c.gridwidth = 1;
                c.gridy = y;
                final JLabel label = new JLabel("Color " + colorIndex + " (#" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase() + ")" +":");
                pane.add(label, c);
                colorLabels.add(label);
                c.gridx = 1;
                c.gridwidth = 1;
                c.gridy = y;

                final JPanel jPanel = new JPanel();

                final JButton changeButton = new JButton("Change");
                final int colorIndexFinal = colorIndex - 1;
                changeButton.setForeground(getContrastColor(color));
                changeButton.setBackground(color);
                changeButton.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Color newColor = JColorChooser.showDialog(changeButton, "Choose a color", color);
                        colors.set(colorIndexFinal, newColor);
                        updateUI();

                    }
                });
                jPanel.add(changeButton, c);

                final JButton deleteButton = new JButton("Delete");

                deleteButton.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        colors.remove(colorIndexFinal);
                        updateUI();

                    }
                });
                deleteButton.setForeground(getContrastColor(color));
                deleteButton.setBackground(color);

                jPanel.add(deleteButton, c);

                pane.add(jPanel, c);

                colorIndex++;
                y++;
            }
        }

        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = y;
        JButton button = new JButton("Add color");

        button.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Color color = JColorChooser.showDialog(button, "Choose a color", null);
                colors.add(color);
                updateUI();
            }
        });
        pane.add(button, c);
        c.gridx = 1;
        c.gridwidth = 1;
        c.gridy = y;
        JButton saveButton = new JButton("Save");

        saveButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveProperties();
            }
        });
        pane.add(saveButton, c);
        pane.revalidate();
        pane.repaint();

    }

    public static Color getContrastColor(Color color) {
        double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000;
        return y >= 128 ? Color.black : Color.white;
    }

    private void webhook(List<Integer> data, List<Color> colors) throws IOException, InterruptedException {
        if (webhookId != null && webhookId != "") {
            var client = HttpClient.newHttpClient();
            var colorText = colors.stream().map(color ->
                            "#" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase())
                    .collect(Collectors.joining(","));
            final StringBuilder url = new StringBuilder("https://xtoys.app/webhook?id=" + webhookId + "&action=colors");
            var percentages = data.stream().map(c -> c + "").collect(Collectors.joining(","));
            url.append("&allcolorpercentages=").append(percentages);
            url.append("&allcolors=").append(URLEncoder.encode(colorText, StandardCharsets.UTF_8.toString()));
            URI uri = URI.create(url.toString());
            var request = HttpRequest.newBuilder(uri)
                    .header("Accept", "application/json")
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(HttpResponse::statusCode)
                    .thenAccept(code -> System.out.println(code + " : " + url));
        } else {
            System.out.println("No webhook id defined");
        }
    }

    public List<Integer> countMatchingPixels(List<Color> colors) throws IOException, AWTException {
        if (!colors.isEmpty()) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();

            Rectangle allScreenBounds = new Rectangle();
            final List<Integer> colorCounts = new ArrayList<>(IntStream.range(0, colors.size())
                    .mapToObj((i) -> 0).collect(Collectors.toList()));

            for (GraphicsDevice screen : screens) {
                Rectangle rectangle  = screen.getDefaultConfiguration().getBounds();
                allScreenBounds.width += rectangle .width;
                allScreenBounds.height = Math.max(allScreenBounds.height, rectangle .height);
                BufferedImage image = new Robot().createScreenCapture(rectangle);
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        var rgb = image.getRGB(x, y);
                        var color = new Color(rgb);

                        for (int i = 0; i < colors.size(); i++) {
                            if (similarTo(colors.get(i), color)) {
                                colorCounts.set(i, colorCounts.get(i) + 1);
                            }
                        }
                    }
                }
            }

            var totalPixels = allScreenBounds.getWidth() * allScreenBounds.getHeight();

            var colorPercentages = colorCounts.stream().map(count -> (int) ((double) count / totalPixels * 100)).collect(Collectors.toList());
            var percentages = colorPercentages.stream().map(c -> c + "%").collect(Collectors.joining(", "));
            System.out.println(LocalTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_TIME) + " Resolution: " + (int) allScreenBounds.getWidth() + "x" + (int) allScreenBounds.getHeight() + ", Percentages: " + percentages + " ; Press CTRL+C to stop.");

            return colorPercentages;
        }
        return Collections.emptyList();
    }

    boolean similarTo(Color a, Color b) {
        double distance = (b.getRed() - a.getRed()) * (b.getRed() - a.getRed()) + (b.getGreen() - a.getGreen()) * (b.getGreen() - a.getGreen()) + (b.getBlue() - a.getBlue()) * (b.getBlue() - a.getBlue());
        if (distance < MAX_DISTANCE) {
            return true;
        } else {
            return false;
        }
    }

    void loadProperties() {
        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);
            webhookId = prop.getProperty("webhookId");
            try {
                MAX_DISTANCE = Integer.parseInt(prop.getProperty("max-distance"));
            } catch (Exception e) {
                MAX_DISTANCE = 5000;
            }
            final Enumeration<Object> elements = prop.keys();
            final List<Integer> colorIndexes = new ArrayList<>();
            while (elements.hasMoreElements()) {
                final Object key = elements.nextElement();
                if (key.toString().startsWith("color-")) {
                    colorIndexes.add(Integer.parseInt(key.toString().replace("color-", "")));
                }
            }
            colorIndexes.stream().sorted().forEach(i -> colors.add(Color.decode(prop.getProperty("color-" + i))));
        } catch (IOException ex) {
        }


    }

    public void saveProperties() {
        try {
            //create a properties file
            Properties props = new Properties();
            props.setProperty("webhookId", webhookId == null ? "" : webhookId);
            props.setProperty("max-distance", String.valueOf(MAX_DISTANCE));

            for (int i = 0; i < colors.size(); i++) {
                props.setProperty("color-" + (i + 1), "#" + Integer.toHexString(colors.get(i).getRGB()).substring(2));
            }
            File f = new File("config.properties");
            OutputStream out = new FileOutputStream(f);
            //If you wish to make some comments
            props.store(out, "User properties");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
