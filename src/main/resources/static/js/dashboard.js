(function () {
    function text(value) {
        return value === null || value === undefined ? '' : String(value);
    }

    function csvFromValues(values) {
        if (!Array.isArray(values)) {
            return text(values);
        }
        return values.map((item) => {
            if (typeof item === 'string') {
                return item;
            }
            if (item && typeof item === 'object') {
                return item.name || item.id || JSON.stringify(item);
            }
            return text(item);
        }).join(', ');
    }

    function renderResourceRows(items) {
        const tbody = document.getElementById('resources-body');
        if (!tbody) return;
        tbody.innerHTML = '';
        items.forEach((item) => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${text(item.id)}</td>
                <td>${text(item.name)}</td>
                <td>${csvFromValues(item.skills)}</td>
                <td>${text(item.maxWorkloadHours)}</td>
                <td>${text(item.location)}</td>
                <td>
                    <a href="/resources/edit/${encodeURIComponent(item.id)}">Edit</a>
                    <button type="button" class="delete-resource" data-id="${text(item.id)}">Delete</button>
                </td>`;
            tbody.appendChild(row);
        });
    }

    function renderTaskRows(items) {
        const tbody = document.getElementById('tasks-body');
        if (!tbody) return;
        tbody.innerHTML = '';
        items.forEach((item) => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${text(item.id)}</td>
                <td>${text(item.projectId)}</td>
                <td>${text(item.title)}</td>
                <td>${text(item.duration)}</td>
                <td>${csvFromValues(item.requiredSkills)}</td>
                <td>${text(item.priority)}</td>
                <td>
                    <a href="/tasks/edit/${encodeURIComponent(item.id)}">Edit</a>
                    <button type="button" class="delete-task" data-id="${text(item.id)}">Delete</button>
                </td>`;
            tbody.appendChild(row);
        });
    }

    async function refreshResources() {
        const response = await fetch('/api/scheduler/resources');
        if (!response.ok) return;
        renderResourceRows(await response.json());
    }

    async function refreshTasks() {
        const response = await fetch('/api/scheduler/tasks');
        if (!response.ok) return;
        renderTaskRows(await response.json());
    }

    document.addEventListener('click', async (event) => {
        const resourceId = event.target?.dataset?.id;
        if (event.target?.classList?.contains('delete-resource') && resourceId) {
            await fetch(`/api/scheduler/resources/${encodeURIComponent(resourceId)}`, { method: 'DELETE' });
            await refreshResources();
        }
        if (event.target?.classList?.contains('delete-task') && resourceId) {
            await fetch(`/api/scheduler/tasks/${encodeURIComponent(resourceId)}`, { method: 'DELETE' });
            await refreshTasks();
        }
    });

    document.addEventListener('DOMContentLoaded', () => {
        const resourceRefresh = document.getElementById('refresh-resources');
        if (resourceRefresh) {
            resourceRefresh.addEventListener('click', refreshResources);
            refreshResources();
        }

        const taskRefresh = document.getElementById('refresh-tasks');
        if (taskRefresh) {
            taskRefresh.addEventListener('click', refreshTasks);
            refreshTasks();
        }
    });
})();
