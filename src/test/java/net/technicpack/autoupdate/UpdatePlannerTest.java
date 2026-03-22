package net.technicpack.autoupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.technicpack.autoupdate.io.StreamVersion;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.install.plan.ExecutionPlan;
import net.technicpack.launchercore.install.plan.PlanNode;
import net.technicpack.rest.RestfulAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UpdatePlannerTest {
  @TempDir Path tempDir;

  @Test
  void plannerIncludesResourceDownloadsAndMoverLaunchBeforeExecutionStarts() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-root"));
    UpdatePlanner planner = new UpdatePlanner(fileSystem);

    StreamVersion version =
        parseVersion(
            "{\n"
                + "  \"build\": 5,\n"
                + "  \"url\": {\n"
                + "    \"jar\": \"https://example.invalid/launcher.jar\",\n"
                + "    \"exe\": \"https://example.invalid/launcher.exe\"\n"
                + "  },\n"
                + "  \"resources\": [\n"
                + "    {\n"
                + "      \"filename\": \"discover.json\",\n"
                + "      \"url\": \"https://example.invalid/discover.json\"\n"
                + "    }\n"
                + "  ]\n"
                + "}");

    UpdatePlanner.UpdatePlanRequest request =
        new UpdatePlanner.UpdatePlanRequest(
            "stable",
            4,
            false,
            false,
            "/tmp/current-launcher.jar",
            tempDir.resolve("temp-launcher.jar"),
            "Launcher Update",
            "Launching mover...",
            "Downloading Launcher Asset: %s",
            "Launcher Update");

    ExecutionPlan<UpdatePlanner.UpdateContext> plan =
        planner.planUpdater(new FixedUpdateStream(version), request);

    List<String> nodeIds =
        plan.getNodes().stream().map(PlanNode::getId).collect(Collectors.toList());

    assertEquals(
        Arrays.asList(
            "download-resource-discover.json", "download-launcher-update", "launch-mover"),
        nodeIds);
    assertEquals(
        "Downloading Launcher Asset: discover.json",
        plan.getNodes().stream()
            .filter(node -> node.getId().equals("download-resource-discover.json"))
            .findFirst()
            .orElseThrow(AssertionError::new)
            .getDescription());
    assertEquals(
        Arrays.asList("download-launcher-update"),
        plan.getNodes().stream()
            .filter(node -> node.getId().equals("launch-mover"))
            .findFirst()
            .orElseThrow(AssertionError::new)
            .getDependencies());
  }

  @Test
  void plannerOmitsLauncherUpdateNodesWhenBuildIsCurrent() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-root"));
    UpdatePlanner planner = new UpdatePlanner(fileSystem);

    StreamVersion version =
        parseVersion(
            "{\n"
                + "  \"build\": 5,\n"
                + "  \"url\": {\n"
                + "    \"jar\": \"https://example.invalid/launcher.jar\",\n"
                + "    \"exe\": \"https://example.invalid/launcher.exe\"\n"
                + "  },\n"
                + "  \"resources\": [\n"
                + "    {\n"
                + "      \"filename\": \"discover.json\",\n"
                + "      \"url\": \"https://example.invalid/discover.json\"\n"
                + "    }\n"
                + "  ]\n"
                + "}");

    UpdatePlanner.UpdatePlanRequest request =
        new UpdatePlanner.UpdatePlanRequest(
            "stable",
            5,
            false,
            false,
            "/tmp/current-launcher.jar",
            tempDir.resolve("temp-launcher.jar"),
            "Launcher Update",
            "Launching mover...",
            "Downloading Launcher Asset: %s",
            "Launcher Update");

    ExecutionPlan<UpdatePlanner.UpdateContext> plan =
        planner.planUpdater(new FixedUpdateStream(version), request);

    List<String> nodeIds =
        plan.getNodes().stream().map(PlanNode::getId).collect(Collectors.toList());

    assertEquals(Arrays.asList("download-resource-discover.json"), nodeIds);
    assertTrue(plan.getNodes().stream().noneMatch(node -> node.getId().equals("launch-mover")));
  }

  @Test
  void plannerUsesGenericPhaseLabelAndZeroWeightForLauncherHandoff() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-root"));
    UpdatePlanner planner = new UpdatePlanner(fileSystem);

    StreamVersion version =
        parseVersion(
            "{\n"
                + "  \"build\": 5,\n"
                + "  \"url\": {\n"
                + "    \"jar\": \"https://example.invalid/launcher.jar\",\n"
                + "    \"exe\": \"https://example.invalid/launcher.exe\"\n"
                + "  },\n"
                + "  \"resources\": []\n"
                + "}");

    UpdatePlanner.UpdatePlanRequest request =
        new UpdatePlanner.UpdatePlanRequest(
            "stable",
            4,
            false,
            false,
            "/tmp/current-launcher.jar",
            tempDir.resolve("temp.jar"),
            "Launcher Update",
            "Launching mover...",
            "Downloading Launcher Asset: %s",
            "Launcher Update");

    ExecutionPlan<UpdatePlanner.UpdateContext> plan =
        planner.planUpdater(new FixedUpdateStream(version), request);

    assertEquals("Launcher Update", plan.getPhases().iterator().next().getLabel());
    assertEquals(
        "Launcher Update",
        plan.getNodes().stream()
            .filter(node -> node.getId().equals("download-launcher-update"))
            .findFirst()
            .orElseThrow(AssertionError::new)
            .getDescription());
    assertEquals(
        0.0f,
        plan.getNodes().stream()
            .filter(node -> node.getId().equals("launch-mover"))
            .findFirst()
            .orElseThrow(AssertionError::new)
            .getWeight(),
        0.0001f);
  }

  @Test
  void moverPlanDoesNotReserveProgressForFinalLaunchHandoff() {
    UpdatePlanner planner = new UpdatePlanner(null);

    ExecutionPlan<UpdatePlanner.UpdateContext> plan =
        planner.planMover(
            new UpdatePlanner.MoverPlanRequest(
                tempDir.resolve("launcher.jar"),
                false,
                "Copying update...",
                "Launching Technic..."));

    assertEquals(
        0.0f,
        plan.getNodes().stream()
            .filter(node -> node.getId().equals("launch-launcher-mode"))
            .findFirst()
            .orElseThrow(AssertionError::new)
            .getWeight(),
        0.0001f);
  }

  private static StreamVersion parseVersion(String json) {
    return new Gson().fromJson(json, StreamVersion.class);
  }

  private static class FixedUpdateStream implements IUpdateStream {
    private final StreamVersion version;

    private FixedUpdateStream(StreamVersion version) {
      this.version = version;
    }

    @Override
    public StreamVersion getStreamVersion(String stream) throws RestfulAPIException {
      return version;
    }
  }
}
