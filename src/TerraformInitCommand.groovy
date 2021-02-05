class TerraformInitCommand {
    private boolean input = false
    private String terraformBinary = "terraform"
    private String command = "init"
    String environment
    private prefixes = []
    private suffixes = []
    private backendConfigs = []
    private boolean doBackend = true
    private String directory

    private static globalPlugins = []
    private appliedPlugins = []

    public TerraformInitCommand(String environment) {
        this.environment = environment
    }

    public TerraformInitCommand withInput(boolean input) {
        this.input = input
        return this
    }

    public TerraformInitCommand withPrefix(String prefix) {
        prefixes << prefix
        return this
    }

    public TerraformInitCommand withSuffix(String suffix) {
        suffixes << suffix
        return this
    }

    public TerraformInitCommand withDirectory(String directory) {
        this.directory = directory
        return this
    }

    public TerraformInitCommand withBackendConfig(String backendConfig) {
        this.backendConfigs << backendConfig
        return this
    }

    public TerraformInitCommand withoutBackend() {
        this.doBackend = false
        return this
    }

    public String toString() {
        applyPluginsOnce()

        def pieces = []
        pieces += prefixes
        pieces << terraformBinary
        pieces << command
        if (!input) {
            pieces << "-input=false"
        }
        if (doBackend) {
            backendConfigs.each { config ->
                pieces << "-backend-config=${config}"
            }
        } else {
            pieces << "-backend=false"
        }
        if (directory) {
            pieces << directory
        }

        pieces += suffixes

        return pieces.join(' ')
    }

    private applyPluginsOnce() {
        def remainingPlugins = globalPlugins - appliedPlugins

        for (TerraformInitCommandPlugin plugin in remainingPlugins) {
            plugin.apply(this)
            appliedPlugins << plugin
        }
    }

    public static void addPlugin(TerraformInitCommandPlugin plugin) {
        this.globalPlugins << plugin
    }

    public static TerraformInitCommand instanceFor(String environment) {
        return new TerraformInitCommand(environment)
    }

    public static getPlugins() {
        return this.globalPlugins
    }

    public static reset() {
        globalPlugins = []
        // This is awkward - what about the applied plugins...?
    }
}
