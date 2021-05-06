package jp.shiftinc.automation;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import java.io.IOException;

class SelenoidTestWatcher implements TestWatcher {

    @Override
    public void testSuccessful(ExtensionContext context) {
        try {
            tearDown(context);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        try {
            tearDown(context);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void tearDown(ExtensionContext context) throws IOException, InterruptedException {
        ExampleTest exampleTest = (ExampleTest)context.getRequiredTestInstance();
        exampleTest.downloadVideo();
        exampleTest.downloadLog();
    }
}
