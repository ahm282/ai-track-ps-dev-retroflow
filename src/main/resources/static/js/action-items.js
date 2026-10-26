async function load() {
  const params = new URLSearchParams();
  const priority = document.getElementById('priority').value;
  const completed = document.getElementById('completed').value;
  if (priority) params.set('priority', priority);
  if (completed) params.set('completed', completed);
  const query = params.toString();
  const items = await API.guard(() => API.get('/action-items' + (query ? '?' + query : '')));
  if (!items) return;
  const list = document.getElementById('items');
  list.replaceChildren();
  items.forEach((item) => {
    const li = API.el('li');
    li.appendChild(API.el('span', item.content, item.completed ? 'done' : undefined));
    const meta = API.el('span', undefined, 'row');
    meta.append(API.el('span', item.priority, 'badge'),
      API.el('span', item.completed ? 'completed' : 'open', 'muted'));
    const link = API.el('a', 'Retrospective');
    link.href = '/retro.html?id=' + encodeURIComponent(item.retrospectiveId);
    meta.appendChild(link);
    li.appendChild(meta);
    list.appendChild(li);
  });
  document.getElementById('empty').hidden = items.length > 0;
}

document.getElementById('filters').addEventListener('submit', (event) => {
  event.preventDefault();
  load();
});

load();
