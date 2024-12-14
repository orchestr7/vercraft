import com.akuleshov7.vercraft.core.ReleaseBranches
import com.akuleshov7.vercraft.core.SemVerReleaseType
import com.akuleshov7.vercraft.core.VersionCalculator
import org.eclipse.jgit.api.Git
import java.io.File


public fun main() {
    Git.open(File(".")).use { git ->
        val releaseBranches = ReleaseBranches(git)
        git.repository.use { repo ->
            val version = VersionCalculator(git, releaseBranches, repo)
            println(version.calc())
            releaseBranches.createNewRelease(SemVerReleaseType.MAJOR)
        }
    }
}
