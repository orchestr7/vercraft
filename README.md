# VerCraft
**A simple and intuitive tool for versioning your JVM applications.**

### What is VerCraft?
1. **Calculates** the current version in SemVer format based on the checked-out commit and **applies** it to build tools and release plugins.
2. **Makes release preparations** by calculating the next version for the upcoming release, 
**creating** a new release **branch**, and **tagging** the commit with the generated version.

### Supported Build Tools
- ✅ **Gradle**
- 🚧 **Maven** – Support coming soon (TBD)

### Quick start
Add following plugin to your parent build.gradle(kts):
```
plugins {
    id("com.akuleshov7.vercraft.plugin-gradle") version("0.0.1")
}
```

Run following tasks to create local release tag, local release branch, calculate and return version:
```
./gradlew makeRelease -PreleaseType=MAJOR
./gradlew makeRelease -PreleaseType=MINOR
```

Calculate, set (`project.version`) and return version for current checked-out commit
```
./gradlew gitVersion
```

### How it works
![docs/example.png](docs/example.png)

### Why Choose VerCraft?
There are already several great tools like `gradle-plugin-versionest`, `reckon`, `JGitver`, and `nebula-release-plugin` 
that calculate project versions based on the current commit and branch. Most of these tools rely on tags and offer extensive configurability.

**However...**  
In practice, these tools often introduce complex configurations without enforcing 
a structured Git workflow—something essential in 80% of enterprise development environments.

VerCraft takes a different approach:
- **Simplicity over complexity** – Versioning and releasing pipeline based on release branches
is streamlined, familiar to most of developers in enterprise and easy to adopt.
- **Structured workflow** – VerCraft encourages a straightforward, Git-driven approach to versioning.

Unlike other tools, VerCraft doesn't treat Git tags as a database for previous releases. 
Instead, it uses **release branches** and some extra heuristics, ensuring that your repository 
remains clean and release processes are strict. 
Only necessary release tags and branches are created — unnecessary "tag zoo" is not made.

### Recommended Git workflow
TBD

### Local Development
Run the following commands for local builds or to publish to your local Maven repository:
```bash
./gradlew publishToMavenLocal
./gradlew build