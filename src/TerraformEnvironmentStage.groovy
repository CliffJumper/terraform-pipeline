class TerraformEnvironmentStage implements Stage {
    private Jenkinsfile jenkinsfile
    private String environment
    private StageDecorations decorations
    private localPlugins

    private static final DEFAULT_PLUGINS = [ new ConditionalApplyPlugin(), new ConfirmApplyPlugin(), new DefaultEnvironmentPlugin() ]
    private static globalPlugins = DEFAULT_PLUGINS.clone()
    private static Closure stageNamePattern

    public static final String ALL = 'all'
    public static final String PLAN = 'plan'
    public static final String CONFIRM = 'confirm'
    public static final String APPLY = 'apply'

    TerraformEnvironmentStage(String environment) {
        this.environment = environment
        this.jenkinsfile = Jenkinsfile.instance
        this.decorations = new StageDecorations()
    }

    public Stage then(Stage nextStage) {
        return new BuildGraph(this).then(nextStage)
    }

    public static withGlobalEnv(String key, String value) {
        def plugin = new EnvironmentVariablePlugin()
        plugin.withEnv(key, value)
        addPlugin(plugin)

        return this
    }

    public TerraformEnvironmentStage withEnv(String key, String value) {
        def plugin = new EnvironmentVariablePlugin()
        plugin.withEnv(key, value)

        reconcileLocalAndGlobalPlugins(plugin)

        return this
    }

    public void build() {
        Jenkinsfile.build(pipelineConfiguration())
    }

    public Closure pipelineConfiguration() {
        def initCommand = TerraformInitCommand.instanceFor(environment)
        def planCommand = TerraformPlanCommand.instanceFor(environment)
        def applyCommand = TerraformApplyCommand.instanceFor(environment)

        applyPlugins()

        def String environment = this.environment
        return { ->
            node(jenkinsfile.getNodeName()) {
                deleteDir()
                checkout(scm)

                decorations.apply(ALL) {
                    stage(getStageNameFor(PLAN)) {
                        decorations.apply(PLAN) {
                            sh initCommand.toString()
                            sh planCommand.toString()
                        }
                    }

                    decorations.apply("Around-${CONFIRM}") {
                        // The stage name needs to be editable
                        stage(getStageNameFor(CONFIRM)) {
                            decorations.apply(CONFIRM) {
                                echo "Approved"
                            }
                        }
                    }

                    decorations.apply("Around-${APPLY}") {
                        // The stage name needs to be editable
                        stage(getStageNameFor(APPLY)) {
                            decorations.apply(APPLY) {
                                sh initCommand.toString()
                                sh applyCommand.toString()
                            }
                        }
                    }
                }
            }
        }
    }

    public String getStageNameFor(String command) {
        def pattern = stageNamePattern ?: { options -> "${options['command']}-${options['environment']}" }
        def options = [ command: command, environment: environment ]

        return pattern.call(options)
    }

    public void decorate(Closure decoration) {
        decorations.add(ALL, decoration)
    }

    public decorate(String stageName, Closure decoration) {
        decorations.add(stageName, decoration)
    }

    public decorateAround(String stageName, Closure decoration) {
        decorations.add("Around-${stageName}", decoration)
    }

    public String toString() {
        return environment
    }

    public static addPlugin(plugin) {
        globalPlugins << plugin
    }

    public void applyPlugins() {
        // Apply both global and local plugins, in the correct order
        for (plugin in getAllPlugins()) {
            plugin.apply(this)
        }
    }

    public String getEnvironment() {
        return environment
    }

    /* Returns global globalPlugins, in addition to all
     * plugins that have been added to this instance
     */
    public getAllPlugins() {
        return reconcileLocalAndGlobalPlugins()
    }

    private reconcileLocalAndGlobalPlugins(TerraformEnvironmentStagePlugin newPlugin = null) {
        if (localPlugins == null) {
            if (newPlugin == null) {
                // No local plugins were added - only global plugins take effect
                return globalPlugins
            }

            // The first local plugin was added.  It takes effective *after* every current global plugin
            localPlugins = globalPlugins.clone()
            localPlugins << newPlugin
            return localPlugins
        }
        // We're here because a localPlugin was previously added.  Check if any new global plugins
        // have been added since.

        // Start off with all global plugins
        def remainingGlobalPlugins = globalPlugins.clone()
        for (def plugin in localPlugins) {
            // If all global plugins are accounted for, stop
            if (remainingGlobalPlugins.isEmpty()) {
                break;
            }
            // Cross off each global plugin that has not yet been accounted for
            if (remainingGlobalPlugins.first() == plugin) {
                remainingGlobalPlugins.remove(plugin)
            }
            // If the plugin was not in remainingGlobalPlugins, it means it was added locally
        }

        // Any global plugins that remain in this list have been added since the last time we checked.
        // Add them now.
        localPlugins.addAll(remainingGlobalPlugins)

        // If we have a new plugin to add, do it now
        if (newPlugin != null) {
            localPlugins << newPlugin
        }

        return localPlugins
    }

    public static getPlugins() {
        return globalPlugins
    }

    public static withStageNamePattern(Closure stageNamePattern) {
        this.stageNamePattern = stageNamePattern
    }

    public static void reset() {
        // This totally jacks with localPlugins
        this.globalPlugins = DEFAULT_PLUGINS.clone()
        this.stageNamePattern = null
    }
}
