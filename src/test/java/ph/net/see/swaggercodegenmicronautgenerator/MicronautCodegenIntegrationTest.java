package ph.net.see.swaggercodegenmicronautgenerator;

import io.swagger.codegen.v3.cli.SwaggerCodegen;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MicronautCodegenIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicronautCodegenIntegrationTest.class);
    private static File projectDirectory;
    private static String testDir;

    @BeforeAll
    static void setup() throws IOException {
        URL markerUrl = Thread.currentThread().getContextClassLoader().getResource(".marker");
        assertNotNull(markerUrl, "[GUARD] Should have been able to locate .marker from the test classpath");

        testDir = new File(markerUrl.getFile()).getParent();
        projectDirectory = new File(testDir, "output-project");

        FileUtils.deleteDirectory(projectDirectory);
    }

    @Test
    @Order(1)
    void shouldBeAbleToCreateProject() throws InterruptedException {
        File petstoreYaml = new File(testDir, "petstore.yaml");

        String[] args = new String[] {
                "generate",
                "-l", "ph.net.see.swaggercodegenmicronautgenerator.MicronautCodegen",
                "-i", petstoreYaml.getAbsolutePath(),
                "-o", projectDirectory.getAbsolutePath()};

        LOGGER.info("Executing {}", String.join(" ", args));

        File mvnCommand = new File(projectDirectory, "mvnw");

        SwaggerCodegen.main(args);

        long start = System.currentTimeMillis();
        while(!mvnCommand.exists() &&
                (System.currentTimeMillis() - start) < 10_000) {
            Thread.sleep(100);
        }

        assertTrue(mvnCommand.exists(), mvnCommand.getAbsolutePath()
                + " should have existed after " + (System.currentTimeMillis() - start)/1000 + " seconds");
    }

    @Test
    @Order(2)
    void shouldBeAbleToBuildProject() throws IOException, InterruptedException {
        List<String> command = new LinkedList<>();
        command.addAll(isWindows() ? singleton("mvn.cmd") : asList("sh", "mvnw"));
        command.addAll(asList("clean", "install"));

        LOGGER.info("Executing {} from directory {}", String.join(" ", command), projectDirectory.getAbsolutePath());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectDirectory);
        builder.inheritIO();
        Process process = builder.start();

        process.waitFor(2, TimeUnit.MINUTES);

        assertEquals(0, process.exitValue(), "Building the project should have been a success");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

}
