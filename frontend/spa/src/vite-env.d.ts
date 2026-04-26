/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_OIDC_AUTHORITY: string;
  readonly VITE_OIDC_CLIENT_ID: string;
  readonly VITE_OIDC_REDIRECT_PATH?: string;
  readonly VITE_OIDC_POST_LOGOUT_REDIRECT_PATH?: string;
  readonly VITE_OIDC_SCOPE?: string;
  readonly VITE_API_BASE_PATH?: string;
  readonly VITE_IDP_ADMIN_BASE_URL?: string;
  readonly VITE_IDP_ADMIN_USERS_PATH?: string;
  readonly VITE_IDP_ADMIN_REPORT_PATH?: string;
  readonly VITE_IDP_ADMIN_EVENTS_PATH?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
