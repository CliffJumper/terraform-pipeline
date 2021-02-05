import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ResetStaticStateExtension.class)
class RegressionStageTest {

    @Nested
    public class AutomationRepo {
        @AfterEach
        void reset() {
            Jenkinsfile.instance = mock(Jenkinsfile.class)
            Jenkinsfile.original = null
        }

        private configureJenkins(Map config = [:]) {
            Jenkinsfile.instance = mock(Jenkinsfile.class)
            Jenkinsfile.original = new Expando()
            Jenkinsfile.original.ApplyJenkinsfileClosure = { closure -> }
            when(Jenkinsfile.instance.getStandardizedRepoSlug()).thenReturn(config.repoSlug)
            when(Jenkinsfile.instance.getEnv()).thenReturn(config.env ?: [:])
        }

        @Test
        void automationRepoSpecifiedSuccessfullyCallApply() {
            RegressionStagePlugin fakePlugin = mock(RegressionStagePlugin.class)
            RegressionStage.addPlugin(fakePlugin)

            configureJenkins()
            RegressionStage stage = new RegressionStage().withScm("git:someHost:someUser/someRepo.git")
            stage.build()

            verify(fakePlugin).apply(stage)
        }

        @Test
        void automationRepoAndAppRepoSpecifiedSuccessfullyCallApply() {
            RegressionStagePlugin fakePlugin = mock(RegressionStagePlugin.class)
            RegressionStage.addPlugin(fakePlugin)

            configureJenkins()
            RegressionStage stage = new RegressionStage().withScm("git:someHost:someUser/someRepo.git")
                                                         .withScm("git:someHost:someUser/someOtherRepo.git")
            stage.build()

            verify(fakePlugin).apply(stage)
        }

        @Test
        void automationRepoAndAppRepoWithChangeDirectorySpecifiedSuccessfullyCallApply() {
            RegressionStagePlugin fakePlugin = mock(RegressionStagePlugin.class)
            RegressionStage.addPlugin(fakePlugin)

            configureJenkins()
            RegressionStage stage = new RegressionStage().withScm("git:someHost:someUser/someRepo.git")
                    .withScm("git:someHost:someUser/someOtherRepo.git")
                    .changeDirectory("someDir")
            stage.build()

            verify(fakePlugin).apply(stage)
        }

        @Test
        void noAutomationRepoSpecifiedSuccessfullyCallApply() {
            RegressionStagePlugin fakePlugin = mock(RegressionStagePlugin.class)
            RegressionStage.addPlugin(fakePlugin)

            configureJenkins()
            RegressionStage stage = new RegressionStage()
            stage.build()

            verify(fakePlugin).apply(stage)
        }
    }

    @Nested
    public class AddedPlugins {
        @Test
        void willHaveApplyCalled() {
            RegressionStagePlugin fakePlugin = mock(RegressionStagePlugin.class)
            RegressionStage.addPlugin(fakePlugin)

            RegressionStage stage = new RegressionStage()
            stage.applyPlugins()

            verify(fakePlugin).apply(stage)
        }
    }
}
