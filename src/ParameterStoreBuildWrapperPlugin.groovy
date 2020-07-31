import static TerraformEnvironmentStage.PLAN
import static TerraformEnvironmentStage.APPLY

class ParameterStoreBuildWrapperPlugin implements TerraformEnvironmentStagePlugin {
    private static globalPathPattern
    private static defaultPathPattern = { options -> "/${options['organization']}/${options['repoName']}/${options['environment']}/" }

    public static void init() {
        TerraformEnvironmentStage.addPlugin(new ParameterStoreBuildWrapperPlugin())
    }

    public static withPathPattern(Closure newPathPattern) {
        globalPathPattern = newPathPattern
        return this
    }

    @Override
    public void apply(TerraformEnvironmentStage stage) {
        def environment = stage.getEnvironment()
        def parameterStorePath = pathForEnvironment(environment)

        def options = [
            path: parameterStorePath,
            credentialsId: "${environment.toUpperCase()}_PARAMETER_STORE_ACCESS"
        ]

        stage.decorate(PLAN, addParameterStoreBuildWrapper(options))
        stage.decorate(APPLY, addParameterStoreBuildWrapper(options))
    }

    String pathForEnvironment(String environment) {
        String organization = Jenkinsfile.instance.getOrganization()
        String repoName = Jenkinsfile.instance.getRepoName()
        def patternOptions = [ environment: environment,
                               repoName: repoName,
                               organization: organization ]

        def pathPattern = globalPathPattern ?: defaultPathPattern

        return pathPattern(patternOptions)
    }

    public static Closure addParameterStoreBuildWrapper(Map options = []) {
        def Map defaultOptions = [
            naming: 'basename'
        ]

        def parameterStoreOptions = defaultOptions + options

        return { closure ->
            withAWSParameterStore(parameterStoreOptions) {
                closure()
            }
        }
    }

    public static reset() {
        globalPathPattern = null
    }
}
