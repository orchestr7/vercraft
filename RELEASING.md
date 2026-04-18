# Release Guide for Vercraft

## If You Encounter `401`

Ensure you are using a **valid TOKEN**, not a password:
[Generate a portal token](https://central.sonatype.org/publish/generate-portal-token/)

---

## How to Release a New Version of Vercraft

1. Make sure you have **permissions to push** to the main repository.
2. Create a **release branch** and **release tag**.

Use the Vercraft Gradle script:

```bash
./gradlew makeRelease
```

This will create, for example:

* Branch: `release/0.7.x`
* Tag: `0.7.0`

Once the release workflow starts:

* The **version number** is determined from the tag
* Binaries are uploaded to **Maven Central**
* A new **GitHub release** is created with the fat JAR

> Note: We publish releases to both Maven Central and GitHub.
> To prevent invalid files from reaching Nexus, you should **manually promote releases** in [Staging](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#documentation-of-manual-api-endpoints).

---

## Manual Promotion after Staging release from CI.

In the logs, you will see something like:

```text
Created staging repository 'com.akuleshov7--367d523a-ce26-49b3-91f1-e37efd243dab' at https://ossrh-staging-api.central.sonatype.com/service/local/repositories/com.akuleshov7--367d523a-ce26-49b3-91f1-e37efd243dab/content/
```

**Important:** To promote this repository, you must use the **exact same token** that was used for publishing.

```bash
# Use the same token that was used for publishing
export TOKEN='YOUR_TOKEN'

# Check status of repositories
curl -H "Authorization: Bearer $TOKEN" \
"https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?ip=any&profile_id=com.akuleshov7"

# Example response:
# {"repositories":[{"key":"nAOjPo/any/com.akuleshov7--367d523a-ce26-49b3-91f1-e37efd243dab","state":"open","description":"com.akuleshov7.vercraft:vercraft:0.7.0","portal_deployment_id":null}]}

# Promote repository to Maven Central
curl -X POST -H "Authorization: Bearer $TOKEN" \
"https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/nAOjPo/any/com.akuleshov7--367d523a-ce26-49b3-91f1-e37efd243dab?publishing_type=automatic"
```
