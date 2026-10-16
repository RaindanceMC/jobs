# RaindanceMC/jobs

- **`jobs-module`** — Rain's Flexible Modules — Jobs: Server goals on a job board: everybody delivers or fishes towards a goal that runs for days, and is paid by their share of it. Each goal learns how big the next one of its kind should be.
- **`jobs-standalone`** — Rain's Jobs: Rain's Jobs, as a plugin of its own: the jobs module and the standard wrapper, shaded together.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Split out of `rains-flexible-modules` on 16.10.2026, history kept.

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `jobs-standalone/target/RainsJobs-0.9.3.jar` — also published as `de.raindancer.modules:jobs-standalone:0.9.3`

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.
