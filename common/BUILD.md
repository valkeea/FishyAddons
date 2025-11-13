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

_Clearing the cache is required after modifying common sources_  

### Current Exclusions

All modules host /mixin for clarity.

#### fabric-1.21.10, 1.21.8
- **`render/FaLayers.java`**: No longer in use due to DrawContext changes

#### fabric-1.21.5+
- Current reference version (no exclusions)

**Overrides are shown in build output**