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

_Clearing the cache is required after editing overrides_  

### Current Exclusions

All modules host /mixin for clarity.

#### fabric-1.21.10
- Current reference version

**Overrides are shown in build output**