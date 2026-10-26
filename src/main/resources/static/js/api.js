const API = (() => {
  async function request(method, path, body) {
    const options = { method, headers: {} };
    if (body !== undefined) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }
    let response;
    try {
      response = await fetch('/api' + path, options);
    } catch (e) {
      throw new Error('Cannot reach the server');
    }
    if (response.status === 204) return null;
    let data = null;
    try {
      data = await response.json();
    } catch (e) {
      data = null;
    }
    if (!response.ok) {
      throw new Error((data && data.message) || 'Request failed (' + response.status + ')');
    }
    return data;
  }

  function actingKey(teamId) {
    return 'retroflow.actingAs.' + teamId;
  }

  function getActingMember(teamId) {
    try {
      return localStorage.getItem(actingKey(teamId));
    } catch (e) {
      return null;
    }
  }

  function setActingMember(teamId, memberId) {
    try {
      localStorage.setItem(actingKey(teamId), memberId);
    } catch (e) {
      // Storage unavailable: the selection lasts for this page only.
    }
  }

  function queryParam(name) {
    return new URLSearchParams(window.location.search).get(name);
  }

  function showError(message) {
    document.getElementById('error').textContent = message || '';
  }

  async function guard(action) {
    showError('');
    try {
      return await action();
    } catch (e) {
      showError(e.message);
      return undefined;
    }
  }

  function el(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = text;
    if (className) node.className = className;
    return node;
  }

  function button(label, onClick, className) {
    const node = el('button', label, className);
    node.type = 'button';
    node.addEventListener('click', onClick);
    return node;
  }

  function fillMemberSelect(select, members, selectedId) {
    select.replaceChildren();
    members.forEach((m) => {
      const option = el('option', m.name);
      option.value = m.id;
      select.appendChild(option);
    });
    if (members.some((m) => m.id === selectedId)) select.value = selectedId;
    return select.value || null;
  }

  return {
    get: (path) => request('GET', path),
    post: (path, body) => request('POST', path, body),
    put: (path, body) => request('PUT', path, body),
    del: (path) => request('DELETE', path),
    getActingMember, setActingMember, queryParam, showError, guard, el, button, fillMemberSelect,
  };
})();
