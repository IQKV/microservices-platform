export default {
  extends: ["@commitlint/config-conventional"],
  rules: {
    "footer-max-line-length": [1, "always", 180],
    "type-enum": [2, "always", ["feat", "fix", "rfc", "docs", "style", "improvement", "enhancement", "refactor", "perf", "test", "chore", "build", "ci", "revert"]],
  },
};
