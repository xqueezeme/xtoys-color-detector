
import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class XToysDetectColors {
    //Tweak this number for the color accuracy: Lower = more accurate, High = less accurate
    private final static int MAX_DISTANCE = 5000;
    private final static int UPDATE_RATE_SECONDS = 1;

    public static void main(String[] args) {
        if(args.length > 1) {
            var webhookId = args[0];
            final List<Color> colors = new ArrayList<>();
            for(int i = 1;i<args.length; i++){
                colors.add(Color.decode(args[i]));
            }
            try {
                Robot robot = new Robot();
                final List<Integer>[] previousData = new List[]{null};
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable toRun = () -> {
                    try {
                        final List<Integer> data = countMatchingPixels(robot, colors);
                        if(!Objects.equals(data, previousData[0])) {
                            previousData[0] = data;
                            try {
                                webhook(webhookId, colors, data);
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                 };
                ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(toRun, 1, UPDATE_RATE_SECONDS, TimeUnit.SECONDS);
            } catch ( AWTException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Please provide the following arguments. You can add how many colors you want: <webhookid> <color1> <color2>\n" +
                    "Example: java XToysDetectColors.java D2COE76wV65S #000000 #eb4132");
        }
    }

    private static void webhook(String webhookId, List<Color> colors, List<Integer> data) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var colorText = colors.stream().map(color ->
                        "#"+Integer.toHexString(color.getRGB()).substring(2).toUpperCase())
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
                .thenAccept(code -> System.out.println(code+ " : " + url));
    }

    public static List<Integer> countMatchingPixels(Robot r, List<Color> colors) throws IOException {
        Rectangle capture = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage image = r.createScreenCapture(capture);
        var totalPixels = image.getWidth() * image.getHeight();
        final List<Integer> colorCounts = new ArrayList<>(IntStream.range(0, colors.size())
                .mapToObj((i) -> 0).toList());
        for (int x = 0; x < image.getWidth(); x++) {
            for(int y = 0; y < image.getHeight(); y++) {
                var rgb = image.getRGB(x, y);
                var color = new Color(rgb);

                for(int i = 0; i<colors.size(); i++) {
                    if (similarTo(colors.get(i), color)) {
                        colorCounts.set(i, colorCounts.get(i) + 1);
                    }
                }
            }
        }

        var colorPercentages = colorCounts.stream().map(count -> (int) ((double) count / totalPixels * 100)).collect(Collectors.toList());
        var percentages = colorPercentages.stream().map(c -> c + "%").collect(Collectors.joining(", "));
        System.out.println(LocalTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_TIME) + " Resolution: " + image.getWidth() + "x" + image.getHeight() + ", Percentages: " + percentages + " ; Press CTRL+C to stop.");

        return colorPercentages;
    }

    static boolean similarTo(Color a, Color b){
        double distance = (b.getRed() - a.getRed())*(b.getRed() - a.getRed()) + (b.getGreen() - a.getGreen())*(b.getGreen() - a.getGreen()) + (b.getBlue() - a.getBlue())*(b.getBlue() - a.getBlue());
        if(distance < MAX_DISTANCE){
            return true;
        } else {
            return false;
        }
    }
}
