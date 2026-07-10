# ClassLoader Directory

The Java ClassLoader can only load classes from files on the filesystem. This directory is the staging area Cherry uses to make connector JARs accessible to the ClassLoader.

JAR files are copied here from three sources:

- **Database storage** — connectors stored as BLOBs are extracted here at load time.
- **Store installation** — when a connector is installed from the Store (Camunda Connectors, Marketplace, CommunityHub, or a private GitHub repo), the downloaded JAR is placed here.
- **Manual upload** — when a user uploads a JAR through the UI, it is also copied here so it can be loaded immediately.

This directory is managed automatically by Cherry. Do not manually add or remove files while the application is running.
