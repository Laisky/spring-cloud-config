# Qualified historical build baseline

The original external 2.2.0.BUILD-SNAPSHOT parent is unavailable. Its failure
before tests is retained as evidence; the vanished artifact is not retried or
claimed qualified.

The fork's September 2019 parent source targeted Java 8, Spring Boot
2.2.0.BUILD-SNAPSHOT and formatter 0.0.9. The official Config 2.2.0 release uses
Build/Commons 2.2.0.RELEASE. This repair uses those available same-family releases,
the matching dependencies parent and Bus 2.2.0.RELEASE; the resulting runtime
baseline is Java 8/Boot 2.2.1.RELEASE. Project and child artifact versions remain
2.2.0.BUILD-SNAPSHOT, and production Java sources/public APIs are unchanged.
These are historical released artifacts, not a claim of current platform support.

The same-release upstream Checkstyle suppressions are kept locally because
unchanged client tests fail when the inherited moving URL cannot be read offline.
The original project-specific suppression file remains in use. Source:
https://raw.githubusercontent.com/spring-cloud/spring-cloud-build/v2.2.0.RELEASE/spring-cloud-build-tools/src/checkstyle/checkstyle-suppressions.xml

## Behavior acceptance

Use JDK 8 and Maven 3.9.9, with dependencies cached before an offline run:

~~~sh
mvn -o -B -ntp -pl spring-cloud-config-server -am \
  -Dtest=AwsS3EnvironmentRepositoryTests,AwsS3CompatibilityTests,JGitSubmoduleCompatibilityTests \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

All 13 focused tests pass without skips. The six added regressions exercise real
local submodule cloning and ordinary root configuration, S3 format precedence,
server overrides, fallback request order and object-stream closure. AWS is
mocked; Git fixtures use local paths and no credentials. Plain absolute paths
exercise the clone path; file: URIs would read in place and would not guard
the fork's clone feature.

The broader client/Git/S3 selection passes 101 tests without skips on both
AWS SDK 1.11.52 and 1.12.261 with identical repaired build coordinates and test
sources. A controlled removal of setCloneSubmodules(true) makes exactly the
submodule configuration regression fail while the plain-repository test passes.
The control source is restored exactly afterward. Test compilation of the
client/server/monitor/sample/starter reactor also passes.

A further 36 local monitor tests pass without skips on both controls: webhook
path extraction, event destinations and auto-configuration. No broker is started.

The original vanished snapshot runtime cannot be compared directly. Full
integration suites, real AWS/SSH/Redis/Vault/broker connections, documentation
rendering and production deployment are outside this acceptance.

## Automatic CI and publication

The existing CircleCI source now uses the same JDK 8/Maven image, one small job,
format checks and only the 13 fast behavior units. Full integration/coverage
runs stay local; the old codecov upload and stored-artifact steps are removed.
The job invokes test, never install/deploy/publish. No credentials or host,
account, network or security settings are changed.
