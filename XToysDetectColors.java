
import java.awt.*;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

public class XToysDetectColors {
    //Tweak this number for the color accuracy: Lower = more accurate, High = less accurate
    private final static int MAX_DISTANCE = 5000;
    private final static int UPDATE_RATE = 1000;

    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);

    public static void main(String[] args) {
        if(args.length > 2) {
            var webhookId = args[0];
            final List<Color> colors = new ArrayList<>();
            for(int i = 1;i<args.length; i++){
                colors.add(Color.decode(args[i]));
            }
            try {
                Robot r = new Robot();

                while (true) {
                    final List<Integer> data = countMatchingPixels(r, colors);
                    executorService.submit(() -> {
                        try {
                            webhook(webhookId, data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            executorService.shutdownNow();
                        }
                    });
                    sleep(UPDATE_RATE);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (AWTException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Please provide the following arguments. You can add how many colors you want, but you'll need to configure the script to handle them: <webhookid> <color1> <color2>\n" +
                    "Example: java XToysDetectColors.java D2COE76wV65S #000000 #eb4132");
        }
    }

    private static void webhook(String webhookId, List<Integer> data) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        final StringBuilder url = new StringBuilder("https://xtoys.app/webhook?id=" + webhookId + "&action=colors");
        for(int i = 0; i<data.size(); i++) {
            url.append("&color").append(i + 1).append("=").append(data.get(i));
        }
        URI uri = URI.create(url.toString());
        var request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .build();
        int statusCode = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(HttpResponse::statusCode)
                .join();
        System.out.println(statusCode+ " : " + url);
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
        System.out.println(LocalTime.now().toString() + " Resolution: " + image.getWidth() + "x" + image.getHeight() + ", Percentages: " + percentages + "\nPress CTRL+C to stop.");

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
