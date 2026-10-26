const teamId = API.queryParam('id');
const actingSelect = document.getElementById('acting');
let team = null;

function render() {
  document.getElementById('team-name').textContent = team.name;
  document.title = team.name + ' - RetroFlow';
  const acting = API.fillMemberSelect(actingSelect, team.members, API.getActingMember(teamId));
  if (acting) API.setActingMember(teamId, acting);
  const list = document.getElementById('members');
  list.replaceChildren();
  team.members.forEach((member) => {
    const li = API.el('li');
    li.appendChild(API.el('span', member.name));
    const actions = API.el('span', undefined, 'row');
    actions.append(
      API.button('Rename', () => renameMember(member), 'secondary'),
      API.button('Remove', () => removeMember(member), 'danger'));
    li.appendChild(actions);
    list.appendChild(li);
  });
}

async function loadTeam() {
  const loaded = await API.guard(() => API.get('/teams/' + encodeURIComponent(teamId)));
  if (loaded) {
    team = loaded;
    render();
  }
}

async function loadRetros() {
  const retros = await API.guard(() => API.get('/teams/' + encodeURIComponent(teamId) + '/retrospectives'));
  if (!retros) return;
  const list = document.getElementById('retros');
  list.replaceChildren();
  retros.forEach((retro) => {
    const li = API.el('li');
    const link = API.el('a', retro.title);
    link.href = '/retro.html?id=' + encodeURIComponent(retro.id);
    const meta = API.el('span', undefined, 'row');
    meta.append(API.el('span', retro.date, 'muted'),
      API.el('span', retro.status, retro.status === 'OPEN' ? 'badge open' : 'badge'));
    li.append(link, meta);
    list.appendChild(li);
  });
  document.getElementById('empty').hidden = retros.length > 0;
}

async function renameMember(member) {
  const name = window.prompt('New name for ' + member.name, member.name);
  if (name === null) return;
  await API.guard(async () => {
    await API.put('/teams/' + teamId + '/members/' + member.id, { name });
    await loadTeam();
  });
}

async function removeMember(member) {
  await API.guard(async () => {
    await API.del('/teams/' + teamId + '/members/' + member.id);
    await loadTeam();
  });
}

actingSelect.addEventListener('change', () => API.setActingMember(teamId, actingSelect.value));

document.getElementById('member-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const name = document.getElementById('member-name').value;
  await API.guard(async () => {
    await API.post('/teams/' + teamId + '/members', { name });
    event.target.reset();
    await loadTeam();
  });
});

document.getElementById('retro-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const title = document.getElementById('retro-title').value;
  await API.guard(async () => {
    await API.post('/teams/' + teamId + '/retrospectives', { title, memberId: actingSelect.value });
    event.target.reset();
    await loadRetros();
  });
});

loadTeam();
loadRetros();
