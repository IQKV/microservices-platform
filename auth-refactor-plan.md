- **Hardcoded domains**:
  - `app.pynity.com/src/app/config/auth-config.ts` and `auth.pynity.com/src/app/config/auth-config.ts` default to iqkv.com domains. Needs alignment to pynity.com.
- **Redirect flow in App**:
  - `app.pynity.com/src/processes/auth/lib/navigation.ts` uses `window.location.href = ${config.domains.auth}?redirect=${currentUrl}`.
  - Uses `redirect` param (not `returnTo`), no allowlist validation.
- **Auth App routing**:
  - `auth.pynity.com/src/pages/login.tsx` uses `requireGuest()` guard but no explicit logic shown to redirect back to app when already authenticated.
- **Token storage**:
  - Both configs define `tokenStorage.accessTokenKey` and `refreshTokenKey`; App’s `logoutAndRedirect` clears localStorage tokens. This implies frontend-managed tokens, not a gateway cookie session.
- **Endpoints layout**:
  - Both configs expect `/api/v1/auth/*` endpoints, suggesting the Auth app talks to an API. It’s unclear if the gateway (api.pynity.com) is the single BFF or the auth service exposes public endpoints directly.

# Proposed target architecture

- **Gateway (api.pynity.com) as BFF**:
  - Terminate user sessions using secure, httpOnly cookies on `.pynity.com`.
  - Expose `POST /auth/login`, `POST /auth/signup`, `POST /auth/refresh`, `POST /auth/logout`, `GET /me`.
  - Proxy/aggregate downstream services. Use mTLS or signed service JWT for s2s auth.
- **Auth Service (internal)**:
  - Pure API and identity domain logic (password, email verify, reset, MFA).
  - Gateway calls it; users never call auth service directly. All Set-Cookie done at gateway for `.pynity.com`.
- **React Apps**:
  - App (app.pynity.com) performs `GET api.pynity.com/me` on load to decide redirects.
  - Auth (auth.pynity.com) is UI only; it submits to gateway endpoints. On success, gateway sets session cookies, then UI redirects to `returnTo`.

# Gaps/Risks to address

- **LocalStorage tokens**: Vulnerable to XSS, inconsistent with BFF model. Migrate to httpOnly cookie session at `.pynity.com`.
- **Redirect param**: Use `returnTo` with strict allowlist and same-site path validation to avoid open redirect.
- **Domain defaults**: Replace iqkv.com defaults with pynity.com, load from env at build/runtime.
- **CORS/CSRF**: With cookie-based auth, configure SameSite=Lax, CSRF tokens or double-submit for non-GET.
- **Auth UI calling patterns**: Ensure auth UI calls gateway only (not auth service) to receive cookies.
- **Observability + error contract**: Standardize problem+json errors and tracing across gateway and services.

# Detailed action plan

- **Gateway BFF (api.pynity.com)**
  - **Session model**
    - Implement gateway-managed session: short-lived access cookie + refresh cookie (httpOnly, Secure, SameSite=Lax, Domain=.pynity.com).
    - Rotate refresh tokens, detect reuse.
  - **Endpoints**
    - `POST /auth/login`, `POST /auth/signup`, `POST /auth/refresh`, `POST /auth/logout`, `GET /me`.
    - Normalize to problem+json error shape.
  - **Proxying**
    - `/api/*` proxies to services with s2s auth (mTLS or signed JWT). Propagate user identity via `X-User-Id` or JWT claims.
  - **Security**
    - CSRF: set CSRF cookie and require header on state-changing requests.
    - CORS: only allow `https://app.pynity.com` and `https://auth.pynity.com` where needed; prefer same-site requests from Auth UI to gateway.
    - Headers: HSTS, CSP (nonce/strict-dynamic), Referrer-Policy, Permissions-Policy.

- **Auth Service**
  - Keep internal endpoints: login, signup, forgot/reset password, email verify/resend, optional MFA.
  - Rate-limiting + bot protection for login/signup/forgot.
  - Emit events for audit and analytics.
  - No cookies set; return tokens only to gateway.

