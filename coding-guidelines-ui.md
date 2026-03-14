# UI Coding Guidelines

> Consolidated reference for building UI code across `app.iqscaffold.com` and `auth.iqscaffold.com`.
> Both projects share identical conventions. Follow this document as the single source of truth.

---

## Tech Stack

| Category          | Library / Tool             | Version            |
| ----------------- | -------------------------- | ------------------ |
| Language          | TypeScript                 | ~5.9.3             |
| Runtime           | React                      | ^19.2.4            |
| Build             | Vite + SWC                 | ^7.3.1 / ^4.3.0    |
| Package manager   | pnpm                       | 10.32.1            |
| UI library        | Mantine                    | ^8.3.16            |
| Routing           | TanStack Router            | ^1.166.7           |
| Data fetching     | TanStack Query             | ^5.90.21           |
| State management  | Zustand + Immer            | ^5.0.11 / ^11.1.4  |
| Forms             | React Hook Form + Zod      | ^7.71.2 / ^3.25.76 |
| i18n              | LinguiJS                   | ^5.9.2             |
| Icons             | @tabler/icons-react        | ^3.40.0            |
| HTTP client       | Axios                      | ^1.13.6            |
| Date handling     | dayjs                      | ^1.11.19           |
| Pattern matching  | ts-pattern                 | ^5.9.0             |
| Drag & drop       | @dnd-kit                   | ^6.3.1             |
| Charts            | Recharts + @mantine/charts | ^3.8.0             |
| Rich text         | TipTap                     | ^3.20.1            |
| Flow diagrams     | @xyflow/react              | ^12.10.1           |
| Payments          | @stripe/react-stripe-js    | ^4.0.2             |
| Animations        | lottie-web / react-lottie  | ^5.13.0            |
| URL state         | nuqs                       | ^2.8.9             |
| Collections       | collect.js                 | ^4.36.1            |
| HTML sanitization | sanitize-html              | ^2.17.1            |
| JWT               | jwt-decode                 | ^4.0.0             |
| Cookies           | js-cookie                  | ^3.0.5             |
| Linter            | oxlint (type-aware)        | ^1.55.0            |
| Formatter         | oxfmt                      | ^0.37.0            |
| CSS linter        | Stylelint                  | ^17.4.0            |
| Dead code         | Knip                       | 5.86.0             |
| Unit tests        | Vitest                     | 4.0.18             |
| Component tests   | @testing-library/react     | ^16.3.2            |
| API mocking       | MSW                        | ^2.12.10           |
| E2E tests         | Playwright                 | 1.58.2             |
| Git hooks         | Husky + lint-staged        | ^9.1.7             |
| Releases          | release-it                 | ^19.2.4            |
| Task runner       | Nx                         | ^22.5.4            |

---

## Project Architecture — Feature-Sliced Design (FSD)

