## Source Management

This module (common) holds all base sources, which can be modified depending on version requirements.
26.1.2 is currently built independently.

### Build Process

1. During build configuration, each module:
   - Copies all common sources to `build/filtered-common/`
   - Excludes any files that have local overrides
   - Excludes any files marked as version-incompatible -> `versionExcludes`
   - Adds the filtered directory as a source set

2. Gradle compiles using:
   - Local sources (highest priority)
   - Filtered common sources (automatically included)

### Current Exclusions

All modules host /mixin for clarity.

#### fabric-1.21.10
Current reference version.
_This will be moved to 26.1.2 once Hypixel drops support for 1.21.x._