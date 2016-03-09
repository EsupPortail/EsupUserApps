interface Window {
  cssToLoadIfInsideIframe: string,
  bandeau_ENT_onAsyncLogout(): void,
  bandeau_ENT_detectReload(time): void,
  bandeau_ENT: {
    helpers: typeof h
    quirks: {},
    current: string,
    currentAppIds: string[],
    div_id: string,
    div_is_uid: boolean,
    logout: boolean,
    login: boolean,
    is_logged: boolean | { fn: (find: any) => boolean },
    account_links: {},
    forced_uid: boolean,

    hide_menu: boolean,
    showSearch: boolean,
    no_titlebar: boolean,
    no_footer: boolean,

    onload(DATA: any, PARAMS: any, CONF: any): void,

    url: string,

    uid: string,
    prevHash: string,
    localStorage_prefix: string,
    localStorage_js_text_field: string,
    js_text: string,
    loadTime: string,
    notFromLocalStorage: boolean,
  }
}
