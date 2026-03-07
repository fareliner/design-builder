# Flatten File Collision Enhancement

## Status
- Proposed
- Owner: `design-builder-maven-plugin` flatten workflow
- Companion technical draft: [Flatten File Collision Design Draft 1](/Users/bertramn/workspaces/fares-io/oss/design-builder/docs/flatten-file-collision-design-draft1.md)

## Problem Statement
The flatten goal writes resolved XML resources (XSD/WSDL/XSL) into a single target folder.  
Today, collisions can happen when two different resolved resources produce the same target filename (for example `Order.xsd`), especially when XML Catalog rules resolve references differently by namespace/system/public/suffix mappings.

With overwrite behavior enabled, later writes may silently replace earlier content. This can produce broken flattened outputs and difficult-to-diagnose defects.

## Design Constraints
1. Default behavior should keep flattened filenames readable and unchanged wherever possible.
2. Design tools and downstream consumers should continue to work without unexpected filename changes.
3. Collision detection must use resolved identity and content, not only the requested reference string.
4. Hash-based renamed outputs should be optional.

## Goals
1. Detect filename collisions deterministically during flattening.
2. Prevent silent data corruption from different content being written to the same output filename.
3. Preserve existing output names by default when no true collision exists.
4. Provide clear diagnostics when catalog resolution causes ambiguity.

## Non-Goals
1. Preserve byte-for-byte lexical formatting of serialized XML.
2. Replace XML catalog semantics or resolver strategy in this iteration (resolver behavior remains authoritative input).
3. Force global filename renaming in non-collision scenarios.

## Proposed Behavior

### Output Identity Tracking
Maintain an in-memory registry during a flatten run:
- Key: output filename (for example `Order.xsd`)
- Value:
  - first resolved identity key
  - first content hash
  - source trace (resolved URI + resolver details)

Also track:
- resolved identity key to assigned filename mapping

### Identity Key
Identity key should be resolver-aware:
- canonical resolved URI (required)
- optional resolver trace metadata (if available)

If resolver trace details are not available, canonical resolved URI remains the base identity.

### Two-Tier Identity and Comparison Strategy
To improve performance, collision adjudication should use a two-tier strategy:

1. Tier 1 (cheap): origin equivalence
- Compare normalized resource origin derived from resolver output, for example:
  - same local file path
  - same jar file path + entry path
  - same resolved artifact coordinates/path for Maven-backed resources
- If origins are equivalent, treat as same source candidate without immediate byte hashing.

2. Tier 2 (exact): content hash on demand
- If origin is different or uncertain, compute hash of raw bytes from resolved URI.
- Use hash comparison to determine whether content is actually equal or conflicting.

This preserves correctness while avoiding unnecessary hashing in common equivalent-origin cases.

### Content Hash
Use hash of resolved source bytes (not serialized output) as collision guard.
Prefer lazy hash computation:
1. Compute immediately for resources in known collision groups.
2. Defer for unambiguous resources until needed.

### Collision Decision
When writing resource `R` to target filename `F`:
1. If `F` not seen before: write and register.
2. If `F` seen and hash matches existing: treat as equivalent content; reuse existing mapping.
3. If `F` seen and hash differs: treat as collision and apply configured policy.

## Collision Policy
Add new configuration:
- `flatten.collisionPolicy`: `warn | fail | rename | auto`

Default:
- `warn` (safe migration path with visibility)

Behavior:
1. `warn`
- Keep first file bound to filename.
- Emit warning with both identities/hashes/resolution traces.
- Continue processing.

2. `fail`
- Throw `MojoExecutionException` on first differing-content collision.
- No silent corruption.

3. `rename`
- Keep first file as-is.
- Write conflicting resource with deterministic readable hash suffix:
  - `Order__a1b2c3.xsd` (3-byte hex suffix = 6 hex chars)
- Rewrite references to the renamed file.

4. `auto`
- Use a two-stage execution model (plan, then write).
- Detect all collision groups before writing any output file.
- Keep non-collision filenames unchanged.
- For each collision group, deterministically assign readable hash-suffixed names to all conflicting variants in that group.
- Rewrite all affected references consistently in a single write phase.
- Emit a plan summary before writing.

## Optional Hash-Suffix Naming
Hash suffix mode is opt-in through `collisionPolicy=rename` or `collisionPolicy=auto`.

Naming rules:
1. Keep base readable filename.
2. Append short hash suffix only for conflicting variants.
3. Deterministic mapping within a run.
4. If rare short-hash collision occurs, extend suffix length for that filename group.

## Two-Stage Auto-Resolution Option

### Motivation
Single-pass flattening can discover collisions late, after an earlier conflicting file has already been written with the plain name.  
This creates consistency problems if a later collision requires renaming.

