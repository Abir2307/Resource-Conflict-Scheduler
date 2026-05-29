document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('task-modal');
    const open = document.getElementById('open-task-modal');
    const cancel = document.getElementById('task-cancel');
    const form = document.getElementById('task-modal-form');
    const preferred = document.getElementById('task-preferredStart');
    const deadline = document.getElementById('task-deadline');

    function openModal() {
        // default preferred start to tomorrow 09:00
        const d = new Date();
        d.setDate(d.getDate() + 1);
        d.setHours(9,0,0,0);
        preferred.value = d.toISOString().slice(0,16);
        const dl = new Date(d.getTime());
        dl.setDate(dl.getDate() + 7);
        deadline.value = dl.toISOString().slice(0,16);
        modal.style.display = 'flex';
    }

    function closeModal() { modal.style.display = 'none'; }

    if (open) open.addEventListener('click', openModal);
    if (cancel) cancel.addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });
    
    // Filter assignees by required skills
    const skillsInput = document.getElementById('task-skills');
    const assigneesSelect = document.getElementById('task-assignees');
    const assigneesHidden = document.getElementById('task-assignees-hidden');

    function skillSetFromCsv(csv) {
        return csv.split(',').map(s => s.trim()).filter(s => s.length > 0).map(s => s.toUpperCase());
    }

    function filterAssignees() {
        if (!assigneesSelect) return;
        const required = skillSetFromCsv(skillsInput ? skillsInput.value : '');
        for (const opt of assigneesSelect.options) {
            const avail = (opt.getAttribute('data-skills') || '').split(',').map(s => s.trim()).filter(s => s.length>0).map(s => s.toUpperCase());
            // show option only if every required skill is in avail
            const show = required.every(r => avail.includes(r));
            opt.style.display = show ? '' : 'none';
        }
    }

    if (skillsInput) skillsInput.addEventListener('input', filterAssignees);
    // ensure filter runs on open
    if (open) open.addEventListener('click', () => setTimeout(filterAssignees, 10));

    // On form submit, join selected assignees into hidden CSV field
    if (form) {
        form.addEventListener('submit', (e) => {
            if (!assigneesSelect || !assigneesHidden) return;
            const selected = Array.from(assigneesSelect.selectedOptions).map(o => o.value).join(',');
            assigneesHidden.value = selected;
        });
    }
});