# BigTable Connector - Developer Documentation Index

Welcome to the BigTable connector developer documentation. This index helps you navigate all available developer resources.

## Documentation Structure

```
docs/developer/
├── INDEX.md                    # This file - navigation hub
├── README.md                   # Main developer guide and overview
├── ARCHITECTURE.md             # Deep dive into connector architecture
├── ADDING_ACTIONS.md           # Step-by-step guide for adding new actions
├── TESTING_GUIDE.md            # Comprehensive testing guide
└── CODE_PATTERNS.md            # Code patterns and conventions
```

---

## Quick Start

**New to the connector?** Start here:

1. **[README.md](README.md)** — Overview, setup, and quick reference
2. **[ARCHITECTURE.md](ARCHITECTURE.md)** — Understand the design
3. **[CODE_PATTERNS.md](CODE_PATTERNS.md)** — Learn coding conventions

**Want to contribute?**

1. **[ADDING_ACTIONS.md](ADDING_ACTIONS.md)** — Add a new action (step-by-step)
2. **[TESTING_GUIDE.md](TESTING_GUIDE.md)** — Write comprehensive tests
3. **[README.md#contributing](README.md#contributing)** — Contribution workflow

---

## Documentation by Topic

### Getting Started

| Document | Description | When to Read |
|----------|-------------|--------------|
| [README.md](README.md) | Developer guide overview | First time setup |
| [README.md#development-setup](README.md#development-setup) | Setup instructions | Before coding |
| [README.md#quick-reference](README.md#quick-reference) | Build commands | Reference |

### Understanding the Architecture

| Document | Description | When to Read |
|----------|-------------|--------------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Complete architecture guide | Understanding design |
| [ARCHITECTURE.md#action-discovery-mechanism](ARCHITECTURE.md#action-discovery-mechanism) | ServiceLoader flow | How actions are found |
| [ARCHITECTURE.md#yaml-execution-pipeline](ARCHITECTURE.md#yaml-execution-pipeline) | YAML to execution | How YAML is processed |
| [ARCHITECTURE.md#design-decisions](ARCHITECTURE.md#design-decisions) | Why we chose this design | Understanding tradeoffs |

### Adding Features

| Document | Description | When to Read |
|----------|-------------|--------------|
| [ADDING_ACTIONS.md](ADDING_ACTIONS.md) | Complete guide to adding actions | Adding new action |
| [ADDING_ACTIONS.md#complete-example](ADDING_ACTIONS.md#complete-example) | Minimal working example | Quick reference |
| [ADDING_ACTIONS.md#checklist](ADDING_ACTIONS.md#checklist) | Completion checklist | Before PR |

### Testing

| Document | Description | When to Read |
|----------|-------------|--------------|
| [TESTING_GUIDE.md](TESTING_GUIDE.md) | Complete testing guide | Writing tests |
| [TESTING_GUIDE.md#unit-testing](TESTING_GUIDE.md#unit-testing) | Unit test patterns | Testing individual classes |
| [TESTING_GUIDE.md#mocking-strategies](TESTING_GUIDE.md#mocking-strategies) | EasyMock usage | Mocking dependencies |
| [TESTING_GUIDE.md#best-practices](TESTING_GUIDE.md#best-practices) | Testing best practices | Writing quality tests |

### Code Standards

| Document | Description | When to Read |
|----------|-------------|--------------|
| [CODE_PATTERNS.md](CODE_PATTERNS.md) | Code patterns and conventions | Writing new code |
| [CODE_PATTERNS.md#action-patterns](CODE_PATTERNS.md#action-patterns) | Standard action template | Creating actions |
| [CODE_PATTERNS.md#validation-patterns](CODE_PATTERNS.md#validation-patterns) | Input validation | Adding validation |
| [CODE_PATTERNS.md#logging-patterns](CODE_PATTERNS.md#logging-patterns) | Logging standards | Adding logs |

---

## Common Tasks

### I want to...

#### ...understand how the connector works
1. Read [README.md](README.md) — Overview
2. Read [ARCHITECTURE.md](ARCHITECTURE.md) — Deep dive
3. Review existing actions (e.g., `CreateTable.java`)

#### ...add a new action
1. Read [ADDING_ACTIONS.md](ADDING_ACTIONS.md) — Step-by-step guide
2. Follow [CODE_PATTERNS.md#action-patterns](CODE_PATTERNS.md#action-patterns) — Template
3. Use [ADDING_ACTIONS.md#checklist](ADDING_ACTIONS.md#checklist) — Verify completion

#### ...write tests for my code
1. Read [TESTING_GUIDE.md](TESTING_GUIDE.md) — Testing overview
2. Follow [TESTING_GUIDE.md#unit-testing](TESTING_GUIDE.md#unit-testing) — Unit test patterns
3. Check [TESTING_GUIDE.md#coverage-goals](TESTING_GUIDE.md#coverage-goals) — Coverage targets

#### ...fix a bug
1. Review [CODE_PATTERNS.md](CODE_PATTERNS.md) — Understand patterns
2. Write test that reproduces bug (see [TESTING_GUIDE.md](TESTING_GUIDE.md))
3. Fix code following patterns
4. Verify all tests pass

#### ...understand validation logic
1. Read [CODE_PATTERNS.md#validation-patterns](CODE_PATTERNS.md#validation-patterns)
2. Review [ARCHITECTURE.md#error-handling](ARCHITECTURE.md#error-handling)
3. Check existing validation (e.g., `CreateTable.validate()`)

#### ...understand ServiceLoader mechanism
1. Read [ARCHITECTURE.md#action-discovery-mechanism](ARCHITECTURE.md#action-discovery-mechanism)
2. Review [ADDING_ACTIONS.md#step-5-register-with-serviceloader](ADDING_ACTIONS.md#step-5-register-with-serviceloader)
3. Check `META-INF/services/...ActionFactory` file

#### ...understand Jackson binding
1. Read [CODE_PATTERNS.md#jackson-binding](CODE_PATTERNS.md#jackson-binding)
2. Review [ARCHITECTURE.md#yaml-execution-pipeline](ARCHITECTURE.md#yaml-execution-pipeline)
3. Check existing action POJOs

#### ...contribute to the project
1. Read [README.md#contributing](README.md#contributing) — Workflow
2. Follow [ADDING_ACTIONS.md](ADDING_ACTIONS.md) — If adding action
3. Use [ADDING_ACTIONS.md#checklist](ADDING_ACTIONS.md#checklist) — Before PR

---

## Reference Materials

### Architecture

- **[Component Diagram](ARCHITECTURE.md#architecture-diagram)** — Visual overview
- **[Action Discovery Flow](ARCHITECTURE.md#action-discovery-mechanism)** — How actions are loaded
- **[YAML Execution Pipeline](ARCHITECTURE.md#yaml-execution-pipeline)** — YAML to execution
- **[Variable Substitution](ARCHITECTURE.md#variable-substitution)** — How variables work
- **[Admin Client Management](ARCHITECTURE.md#admin-client-management)** — Client lifecycle

### Code Templates

- **[Action Template](CODE_PATTERNS.md#standard-action-template)** — Standard action code
- **[Factory Template](CODE_PATTERNS.md#factory-template)** — Standard factory code
- **[Test Template](TESTING_GUIDE.md#unit-testing)** — Standard test code
- **[JavaDoc Template](CODE_PATTERNS.md#javadoc-template)** — Documentation format

### Best Practices

- **[Validation Patterns](CODE_PATTERNS.md#validation-patterns)** — Input validation
- **[Error Handling](CODE_PATTERNS.md#error-handling)** — Error patterns
- **[Logging Patterns](CODE_PATTERNS.md#logging-patterns)** — Logging standards
- **[Test Best Practices](TESTING_GUIDE.md#best-practices)** — Testing guidelines

---

## External Resources

### TotalSchema Core

- **[Main AGENTS.md](../../../../AGENTS.md)** — Core TotalSchema guide
- **[IoC Container](../../../../docs/developer/IOC_CONTAINER_ARCHITECTURE.md)** — Custom IoC design
- **[Connector Architecture](../../../../docs/developer/CONNECTOR_ARCHITECTURE.md)** — Connector framework

### User Documentation

- **[User Manual](../user_manual/README.md)** — Complete user guide
- **[Actions Reference](../user_manual/ACTIONS-REFERENCE.md)** — All actions documented
- **[Examples](../../examples/README.md)** — 40+ YAML examples

### Project Resources

- **[Main README](../../README.md)** — Connector overview
- **[AGENTS.md](../../AGENTS.md)** — AI agent guide
- **[Examples](../../examples/)** — Real-world usage examples

---

## Related Documentation

### BigTable Connector Docs

```
totalschema-connector-gcp-bigtable/
├── README.md                          # Main connector README
├── AGENTS.md                          # AI agent guide
├── docs/
│   ├── developer/                     # Developer docs (this directory)
│   │   ├── INDEX.md                   # This file
│   │   ├── README.md                  # Developer guide
│   │   ├── ARCHITECTURE.md            # Architecture deep dive
│   │   ├── ADDING_ACTIONS.md          # Action creation guide
│   │   ├── TESTING_GUIDE.md           # Testing guide
│   │   └── CODE_PATTERNS.md           # Code patterns
│   └── user_manual/                   # User documentation
│       ├── README.md                  # User manual
│       ├── ACTIONS-REFERENCE.md       # Action reference
│       ├── QUICK-START.md             # Quick start
│       └── FAQ.md                     # FAQ
└── examples/                          # YAML examples
    ├── README.md                      # Examples index
    ├── 01-simple-families.yml
    ├── 02-max-versions.yml
    └── ... (40+ examples)
```

### TotalSchema Core Docs

```
totalschema/
├── README.md                          # Project README
├── AGENTS.md                          # Core TotalSchema guide
└── docs/
    ├── JAVA-API-USAGE.md              # Java API
    ├── MAVEN-USAGE.md                 # Maven plugin
    └── developer/                     # Core developer docs
        ├── IOC_CONTAINER_ARCHITECTURE.md
        ├── CONNECTOR_ARCHITECTURE.md
        └── SCRIPT_EXECUTOR_SUBSYSTEM.md
```

---

## Help & Support

### Getting Help

- **GitHub Issues** — https://github.com/totalschema/totalschema/issues
- **GitHub Discussions** — https://github.com/totalschema/totalschema/discussions
- **Stack Overflow** — Tag: `totalschema`

### Reporting Issues

When reporting issues, include:
- Detailed description
- Steps to reproduce
- Expected vs actual behavior
- Environment (Java version, OS, etc.)
- Relevant code snippets
- Error messages and stack traces

### Contributing

See [README.md#contributing](README.md#contributing) for:
- Contribution workflow
- Code quality standards
- Commit message conventions
- Pull request guidelines

---

## Document Change Log

| Date | Document | Change |
|------|----------|--------|
| 2026-07-31 | All | Initial developer documentation |

---

**Last Updated:** July 31, 2026  
**Maintainers:** TotalSchema Development Team

**Quick Links:**
- [Main README](README.md)
- [Architecture](ARCHITECTURE.md)
- [Adding Actions](ADDING_ACTIONS.md)
- [Testing Guide](TESTING_GUIDE.md)
- [Code Patterns](CODE_PATTERNS.md)

