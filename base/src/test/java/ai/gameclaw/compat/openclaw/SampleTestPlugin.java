package ai.gameclaw.compat.openclaw;

@OpenClawPlugin(name = "sample-test-plugin", version = "1.0.0", permissions = {"fs:read:workspace/"})
public class SampleTestPlugin implements OpenClawTool {

    @Override
    public String name() {
        return "sample_test_tool";
    }

    @Override
    public String description() {
        return "A sample test tool for unit testing";
    }

    @Override
    public String execute(String input) {
        return "Echo: " + input;
    }
}