- **App (app.pynity.com)**
  - **Config**
    - Switch defaults to `auth.pynity.com` and `app.pynity.com`.
  - **Auth guard**
    - On app bootstrap (router loader or top-level effect), call `GET https://api.pynity.com/me` with credentials; if 401, redirect to `https://auth.pynity.com/login?returnTo=<encoded current URL>`.
  - **Logout**
    - Call `POST https://api.pynity.com/auth/logout` with credentials, then hard redirect to `https://auth.pynity.com/login`.
  - **Remove localStorage tokens**
    - Stop reading/writing access/refresh tokens in the app. Rely on cookies.

- **Auth (auth.pynity.com)**
  - **Config**
    - Update domain defaults to pynity.com. Use `returnTo` parameter consistently.
  - **Guest/Authed guards**
    - On mount, call `GET https://api.pynity.com/me` with credentials; if authenticated, redirect to `returnTo` if present (allowlisted), otherwise `https://app.pynity.com/`.
  - **Flows**
    - All form submissions to `https://api.pynity.com/auth/*` with `credentials: include`. If success, gateway sets cookies; then redirect to `returnTo`.
  - **ReturnTo validation**
    - Only allow returnTo URLs within `https://app.pynity.com` (and optionally `https://auth.pynity.com` specific paths). Fall back to `/`.

- **Shared FE SDK (@gripday/auth-client)**
  - Functions: `getSession`, `login`, `signup`, `logout`, `requireAuthGuard`, `redirectToAuth`, `resolveReturnTo`.
  - Centralize environment config and domain endpoints.
  - Types for user/session.

- **Dev/Local**
  - mkcert or local proxy for HTTPS subdomains: `app.local.pynity.com`, `auth.local.pynity.com`, `api.local.pynity.com` with hosts entries.
  - Docker compose for gateway+auth; seeded test user.
  - Playwright E2E to verify redirect/guard flows, refresh, logout, returnTo allowlist.

- **CI/CD**
  - Secrets in vault; per-environment domain config.
  - Preview deploys with temporary subdomains and smoke tests.
  - SAST/DAST on auth endpoints and CSP reports.

# Concrete code changes to schedule

- **Replace iqkv.com defaults**
  - In both `auth-config.ts` files, set defaults to:
    - `DEFAULT_AUTH_DOMAIN = "https://auth.pynity.com"`
    - `DEFAULT_APP_DOMAIN = "https://app.pynity.com"`
- **Rename and standardize redirect param**
  - In `app.pynity.com/src/processes/auth/lib/navigation.ts`: switch to `?returnTo=`.
  - Add allowlist validation utility in shared SDK. Ensure Auth app reads `returnTo`.
- **Remove localStorage token usage**
  - Update `logoutAndRedirect` to call gateway `/auth/logout` and stop clearing token keys (eventually remove `tokenStorage` from config).
- **Network calls**
  - Ensure all fetch/XHR include `credentials: 'include'` when calling `api.pynity.com`.
- **Add `/me` checks**
  - App: route loader or top-level effect to call `/me`.
  - Auth: top-level effect to redirect if already authenticated.

# Acceptance criteria

- Visiting `app.pynity.com/protected` unauthenticated redirects to `auth.pynity.com/login?returnTo=https%3A%2F%2Fapp.pynity.com%2Fprotected`.
- Successful login sets gateway cookies and redirects back to `app.pynity.com/protected`.
- Visiting `auth.pynity.com/login` while authenticated immediately redirects to the returnTo/app root.
- `logout` clears cookies at `.pynity.com` and redirects to `auth.pynity.com/login`.
- LocalStorage contains no access/refresh tokens.
- E2E suite passes for the above scenarios.

# Next steps I can take now

- **Verify presence of gateway and auth service code** and map endpoints to confirm where to implement cookie session and `/me`.
- **Patch both React configs** to use gripday domains and `returnTo`, and refactor App/Auth guards accordingly.

Would you like me to:
- Audit the repo further to locate the gateway/auth service code and current routes?
- Start implementing the React-side changes (config defaults, returnTo, guards) behind env flags to avoid breaking current flows?

Summary: Produced a concrete, security-focused plan to move to a proper BFF with cookie sessions, standardized redirect/guard behavior across apps, and actionable code changes to align both React apps and backend gateway/auth services with app.pynity.com, auth.pynity.com, and api.pynity.com. The discovery task remains in progress; ready to proceed with audits or React updates.