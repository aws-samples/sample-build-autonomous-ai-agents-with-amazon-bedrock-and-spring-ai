// Wires the landing page to the endpoints in window.LANDING_CONFIG (config.js)
// and shows a reachability badge for each UI and backend.
(function () {
  'use strict';

  var defaults = {
    autonomous: { ui: 'http://localhost:5173', api: 'http://localhost:8080' },
    idp: { ui: 'http://localhost:5174', api: 'http://localhost:8081' },
  };
  var config = window.LANDING_CONFIG || {};

  function endpoint(agent, kind) {
    var value = config[agent] && config[agent][kind];
    return (value || defaults[agent][kind]).replace(/\/+$/, '');
  }

  // A no-cors request resolves for any HTTP response and rejects only when the
  // host is unreachable, which is all a reachability badge needs.
  function probe(url) {
    var controller = new AbortController();
    var timer = setTimeout(function () { controller.abort(); }, 4000);
    return fetch(url, { mode: 'no-cors', cache: 'no-store', signal: controller.signal })
      .then(function () { return true; })
      .catch(function () { return false; })
      .finally(function () { clearTimeout(timer); });
  }

  function setBadge(el, label, up) {
    el.classList.remove('checking', 'up', 'down');
    el.classList.add(up ? 'up' : 'down');
    el.textContent = label + (up ? ': running' : ': not reachable');
  }

  var list = document.getElementById('endpoint-list');
  var names = { autonomous: 'Travel agent', idp: 'IDP' };

  Object.keys(defaults).forEach(function (agent) {
    var card = document.querySelector('[data-agent="' + agent + '"]');
    var ui = endpoint(agent, 'ui');
    var api = endpoint(agent, 'api');

    var link = card.querySelector('[data-link="ui"]');
    link.href = ui + '/';

    [['UI', ui], ['backend', api]].forEach(function (pair) {
      var item = document.createElement('li');
      var code = document.createElement('code');
      code.textContent = pair[1];
      item.append(names[agent] + ' ' + pair[0] + ': ', code);
      list.appendChild(item);
    });

    probe(ui + '/').then(function (up) {
      setBadge(card.querySelector('[data-check="ui"]'), 'UI', up);
    });
    // /ping is part of the AgentCore Runtime contract and served by both backends.
    probe(api + '/ping').then(function (up) {
      setBadge(card.querySelector('[data-check="api"]'), 'Backend', up);
    });
  });
})();
