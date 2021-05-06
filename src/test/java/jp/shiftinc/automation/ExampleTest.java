package jp.shiftinc.automation;

import io.qameta.allure.Allure;
import io.qameta.allure.Story;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SelenoidTestWatcher.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExampleTest {
    private final String TEST_URL = "https://www.selenium.dev/downloads/";
    private RemoteWebDriver driver;
    private String baseUrl;
    private URL nodeUrl;
    private String videoName;
    private String sessionId;
    private String md5;

    @BeforeAll
    void setUpClass() throws MalformedURLException {
        baseUrl = System.getProperty("selenoid.base.url");
        nodeUrl = new URL(baseUrl + System.getProperty("selenoid.path"));
    }

    void setUp(Boolean allowPopup) {
        videoName = String.format("%s.mp4", RandomStringUtils.randomAlphanumeric(10));
        driver = new RemoteWebDriver(nodeUrl,setChromeOption(allowPopup));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        sessionId = driver.getSessionId().toString();
    }

    ChromeOptions setChromeOption(Boolean allowPopup) {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("download.default_directory", "/home/selenium/Downloads");
        prefs.put("download.prompt_for_download", allowPopup);
        options.setExperimentalOption("prefs", prefs);
        return options.merge(setCapabilities());
    }

    DesiredCapabilities setCapabilities() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setBrowserName("chrome");
        capabilities.setCapability("enableVideo", true);
        capabilities.setCapability("enableVNC", true);
        capabilities.setCapability("enableLog", true);
        capabilities.setCapability("videoName", videoName);
        return capabilities;
    }

    @Story("Example for download file")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void canDownloadCorrectFile(boolean allowPopup) throws IOException, InterruptedException {
        String IE_ZIP = "IEDriverServer_Win32_3.150.1.zip";
        String REFER_PATH = "src/test/resources/" + IE_ZIP;

        setUp(allowPopup);

        // store original file MD5
        assertMD5(new File(REFER_PATH), false);

        // access url
        driver.get(TEST_URL);
        attachFileToReport(driver.getScreenshotAs(OutputType.FILE), "img01","image/png","png");

        // download file
        WebElement elm = driver.findElement(By.cssSelector("a[href*=\"IEDriverServer_Win32_3\"]"));
        elm.click();

        // get file by using selenoid api
        URL url = new URL(String.format("%s/download/%s/%s", baseUrl, sessionId, IE_ZIP));
        File download = downloadFile(url);
        attachFileToReport(download, "IEDriverServer", "application/octet-stream", "exe");

        // assert file by MD5
        assertMD5(download, true);
    }

    @Story("Example for failed to download(please check browser log)")
    @Test
    void cannotClickElement() throws IOException {
        setUp(true);
        driver.get(TEST_URL);
        attachFileToReport(driver.getScreenshotAs(OutputType.FILE), "img02","image/png","png");
        driver.findElement(By.cssSelector("a[href*=\"not_exists\"]")).click();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    void downloadVideo() throws IOException, InterruptedException {
        final String URL_FORMAT = "%s/video/%s";
        Thread.sleep(2000);
        URL url = new URL(String.format(URL_FORMAT, baseUrl, videoName));
        attachFileToReport(downloadFile(url), "video", "video/mp4", "mp4");
    }

    void downloadLog() throws IOException, InterruptedException {
        final String URL_FORMAT = "%s/logs/%s.log";
        Thread.sleep(100);
        URL url = new URL(String.format(URL_FORMAT, baseUrl, sessionId));
        attachFileToReport(downloadFile(url), "browser-log-file", "text/plain", "log");
    }

    void attachFileToReport(File file, String name, String mimeType, String ext) throws IOException {
        Allure.addAttachment(
                name,
                mimeType,
                new ByteArrayInputStream(Files.readAllBytes(Paths.get(file.getAbsolutePath()))),
                ext
        );
    }

    void assertMD5(File file, Boolean compare) throws IOException {
        if (compare) {
            assertEquals(md5, DigestUtils.md5Hex(new FileInputStream(file)));
        } else {
            md5 = DigestUtils.md5Hex(new FileInputStream(file));
        }
    }

    File downloadFile(URL url) throws IOException, InterruptedException {
        String path = url.getPath();
        Thread.sleep(5000);
        String downloadFile = "build/tmp/" + path.substring(path.lastIndexOf("/") + 1);
        try (DataInputStream in = new DataInputStream(url.openStream());
             DataOutputStream out = new DataOutputStream(new FileOutputStream(downloadFile))) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
        }
        return new File(downloadFile);
    }
}
