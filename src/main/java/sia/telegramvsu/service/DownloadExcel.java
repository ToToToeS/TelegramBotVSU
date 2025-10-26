package sia.telegramvsu.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
public class DownloadExcel {

    private String oldHref;

    @Value("${path.excel}")
    private String pathExcel;
    @Value("${path.website}")
    private List<String> siteUrls;

    public boolean isNewSchedule() throws IOException {
        String href = findHref(siteUrls.get(0)).orElseThrow();
        if (oldHref.equals(href)) {
            oldHref = href;
            return true;
        } else return false;
    }


    private void downloadFile(String fileUrl, String outputFileName) throws IOException {

        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        connection.setInstanceFollowRedirects(true);

        try (InputStream in = connection.getInputStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(outputFileName)) {

            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }

    }

    public void downloadSchedules() {
        try {
            int i = 1;
            for (String siteUrl : siteUrls) {
                String href = findHref(siteUrl).orElseThrow();
                System.out.println(pathExcel + href.split("/")[4] + ".xlsx");
                downloadFile("https://vsu.by" + href, pathExcel + href.split("/")[4] + ".xlsx");
                log.info("Excel file downloaded successful");
            }
        } catch (IOException e) {
            log.error("Excel file no downloaded " + e.getMessage());
        }
    }

    public Optional<String> findHref(String siteUrl) throws IOException {
        String filePattern = ".xlsx";
        Document doc = Jsoup.connect(siteUrl).get();
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.attr("href");
            if (href.contains(filePattern)) {

                return Optional.of(href);
            }
        }


        return Optional.of(null);
    }
}
