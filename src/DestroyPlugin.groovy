class DestroyPlugin implements TerraformEnvironmentStagePlugin {

    private static arguments = []

    public static void init() {
        DestroyPlugin plugin = new DestroyPlugin()

        ConfirmApplyPlugin.withConfirmMessage('WARNING! Are you absolutely sure the plan above is correct? Your environment will be IMMEDIATELY DESTROYED via "terraform destroy"')
        ConfirmApplyPlugin.withOkMessage("Run terraform DESTROY now")

        TerraformEnvironmentStage.addPlugin(plugin)
    }

    public static withArgument(String arg) {
        arguments << arg
        return this
    }

    @Override
    public void apply(TerraformEnvironmentStage stage) {
        stage.withStrategy(new DestroyStrategy(arguments))
    }

}
