package com.xqueezeme.xtoys.color.detector;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;

public class XToysDetectColors {
    private final static int MAX_DISTANCE = 2000;
    private final static int UPDATE_RATE = 1000;

    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);
    public static void main(String[] args) {
        if(args.length == 3) {
            var webhookId = args[0];
            var primaryColor = Color.decode(args[1]);
            var secondaryColor = Color.decode(args[2]);
            try {
                Robot r = new Robot();

                while (true) {
                    final Data data = countMatchingPixels(r, primaryColor, secondaryColor);
                    executorService.submit(() -> {
                        try {
                            webhook(webhookId, data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
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
            System.out.println("Please provide the following arguments: <webhookid> <color1> <color2>\n" +
                    "Example: java XToysDetectColors.java D2COE76wV65S #fbbd01 #eb4132");
        }
    }

    private static void webhook(String webhookId, Data data) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var url = "https://xtoys.app/webhook?id=" + webhookId + "&action=colors&color1="+data.primaryPercent +"&color2="+data.secondaryPercent;
        URI uri = URI.create(url);
        var request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .build();
        int statusCode = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(HttpResponse::statusCode)
                .join();
        System.out.println(statusCode+ " : " + url);
    }

    public static Data countMatchingPixels(Robot r, Color primaryColor, Color secondaryColor) throws IOException {
        Rectangle capture = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage image = r.createScreenCapture(capture);
        var totalPixels = image.getWidth() * image.getHeight();
        int primaryColorCount = 0;
        int secondaryColorCount = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for(int y = 0; y < image.getHeight(); y++) {
                var rgb = image.getRGB(x, y);
                var color = new Color(rgb);
                if (similarTo(color, primaryColor, MAX_DISTANCE)) {
                    primaryColorCount++;
                }
                if (similarTo(color, secondaryColor, MAX_DISTANCE)) {
                    secondaryColorCount++;
                }
            }
        }

        int primaryPercent = (int) ((double) primaryColorCount/totalPixels*100);
        int secondaryPercent =  (int) ((double) secondaryColorCount/totalPixels*100);
        System.out.println(LocalTime.now().toString() + ": Primary color: " + primaryPercent + "%, Secondary color: " + secondaryPercent + "%, Resolution: " + image.getWidth() + "x" + image.getHeight());

        return new Data(primaryPercent, secondaryPercent);
    }

    static boolean similarTo(Color a, Color b, double maxDistance){
        double distance = (b.getRed() - a.getRed())*(b.getRed() - a.getRed()) + (b.getGreen() - a.getGreen())*(b.getGreen() - a.getGreen()) + (b.getBlue() - a.getBlue())*(b.getBlue() - a.getBlue());
        if(distance < maxDistance){
            return true;
        }else{
            return false;
        }
    }
    static class Data {
        int primaryPercent;
        int secondaryPercent;

        Data(int primaryPercent, int secondaryPercent) {
            this.primaryPercent = primaryPercent;
            this.secondaryPercent = secondaryPercent;
        }
    }
}



