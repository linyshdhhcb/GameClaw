package ai.gameclaw.project;

public interface ProjectManager {

    String createProject(String name, String description);

    Project getProject(String projectId);

    void deleteProject(String projectId);
}
