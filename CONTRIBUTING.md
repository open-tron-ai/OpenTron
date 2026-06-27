# Contributing to OpenTron

Thank you for your interest in contributing to OpenTron! This guide covers everything you need to know — from why to contribute, to how to submit your first pull request.

---

## Why Contribute?

Contributing to OpenTron isn't just about code — it's about building the future of on-device AI together. Here's what you get:

### Paper Acknowledgment

All contributors with merged pull requests will be acknowledged as contributors on the OpenTron paper release.

### Mac Mini Giveaway

We're giving away a Mac Mini to one lucky contributor! Install OpenTron on your personal machine and opt in via the desktop app to share anonymized savings data (FLOPs, dollar cost, energy) for a chance to win. Your data is fully anonymous — no IP, no hardware info beyond savings metrics. You must share your email via the desktop app to be eligible.

See the [Savings Leaderboard](https://open-Tron.github.io/OpenTron/leaderboard/) for details.

### Path to Maintainership

Consistent contributors can grow into project maintainers:

- **Contributor** — anyone with a merged PR
- **Reviewer** — invited after 3+ merged PRs in a domain area, can review PRs
- **Maintainer** — reviewers who demonstrate sustained engagement and good judgment

### Recognition

Contributors are recognized in release notes and on our GitHub repository.

---

## Ways to Contribute

### Good First Contributions

These are great starting points for new contributors:

- Documentation improvements and typo fixes
- Bug reports with reproducible steps
- New eval datasets and scorers
- Test coverage improvements

Look for issues labeled [`good-first-issue`](https://github.com/open-Tron/OpenTron/labels/good-first-issue).

### Ideal Contributions

- Bug fixes with tests
- Performance improvements
- New tools, engines, or agents following the [registry pattern](docs/development/contributing.md#registry-pattern)
- New channel integrations (Telegram, Discord, Slack, etc.)

### Harder to Review

These require more context and review time. **Please open an issue for discussion before starting a PR:**

- New primitives or major extensions to existing ones
- Large refactors
- Changes to core abstractions (`BaseAgent`, `InferenceEngine`, etc.)

### May Not Be Accepted

To avoid wasted effort, note that PRs in these categories are unlikely to be merged:

- Changes that break backwards compatibility in the public API
- Changes that add significant new dependencies without justification
- Changes that add friction to the user experience

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you agree to uphold this code.

---

## Questions?

- Open a [Discussion](https://github.com/open-Tron/OpenTron/discussions) for questions and help
- Check the [documentation](https://open-Tron.github.io/OpenTron/) for guides and API reference

