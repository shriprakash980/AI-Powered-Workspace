# DevPilot Pipeline Configuration (`devpilot-ci.yml`)

## Schema Specification

```yaml
name: Java Application

triggers:
  push: true
  pull_request: true
  manual: true

environment:
  java: "21"

steps:
  - name: Build
    type: BUILD

  - name: Test
    type: TEST

  - name: Package
    type: PACKAGE

artifact:
  enabled: true

deployment:
  enabled: false
  environment: development
```

## Validation & Limits
- Max YAML file size: 64 KB
- Rejects dangerous injection patterns.
- Validates step types against permitted allowlist.