Both projects strictly follow [Feature-Sliced Design](https://feature-sliced.design/). The architecture is enforced by `src/architecture.test.ts` — a Vitest test that runs on every CI build.

### Layer Order (top → bottom, strict dependency direction)

```
src/
├── app/          # App bootstrap, providers, router, theme
├── pages/        # Route-level page components (file-based routing)
├── widgets/      # Composite UI blocks composed from features/entities
├── features/     # User-facing interactions (actions, forms, toggles)
├── entities/     # Business domain objects (models, cards, selectors)
├── processes/    # Cross-cutting flows (auth, tenant, feature flags)
└── shared/       # Reusable primitives with no business logic
    ├── api/      # Base HTTP client, interceptors
    ├── lib/      # Utilities, hooks, contexts, design tokens
    ├── ui/       # Generic UI components
    └── types/    # Shared TypeScript types
```

### Dependency Rules

- A layer may only import from layers **below** it in the list above.
- `shared` has no internal layer dependencies.
- `app` is the only layer that wires everything together.
- Cross-layer imports must go through the **public API** (`index.ts`) — never import internal files directly.

### Public API (barrel exports)

Every slice (feature, widget, entity, process) and every `shared` segment **must** expose an `index.ts`:

```
features/
└── user-profile/
    ├── ui/
    │   └── user-profile-card.tsx
    ├── model/
    │   └── user-profile.store.ts
    └── index.ts   ← public API, re-exports only what consumers need
```

This is enforced by `architecture.test.ts`. A missing `index.ts` will fail CI.

### Segment Conventions Inside a Slice

| Segment  | Purpose                            |
| -------- | ---------------------------------- |
| `ui/`    | React components for this slice    |
| `model/` | State, stores, business logic      |
| `api/`   | API calls specific to this slice   |
| `lib/`   | Helpers, hooks local to this slice |
| `types/` | TypeScript types for this slice    |

---

## File & Folder Naming

| Context                           | Convention                     | Example                                |
| --------------------------------- | ------------------------------ | -------------------------------------- |
| Pages (`.tsx` files)              | kebab-case                     | `user-settings.tsx`, `billing.$id.tsx` |
| `shared/ui` component folders     | kebab-case                     | `shared/ui/date-picker/`               |
| Feature / widget / entity folders | kebab-case                     | `features/user-profile/`               |
| Route params                      | `$param` prefix                | `$userId.tsx`                          |
| Generated files                   | `.gen.ts` suffix               | `routeTree.gen.ts`                     |
| Test files                        | `.test.ts(x)` or `.spec.ts(x)` | `user-profile.test.tsx`                |

Rules enforced by `architecture.test.ts`:

- Pages must match `/^[a-z0-9-_$\.]+$/`
- `shared/ui` folders must match `/^[a-z][a-z0-9-]*$/`

---

## TypeScript Configuration

File: `tsconfig.app.json`

```jsonc
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler", // Vite bundler mode
    "jsx": "react-jsx",
    "jsxImportSource": "react",
    "strict": true, // All strict checks enabled
    "isolatedModules": true,
    "moduleDetection": "force",
    "noFallthroughCasesInSwitch": true,
    "noUncheckedSideEffectImports": true,
    "allowImportingTsExtensions": true,
    "noEmit": true,
    "paths": {
      "@/*": ["./src/*"], // Always use @/ alias, never relative ../../
    },
  },
}
```

Key rules:

- Always use `@/` path alias instead of relative `../../` imports.
- `strict: true` — no implicit `any`, no implicit `this`, strict null checks.
- `@total-typescript/ts-reset` is included in `setupTests.ts` for better built-in type defaults.
- Run `pnpm type-check` to validate without building.

---

## Code Formatting

Formatter: **oxfmt** (Oxc formatter). Config is inherited from `.prettierrc` for compatibility.

File: `.prettierrc`

```yaml
endOfLine: lf
trailingComma: es5
tabWidth: 2
semi: true
singleQuote: false
```

File: `.editorconfig`

```ini
[*]
end_of_line = lf
charset = utf-8
trim_trailing_whitespace = true
insert_final_newline = true
indent_style = space
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

Rules:

- LF line endings everywhere (no CRLF).
- 2-space indentation, no tabs.
- Double quotes for strings.
- Semicolons required.
- Trailing commas in ES5 positions (objects, arrays, function params).
- Every file ends with a newline.
- No trailing whitespace (except `.md` files).

Commands:

```bash
pnpm formatter:check   # Check formatting
pnpm formatter:write   # Auto-fix formatting
```

`lint-staged` runs `formatter:check` on every staged file before commit.

---

## Linting

### JavaScript / TypeScript — oxlint

```bash
pnpm lint           # Type-aware lint check
pnpm lint:fix       # Auto-fix + format
```

- Uses `oxlint --type-aware` with `oxlint-tsgolint` plugin.
- Config: `.oxlintrc.json` (extends defaults, jsx-a11y and React settings configured).
- Runs on CI via `pnpm ci:lint`.

### CSS / SCSS — Stylelint

```bash
pnpm lint:stylelint  # Lint all *.css files
```

Config: `.stylelintrc.json` extends `stylelint-config-standard-scss`.

Disabled rules (intentionally relaxed for Mantine compatibility):

- `custom-property-pattern`, `selector-class-pattern` — allow Mantine's naming
- `color-function-notation`, `alpha-value-notation` — allow legacy notation
- `property-no-vendor-prefix` — allow vendor prefixes
- `selector-pseudo-class-no-unknown` — `:global` is allowed

### Dead Code — Knip

```bash
pnpm knip   # Find unused exports, files, dependencies
```

Run periodically to keep the codebase clean.

---

## Routing — TanStack Router (File-Based)

Routes are defined as files inside `src/pages/`. TanStack Router auto-generates `src/routeTree.gen.ts` — **never edit this file manually**.

Config: `tsr.config.json`

```
src/pages/
├── __root.tsx          # Root layout (wraps all routes)
├── index.tsx           # / route
├── dashboard.tsx       # /dashboard
├── users/
│   ├── index.tsx       # /users
│   └── $userId.tsx     # /users/:userId (dynamic param)
└── settings.tsx        # /settings
```

Router is created in `src/app/app.tsx` and receives `queryClient` as context:

```ts
const router = createRouter({
  routeTree,
  context: { queryClient },
  defaultPreload: "intent",
  defaultPreloadStaleTime: 0,
});
```

Rules:

- Register the router type for full type safety: `declare module "@tanstack/react-router" { interface Register { router: typeof router } }`
- Use `@tanstack/zod-adapter` for type-safe search params validation.
- Use `nuqs` for URL search param state that needs to be synced with React state.
- Route files use kebab-case, dynamic segments use `$param` prefix.

---

## Data Fetching — TanStack Query

```ts
// Query
const { data, isLoading } = useQuery({
  queryKey: ["users", userId],
  queryFn: () => api.getUser(userId),
});

// Mutation
const { mutate } = useMutation({
  mutationFn: (data) => api.updateUser(data),
  onSuccess: () => queryClient.invalidateQueries({ queryKey: ["users"] }),
});
```

Rules:

- `queryClient` is defined in `src/shared/lib` and passed to both the router context and `QueryClientProvider`.
- Query keys should be arrays, starting with the resource name.
- Invalidate queries after mutations — don't manually update cache unless performance requires it.
- Use `@tanstack/react-query-devtools` in development (already wired in `app.tsx`).
- Loader data in routes can use `queryClient.ensureQueryData()` for prefetching.

---

## State Management — Zustand + Immer

```ts
import { create } from "zustand";
import { immer } from "zustand/middleware/immer";

interface UserStore {
  user: User | null;
  setUser: (user: User) => void;
}

export const useUserStore = create<UserStore>()(
  immer((set) => ({
    user: null,
    setUser: (user) =>
      set((state) => {
        state.user = user; // Immer allows direct mutation
      }),
  })),
);
```

Rules:

- Always use `immer` middleware — enables direct state mutation syntax.
- Stores live in the `model/` segment of their slice.
- Keep stores small and slice-scoped. Avoid a single global store.
- Server state (API data) belongs in TanStack Query, not Zustand.
- Zustand is for client-only UI state (modals open, selected items, wizard steps, etc.).

---

## Forms — React Hook Form + Zod

```ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";

const schema = z.object({
  email: z.string().email(),
  name: z.string().min(2),
});

type FormValues = z.infer<typeof schema>;

export function UserForm() {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", name: "" },
  });

  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      {/* ... */}
    </form>
  );
}
```

For Mantine form components, use `mantine-form-zod-resolver`:

```ts
import { useForm } from "@mantine/form";
import { zodResolver } from "mantine-form-zod-resolver";

