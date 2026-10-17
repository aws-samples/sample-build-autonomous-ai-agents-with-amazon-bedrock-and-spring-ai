// Local development defaults. The container image regenerates this file at
// start-up from AUTONOMOUS_UI_URL, AUTONOMOUS_API_URL, IDP_UI_URL and IDP_API_URL.
window.LANDING_CONFIG = {
  autonomous: { ui: 'http://localhost:5173', api: 'http://localhost:8080' },
  idp: { ui: 'http://localhost:5174', api: 'http://localhost:8081' },
};
