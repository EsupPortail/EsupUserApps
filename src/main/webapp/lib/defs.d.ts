interface DATA {
  person: { id: string };
  apps: {};
  layout: {};
  realUserId: string;
  canImpersonate: string[];
}

interface PARAMS {
  hash: string;
  is_old: boolean;
}

interface CONF {
  bandeau_ENT_url: string;
  ent_logout_url: string;
  cas_impersonate: { cookie_name: string, cookie_domain: string };
  disableLocalStorage: boolean;
  time_before_checking_browser_cache_is_up_to_date: number;
}

interface CSS {
  base: string;
  desktop: string;
}

interface TEMPLATES {
  header string;
}

interface bandeau_ENT {
  quirks: {};
  current: string;
  currentAppIds: string[];
  div_id: string;
  div_is_uid: boolean;
  logout: boolean;
  login: boolean;
  is_logged: boolean | { fn: (find: any) => boolean };
  account_links: {};
  forced_uid: boolean;

  hide_menu: boolean;
  showSearch: boolean;
  no_titlebar: boolean;
  no_footer: boolean;

  onload(DATA: DATA, PARAMS: PARAMS, CONF: CONF): void;

  url: string;

  uid: string;
  localStorage_prefix: string;
  localStorage_js_text_field: string;
  loadTime: number;
  notFromLocalStorage: boolean;
  
  maybe_loaded: boolean;
  
  helpers: typeof h;
  main: (DATA: DATA, PARAMS: PARAMS) => string;
  DATA: DATA;
  CONF: CONF;
  PARAMS: PARAMS;
  CSS: CSS;
  TEMPLATES: TEMPLATES;
}

interface Window {
  cssToLoadIfInsideIframe: string;
  bandeau_ENT_onAsyncLogout(): void;
  bandeau_ENT_detectReload(time): void;
  bandeau_ENT: bandeau_ENT;
}