const form = useForm({
  validate: zodResolver(schema),
  initialValues: { email: "", name: "" },
});
```

Rules:

- Always define a Zod schema first — derive the TypeScript type from it with `z.infer<typeof schema>`.
- Use `@hookform/resolvers/zod` for React Hook Form, `mantine-form-zod-resolver` for Mantine forms.
- Schemas live in the `model/` segment of the feature.
- Reuse schemas for API request/response validation where possible.

---

## UI Components — Mantine v8

Mantine is the primary component library. Do not introduce other component libraries.

### Theme & Design Tokens

Design tokens are defined in `src/shared/lib/design-tokens` and mapped to the Mantine theme in `src/app/theme.ts`:

```ts
// src/app/theme.ts
export const theme = createTheme({
  colors: { primary, secondary },
  primaryColor: "primary",
  defaultRadius: borderRadius.md,
  fontFamily: typography.fontFamily.base,
  spacing: { xs, sm, md, lg, xl },
  shadows: { xs, sm, md, lg, xl },
  other: { designTokens }, // Access raw tokens via theme.other.designTokens
});
```

Rules:

- Always use design tokens from `src/shared/lib/design-tokens` — never hardcode colors, spacing, or font sizes.
- Use `theme.other.designTokens` for values not covered by Mantine's theme API.
- `defaultColorScheme="auto"` — the app respects the OS color scheme by default.
- Import Mantine styles in `app.tsx`: `@mantine/core/styles.css`, `@mantine/notifications/styles.css`.

### CSS / SCSS

PostCSS is configured with `postcss-preset-mantine` and `postcss-simple-vars`.

```scss
// Use Mantine CSS variables
.my-component {
  color: var(--mantine-color-primary-6);
  padding: var(--mantine-spacing-md);
  border-radius: var(--mantine-radius-md);
}
```

Rules:

- Prefer Mantine's built-in `style` prop and `className` with CSS Modules over global styles.
- Use SCSS only when CSS Modules are insufficient.
- CSS custom properties follow Mantine's `--mantine-*` naming convention.
- Stylelint enforces `stylelint-config-standard-scss`.

### Modals

Modals are registered globally in `ModalsProvider`:

```ts
// Register in app.tsx
<ModalsProvider modals={{ confirmation: ConfirmContextModal }}>