### Memory Model
The two-stage approach does not require retaining full XML documents in memory.

Design intent:
1. Stream each resource once during planning.
2. Extract link metadata (`import`/`include` references) while streaming.
3. Compute content hash from streamed raw bytes.
4. Retain only compact metadata:
- resolved identity
- hash
- proposed/assigned filename
- reference edges and rewrite targets

This keeps memory proportional to resource count and graph metadata, not XML payload size.

### Performance Model
The planner should be optimized for large dependency graphs:
1. Cache resolver-origin fingerprint per resolved URI.
2. Avoid duplicate fetch/hash for already-seen resolved URIs.
3. Use Tier 1 origin comparison first.
4. Use Tier 2 byte hashing only for ambiguous or conflicting filename groups.

### Approach
`collisionPolicy=auto` performs flattening in two stages:

1. Stage 1: Analyze and Plan
- Crawl and resolve full dependency graph without writing final output files.
- Collect:
  - resolved identities
  - canonical source hashes
  - proposed default filenames
  - all references that must be rewritten
- Detect filename collision groups.
- Build final deterministic filename assignment:
  - keep plain filename for non-collision entries
  - assign hash-suffixed names for colliding entries

2. Stage 2: Write and Rewrite
- Execute writes using the finalized filename plan.
- Rewrite all references against the finalized mapping.
- No backtracking/retroactive rename during write phase.

### User Visibility
Before Stage 2, log a concise execution plan:
- total resources discovered
- number of collision groups
- each collision group and chosen output names
- whether any hash suffix expansion was required

## Resolver Contract Enhancement
Enhance resolver output from plain string URI to a structured result:
- `resolvedUri`
- `resolutionMethod` (for example entity/uri/public/fallback)
- `resolutionTrace` (best-effort diagnostic details)

The flattener uses this for:
- identity calculation
- collision diagnostics

## Backward Compatibility
1. No filename changes in default non-collision scenarios.
2. Existing behavior remains for unique files.
3. Collision handling becomes explicit and configurable.
4. `auto` mode is opt-in and does not alter default behavior.

## Test Plan

### Required New Collision Test
Add a dedicated integration-style flatten test that reproduces same-name/different-content collision using XML Catalog entries.

Catalog setup:
```xml
<catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog">
  <system systemId="urn:ns1:Order.xsd" uri="ns1/Order.xsd"/>
  <system systemId="urn:ns2:Order.xsd" uri="ns2/Order.xsd"/>
</catalog>
```

Fixture setup:
1. `ns1/Order.xsd` and `ns2/Order.xsd` must have different structures/content.
2. Two source schemas import:
  - `schemaLocation="urn:ns1:Order.xsd"`
  - `schemaLocation="urn:ns2:Order.xsd"`
3. Flatten target output directory is a single folder.

Expected assertions:
1. `collisionPolicy=warn`
- Warning emitted for `Order.xsd` collision.
- Build continues.

2. `collisionPolicy=fail`
- Build fails with clear collision details.

3. `collisionPolicy=rename`
- First `Order.xsd` remains.
- Conflicting file written as `Order__<shortHash>.xsd`.
- Rewritten import points to renamed file.

4. `collisionPolicy=auto`
- No files written during planning stage.
- Plan reports a collision group for `Order.xsd`.
- Final write uses consistent deterministic names for both variants (for example `Order__a1b2c3.xsd` and `Order__d4e5f6.xsd`).
- All rewritten references point to planned names.

### Additional Tests
1. Same filename + same content from two references should not warn/fail.
2. Deterministic naming across repeated runs.
3. Diagnostic message includes both resolved URIs.

## Implementation Outline
1. Introduce `CollisionRegistry` in flatten pipeline.
2. Add resolver result model (`ResolvedResource`).
3. Add collision policy config parsing in `FlattenImportPathMojo`.
4. Add planning model:
- `FlattenPlan` (resource graph + filename assignments + rewrite map)
- `FlattenPlanner` (stage 1)
- `FlattenWriter` (stage 2)
5. Add origin fingerprint model:
- `ResourceOrigin` (file/jar/artifact/path details)
- comparison helper for Tier 1 equivalence checks
6. Integrate two-tier comparison (origin first, hash on demand).
7. Integrate single-pass behavior for `warn|fail|rename`; two-stage behavior for `auto`.
8. Add tests for warn/fail/rename/auto policies and Tier 1/Tier 2 comparison paths.

## Observability
Log collision diagnostics with:
- target filename
- first and second resolved URI
- first and second content hash
- selected collision policy
- final output filename chosen
- planning summary (for `auto`)
