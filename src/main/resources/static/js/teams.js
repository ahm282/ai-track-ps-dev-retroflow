async function load() {
  const teams = await API.guard(() => API.get('/teams'));
  if (!teams) return;
  const list = document.getElementById('teams');
  list.replaceChildren();
  teams.forEach((team) => {
    const li = API.el('li');
    const link = API.el('a', team.name);
    link.href = '/team.html?id=' + encodeURIComponent(team.id);
    li.append(link, API.el('span', team.members.map((m) => m.name).join(', '), 'muted'));
    list.appendChild(li);
  });
  document.getElementById('empty').hidden = teams.length > 0;
}

document.getElementById('create-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const name = document.getElementById('team-name').value;
  const members = document.getElementById('team-members').value.split(',').map((m) => m.trim()).filter((m) => m);
  const team = await API.guard(() => API.post('/teams', { name, members }));
  if (team) {
    event.target.reset();
    load();
  }
});

load();