// Open from anywhere
import { modals } from "@mantine/modals";
modals.openContextModal({ modal: "confirmation", innerProps: { ... } });
```

### Notifications

```ts
import { notifications } from "@mantine/notifications";
notifications.show({ title: "Success", message: "Saved", color: "green" });
```

---

## Internationalization — LinguiJS v5

Config: `lingui.config.ts`

- Source locale: `en`
- Supported locales: `en`, `ru` (app) / `en`, `ru`, `it` (auth)
- Format: PO files in `locales/{locale}/`
- Fallback: `en`

### Usage

```tsx
import { Trans, useLingui } from "@lingui/react/macro";
import { msg } from "@lingui/macro";

// JSX translation
function MyComponent() {
  return <Trans>Hello, world</Trans>;
}

// Imperative translation
function useMyHook() {
  const { _ } = useLingui();
  const label = _(msg`Save changes`);
}
```

### Workflow

```bash
pnpm messages:extract   # Extract strings from source into .po files
pnpm messages:compile   # Compile .po files into TypeScript catalogs
```

The build script runs both automatically: `tsc -b && pnpm messages:extract && pnpm messages:compile && vite build`.

Rules:

- All user-visible strings must be wrapped in `<Trans>` or `_(msg\`...\`)`.
- Never hardcode UI strings — always use LinguiJS macros.
- Locale is initialized in `app.tsx` via `initializeLocale()` with priority: backend preference > localStorage > browser locale.

---

## Provider Composition

The full provider stack in `src/app/app.tsx` (outermost → innermost):

```
StrictMode
└── HelmetProvider          (@dr.pogodin/react-helmet — SEO meta tags)
    └── I18nProvider        (LinguiJS — i18n context)
        └── ErrorBoundary   (shared/ui — catches render errors)
            └── MantineProvider (theme, defaultColorScheme="auto")
                └── ModalsProvider (registered modal components)
                    └── Notifications (toast notifications)
                        └── QueryClientProvider (TanStack Query)
                            └── TenantProvider (multi-tenancy)
                                └── AuthProvider (authentication state)
                                    └── AuthGuardWrapper (route protection)
                                        └── FeatureProvider (feature flags, autoFetch)
                                            └── RouterProvider (TanStack Router)
                                                ├── ReactQueryDevtools
                                                └── MSWDevTools
```

Rules:

- Do not add new top-level providers without discussion — provider order matters.
- Auth-dependent providers must be inside `AuthProvider`.
- Feature flags (`FeatureProvider`) are inside auth so they can fetch user-specific flags.

---

## Testing

### Unit & Component Tests — Vitest + Testing Library

Config: `vitest.config.ts`

```ts
test: {
  globals: true,
  environment: "jsdom",
  setupFiles: "./src/setupTests.ts",
  testTimeout: 15000,
  include: ["./src/**/*.{test,spec}.{ts,tsx}"],
  exclude: ["./e2e/**", "*.gen.ts", "index.ts", "locales/**", "pages/**"],
}
```

```bash
pnpm test              # Run all unit tests (single run, no watch)
pnpm test:arch         # Run architecture enforcement tests only
pnpm test:coverage     # Run with v8 coverage (text + html + lcov)
pnpm test:ui           # Open Vitest UI
```

Coverage excludes: config files, generated files, barrel `index.ts` files, locale files, page files.

### API Mocking — MSW v2

MSW worker is in `public/mockServiceWorker.js`. Handlers live in `src/shared/mocks/`.

```ts
// src/shared/mocks/handlers.ts
import { http, HttpResponse } from "msw";

export const handlers = [http.get("/api/users", () => HttpResponse.json([{ id: 1, name: "Alice" }]))];
```

MSW is started in `app.tsx` via `startMSW()` — only active in development when enabled.

### E2E Tests — Playwright

```bash
pnpm e2e               # Run all E2E tests
pnpm e2e:chrome        # Chromium only
pnpm e2e:smoke         # Smoke tests on Chromium
pnpm e2e:debug         # Debug mode (PWDEBUG=1)
pnpm e2e:ui            # Playwright UI mode
pnpm playwright:install # Install browsers + deps
```

E2E tests live in `e2e/` and are excluded from Vitest.

### Architecture Tests

`src/architecture.test.ts` enforces FSD rules at CI time:

- All required layers exist
- All slices have `index.ts`
- File naming conventions are followed

Run with: `pnpm test:arch`

---

## Git Workflow

### Branch Naming

Enforced by `.husky/pre-commit`. Branch name must match:

```
^(feature|rfc|poc|bugfix|improvement|enhancement|library|prerelease|hotfix)\/[a-z0-9._-]+$
|^(wip|poc|\d+\.\d+\.x)$
```

Examples:

- `feature/user-profile-page`
- `bugfix/login-redirect`
- `enhancement/table-sorting`
- `hotfix/payment-crash`
- `wip` (temporary work-in-progress)

Direct commits to `main`, `master`, `dev`, `develop` are **blocked** by the pre-commit hook.

### Commit Message Convention

Enforced by commitlint + `.husky/commit-msg`.

Format: `type(scope): description`

Allowed types:

| Type          | Use for                              |
| ------------- | ------------------------------------ |
| `feat`        | New feature                          |
| `fix`         | Bug fix                              |
| `rfc`         | Request for comments / experimental  |
| `docs`        | Documentation only                   |
| `style`       | Formatting, no logic change          |
| `improvement` | General improvement                  |
| `enhancement` | Enhancement to existing feature      |
| `refactor`    | Code restructure, no behavior change |
| `perf`        | Performance improvement              |
| `test`        | Adding or fixing tests               |
| `chore`       | Tooling, config, dependencies        |
| `build`       | Build system changes                 |
| `ci`          | CI/CD changes                        |
| `revert`      | Revert a previous commit             |

Examples:

```
feat(auth): add OAuth2 login flow
fix(dashboard): correct chart data aggregation
chore(deps): upgrade Mantine to 8.3.16
```

### Pre-commit Hooks (lint-staged)

On every commit:

- `oxfmt --check` runs on all staged files
- `sort-package-json` runs on staged `package.json`

### Commit Helper

Use `gitzy` or `cz-conventional-changelog` for interactive commit message creation:

```bash
npx gitzy
```

---

## Build & CI

### Build

```bash
pnpm build        # tsc + lingui extract/compile + vite build
pnpm preview      # Preview production build
pnpm type-check   # TypeScript check without emitting
pnpm cleanup      # Remove dist, .tanstack, coverage
```

### Vite Plugins (production)

| Plugin                        | Purpose                                |
| ----------------------------- | -------------------------------------- |
| `@vitejs/plugin-react-swc`    | React + SWC compiler (fast transforms) |
| `@tanstack/router-plugin`     | Auto-generates `routeTree.gen.ts`      |
| `@lingui/vite-plugin`         | Compiles LinguiJS catalogs             |
| `vite-tsconfig-paths`         | Resolves `@/` path alias               |
| `vite-plugin-remove-console`  | Strips `console.*` in production       |
| `vite-plugin-image-optimizer` | Optimizes images at build time         |
| `vite-bundle-visualizer`      | Bundle size analysis                   |

### CI Pipeline (GitHub Actions)

Workflows:

- **Build check** — `pnpm ci` (lint + test) on every PR
- **Commit message check** — validates commit message format
- **PR title check** — validates PR title follows conventional commits
- **Dependabot auto-approve** — auto-approves dependency update PRs

```bash
pnpm ci           # Full CI check: lint + test
pnpm ci:lint      # Lint only
pnpm ci:test      # Test only
```

### Release

```bash
pnpm release      # release-it --ci (bumps version, generates changelog, tags)
```

Uses `@release-it/conventional-changelog` to auto-generate `CHANGELOG.md` from commit history.

### SonarQube

`sonar-project.properties` is present — coverage reports (lcov) are uploaded to SonarQube after CI runs.

---

## HTTP Client — Axios

Base client and interceptors live in `src/shared/api/`.

```ts
// src/shared/api/client.ts
import axios from "axios";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
});

// Add auth interceptor, error handling, etc.
apiClient.interceptors.request.use(/* ... */);
apiClient.interceptors.response.use(/* ... */);
```

Rules:

- Never import `axios` directly in features/entities — always use the shared `apiClient`.
- API functions for a slice live in that slice's `api/` segment.
- Export them through the slice's `index.ts`.

---

## Environment Variables

Vite exposes env vars prefixed with `VITE_`:

```ts
import.meta.env.VITE_API_URL;
import.meta.env.VITE_STRIPE_KEY;
```

- `.env` files are gitignored — use `.env.example` to document required variables.
- Never access `process.env` in browser code — use `import.meta.env`.

---

## General Clean Code Rules

### Imports

```ts
// Good — use @/ alias
import { useUserStore } from "@/features/user-profile";
import { Button } from "@/shared/ui";

// Bad — relative traversal
import { useUserStore } from "../../features/user-profile";
```

Import order (enforced by oxlint):

1. Node built-ins
2. External packages
3. Internal `@/` imports
4. Relative imports

### Pattern Matching

Use `ts-pattern` instead of long `if/else` or `switch` chains:

```ts
import { match } from "ts-pattern";

const label = match(status)
  .with("active", () => "Active")
  .with("inactive", () => "Inactive")
  .with("pending", () => "Pending")
  .exhaustive();
```

### HTML Sanitization

Always sanitize user-generated HTML before rendering:

```ts
import sanitizeHtml from "sanitize-html";

const safe = sanitizeHtml(userInput, { allowedTags: ["b", "i", "em"] });
```

Never use `dangerouslySetInnerHTML` with unsanitized content.

### Collections

Use `collect.js` for complex array/object transformations instead of chained lodash:

```ts
import collect from "collect.js";

const result = collect(users).where("active", true).sortBy("name").pluck("email").all();
```

### Drag & Drop

Use `@dnd-kit` — it's the project standard. Do not introduce `react-beautiful-dnd` or similar.

### Animations

Use `lottie-web` / `react-lottie` for Lottie animations. Keep animation files in `shared/ui/` or the relevant feature's `ui/` segment.

---

## DevContainer

Both projects include `.devcontainer/` configuration for consistent development environments. Use it when onboarding or when local setup is complex.

---

## Quick Reference — Common Commands

```bash
# Development
pnpm dev                  # Start dev server

# Code quality
pnpm lint                 # Lint (type-aware)
pnpm lint:fix             # Lint + auto-fix + format
pnpm formatter:check      # Check formatting
pnpm formatter:write      # Auto-format
pnpm type-check           # TypeScript check
pnpm knip                 # Find dead code

# i18n
pnpm messages:extract     # Extract translation strings
pnpm messages:compile     # Compile .po → TypeScript

# Testing
pnpm test                 # Unit tests (single run)
pnpm test:arch            # Architecture tests
pnpm test:coverage        # With coverage report
pnpm e2e                  # E2E tests
pnpm e2e:smoke            # Smoke tests only

# Build
pnpm build                # Production build
pnpm preview              # Preview production build
pnpm cleanup              # Clean build artifacts

# Release
pnpm release              # Bump version + changelog + tag
```
