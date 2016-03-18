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
  user: string;
  userAttrs: { id: string };
  layout: { folders: menuEntry[] };
  realUserId: string;
  canImpersonate: string[];
}

interface PARAMS {
  hash: string;
  is_old: boolean;
}

interface CONF {
  theme: string;
  prolongationENT_url: string;
  uportal_base_url: string;
  cas_login_url: string;
  ent_logout_url: string;
  layout_url: string;
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
  footer: string;
}

interface prolongation_ENT_args {
  quirks: string[];
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

  onNotLogged(pE: prolongation_ENT);
  onload(pE: prolongation_ENT): void;

  url: string;
  uid: string;
}

interface prolongation_ENT {
  currentApp: app;
  wanted_uid: string;
  localStorage_prefix: string;
  localStorage_js_text_field: string;
  loadTime: number;
  width_xs: boolean;
  
  maybe_loaded: boolean;
  
  validApps: {};
  DATA: DATA;
  CONF: CONF;
  PARAMS: PARAMS;
  CSS: CSS;
  TEMPLATES: TEMPLATES;
  
  helpers: helpers;
  localStorageSet(field: string, value: string);
  localStorageGet(field: string): string;
  accountLink(text: string, link_spec: any): HTMLAnchorElement;
  callPlugins(event: string);
  plugins: plugin[];
  onAsyncLogout(): void;
  detectReload(time): void;
  main(DATA: DATA, PARAMS: PARAMS, notFromLocalStorage: boolean): string;
}

interface plugin {
  post_compute_currentApp();
  post_header_add();
}
interface helpers {
  mylog(string);
  head(): HTMLElement;
  now(): number;
  removeClass(elt: Element, classToToggle: string); 
  toggleClass(elt: Element, classToToggle: string); 
  insertAfter(e: Element, newNode: Element);
  simpleQuerySelectorAll(selector: string): Element[];
  simpleQuerySelector(selector: string): Element;
  getCookie(name: string): string;
  removeCookie(name: string, domain: string, path: string);
  simpleContains<T>(a: T[], val: T): boolean; 
  simpleEach<T>(a: T[], val: (e: T) => void);
  simpleEachObject(o, fn: (k: string, v, o?) => void); 
  simpleFilter<T>(a: T[], fn: (e: T) => boolean): T[]; 
  simpleMap<T,U>(a: T[], fn: (e: T) => U): U[];
  escapeQuotes(s: string): string;
  template(s: string, map: {}): string;
  onIdOrBody_rec(id: string, f: () => void);
  onIdOrBody(id: string, f: () => void);
  onReady_rec(f: () => void);
  onReady(f: () => void);
  set_div_innerHTML(div_id: string, content: string);
  loadCSS(url: string, media: string);
  unloadCSS(url: string);
  addCSS(css: string);
  loadScript(url: string);
}

interface Window {
  cssToLoadIfInsideIframe: string;
  prolongation_ENT: prolongation_ENT;
  prolongation_ENT_args: prolongation_ENT_args;
}
