# standard-ui-blank-astro-daisyui-ssr

Helm chart for the `@iqkv/standard-ui-blank-astro-daisyui-ssr` Astro SSR application.

The application uses the `@astrojs/node` adapter in standalone mode (`output: "server"`),
so it runs as a Node.js process — not Nginx. The container listens on port **4321**.

## Usage

```bash
# SIT
helm upgrade --install standard-ui-blank-astro-daisyui-ssr ./ \
  --values ./values.yaml \
  --values ./values-sit.yaml \
  --namespace iqkv-sit-env \
  --create-namespace

# UAT
helm upgrade --install standard-ui-blank-astro-daisyui-ssr ./ \
  --values ./values.yaml \
  --values ./values-uat.yaml \
  --namespace iqkv-uat-env \
  --create-namespace

# Production
helm upgrade --install standard-ui-blank-astro-daisyui-ssr ./ \
  --values ./values.yaml \
  --values ./values-prd.yaml \
  --namespace iqkv-prd-env \
  --create-namespace
```

## Key differences from foundation-ui-saas-landing-kit

|              | foundation-ui-saas-landing-kit | standard-ui-blank-astro-daisyui-ssr |
| ------------ | ------------------------------ | ----------------------------------- |
| Runtime      | Nginx (static)                 | Node.js SSR                         |
| Port         | 8080                           | 4321                                |
| Config       | nginx snippets via ConfigMap   | env vars via ConfigMap              |
| Astro output | static                         | server                              |

## Environment Variables

Runtime env vars are injected via ConfigMap. Set them under `app.env` in your values file:

```yaml
app:
  env:
    PUBLIC_SITE_URL: "https://example.com"
    RESEND_API_KEY: "re_..."
```

Secrets (API keys, tokens) should be managed separately via Kubernetes Secrets and
referenced as `secretKeyRef` in the deployment — not stored in values files.
