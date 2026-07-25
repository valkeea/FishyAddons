## Source Management

This module (common) holds all base sources, which can be modified depending on version requirements.

### Build Process

1. During build configuration, each module:
   - Copies all common sources to `build/filtered-common/`
   - Excludes any files that have local overrides
   - Excludes any files marked as version-incompatible -> `versionExcludes`
   - Adds the filtered directory as a source set

2. Gradle compiles using:
   - Local sources (highest priority)
   - Filtered common sources (automatically included)

#### fabric-26.1.2
Current reference version.