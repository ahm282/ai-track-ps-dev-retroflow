const retroId = API.queryParam('id');
const actingSelect = document.getElementById('acting');
const typeSelect = document.getElementById('type');
const TYPES = ['WENT_WELL', 'NEEDS_IMPROVEMENT', 'ACTION_ITEM'];
let retro = null;
let team = null;

function renderRetro() {
  const open = retro.status === 'OPEN';
  document.getElementById('title').textContent = retro.title;
  document.title = retro.title + ' - RetroFlow';
  document.getElementById('date').textContent = retro.date;
  const status = document.getElementById('status');
  status.textContent = retro.status;
  status.className = open ? 'badge open' : 'badge';
  document.getElementById('back').href = '/team.html?id=' + encodeURIComponent(retro.teamId);
  document.getElementById('controls').hidden = !open;
  document.getElementById('add-card').hidden = !open;
}

function renderItems(items) {
  const open = retro.status === 'OPEN';
  TYPES.forEach((type) => {
    const column = document.getElementById('col-' + type);
    column.replaceChildren();
    const inColumn = items.filter((item) => item.type === type);
    if (inColumn.length === 0) column.appendChild(API.el('p', 'Nothing yet.', 'muted'));
    inColumn.forEach((item) => {
      const node = API.el('div', undefined, 'item');
      node.appendChild(API.el('p', item.content, item.completed ? 'done' : undefined));
      let meta = 'by ' + item.submitterName;
      if (type === 'ACTION_ITEM') meta += ' | ' + item.priority + ' | ' + (item.completed ? 'completed' : 'open');
      node.appendChild(API.el('p', meta, 'muted'));
      if (open) {
        const actions = API.el('div', undefined, 'row');
        actions.appendChild(API.button('Edit', () => editItem(item), 'secondary'));
        if (type === 'ACTION_ITEM' && !item.completed) {
          actions.appendChild(API.button('Complete', () => completeItem(item), 'secondary'));
        }
        actions.appendChild(API.button('Delete', () => deleteItem(item), 'danger'));
        node.appendChild(actions);
      }
      column.appendChild(node);
    });
  });
}

async function loadItems() {
  const items = await API.guard(() => API.get('/retrospectives/' + encodeURIComponent(retroId) + '/feedback'));
  if (items) renderItems(items);
}

async function reload() {
  const loaded = await API.guard(() => API.get('/retrospectives/' + encodeURIComponent(retroId)));
  if (!loaded) return;
  retro = loaded;
  renderRetro();
  if (!team) {
    team = await API.guard(() => API.get('/teams/' + retro.teamId));
    if (team) {
      const acting = API.fillMemberSelect(actingSelect, team.members, API.getActingMember(retro.teamId));
      if (acting) API.setActingMember(retro.teamId, acting);
    }
  }
  await loadItems();
}

async function editItem(item) {
  const content = window.prompt('Edit content', item.content);
  if (content === null) return;
  let fields = {};
  if (item.type === 'ACTION_ITEM') {
    const priority = window.prompt('Priority (HIGH, MEDIUM, LOW)', item.priority);
    if (priority === null) return;
    fields = { priority: priority.trim().toUpperCase(), completed: item.completed };
  }
  await API.guard(async () => {
    await API.put('/feedback/' + item.id, { content, ...fields });
    await loadItems();
  });
}

async function completeItem(item) {
  await API.guard(async () => {
    await API.put('/action-items/' + item.id + '/complete');
    await loadItems();
  });
}

async function deleteItem(item) {
  await API.guard(async () => {
    await API.del('/feedback/' + item.id);
    await loadItems();
  });
}

actingSelect.addEventListener('change', () => API.setActingMember(retro.teamId, actingSelect.value));

typeSelect.addEventListener('change', () => {
  document.getElementById('priority-wrap').hidden = typeSelect.value !== 'ACTION_ITEM';
});

document.getElementById('add-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const body = {
    content: document.getElementById('content').value,
    type: typeSelect.value,
    submitterId: actingSelect.value,
  };
  if (body.type === 'ACTION_ITEM') body.priority = document.getElementById('priority').value;
  await API.guard(async () => {
    await API.post('/retrospectives/' + encodeURIComponent(retroId) + '/feedback', body);
    document.getElementById('content').value = '';
    await loadItems();
  });
});

document.getElementById('close').addEventListener('click', async () => {
  await API.guard(async () => {
    await API.put('/retrospectives/' + encodeURIComponent(retroId) + '/close', { memberId: actingSelect.value });
    await reload();
  });
});

reload();
