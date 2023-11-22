package io.goobi.viewer.model.files.external;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import io.goobi.viewer.TestServlet;

public class ExternalFilesDownloaderTest {

    private final Path testZipFile = Path.of("src/test/resources/data/viewer/external-files/1287088031.zip");
    private final Path downloadFolder = Path.of("src/test/resources/output/external-files");
    private final TestServlet server = new TestServlet("127.0.0.1", 9191);

    @Test
    public void test() throws IOException, InterruptedException, ExecutionException, TimeoutException {

        byte[] body = Files.readAllBytes(testZipFile);
        server.getServerClient()
                .when(HttpRequest.request().withPath("/exteral/files/1287088031.zip"))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", "application/zip")).withBody(body));

        assertTrue(Files.isDirectory(downloadFolder) || Files.createDirectory(downloadFolder) != null);
        //        URI uri = testZipFile.toAbsolutePath().toUri();
        //        URI uri = URI.create("https://d-nb.info/1287088031/34");
//                URI uri = URI.create("https://www.splittermond.de/wp-content/uploads/2016/01/Splittermond_GRW_erratiert.pdf");
        
        List<DownloadResult> downloads = new ArrayList<>();
        for(int i = 0; i < 10; i++) {   
            URI uri = URI.create("http://127.0.0.1:9191/exteral/files/1287088031.zip");
            ExternalFilesDownloader download = new ExternalFilesDownloader(downloadFolder, l -> handleProgress(l));
            DownloadResult result = download.downloadExternalFiles(uri);
            downloads.add(result);
        }
        for (DownloadResult result : downloads) {
            Path downloaded = result.getPath().get(2000, TimeUnit.MILLISECONDS);
            assertTrue(Files.exists(downloaded));
        }
    }
    
    private void handleProgress(long l) {
        System.out.println("Progress " + l);
    }

    @After
    public void after() throws Exception {
        server.shutdown();
        if (Files.exists(downloadFolder)) {
            FileUtils.deleteDirectory(downloadFolder.toFile());
        }
    }

}
