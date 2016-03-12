interface app {
  fname: string;
  text: string;
  title: string;
  description: string;
  url: string;
  hashelp: boolean;
}
interface menuEntry {
  title: string;
  portlets: app[];
}
interface DATA {
  person: { id: string };
  layout: { folders: menuEntry[] };
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
  header: string;
}

interface prolongation_ENT_appParams {
  quirks: {};
  current: string;
  currentAppIds: string[];
  div_id: string;
  div_is_uid: boolean;
  logout: boolean;
  login: boolean;
  is_logged: boolean | { fn: (find: any) => boolean };
  account_links: {};

  hide_menu: boolean;
  showSearch: boolean;
  no_titlebar: boolean;
  no_footer: boolean;

  onload(DATA: DATA, PARAMS: PARAMS, CONF: CONF): void;

  url: string;
  uid: string;
}

interface prolongation_ENT {
  appParams: prolongation_ENT_appParams;
  wanted_uid: string;
  localStorage_prefix: string;
  localStorage_js_text_field: string;
  loadTime: number;
  
  maybe_loaded: boolean;
  
  DATA: DATA;
  CONF: CONF;
  PARAMS: PARAMS;
  CSS: CSS;
  TEMPLATES: TEMPLATES;
  
  onAsyncLogout(): void;
  detectReload(time): void;
  main(DATA: DATA, PARAMS: PARAMS, notFromLocalStorage: boolean): string;
}

interface Window {
  cssToLoadIfInsideIframe: string;
  bandeau_ENT: prolongation_ENT_appParams;
  prolongation_ENT: prolongation_ENT;
}
