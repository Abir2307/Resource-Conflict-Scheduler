document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('asset-modal');
    const form = document.getElementById('asset-modal-form');
    const title = document.getElementById('asset-modal-title');
    const cancel = document.getElementById('asset-cancel');

    const fields = {
        id: document.getElementById('asset-id'),
        name: document.getElementById('asset-name'),
        category: document.getElementById('asset-category'),
        quantity: document.getElementById('asset-quantity'),
        unit: document.getElementById('asset-unit'),
        location: document.getElementById('asset-location'),
        notes: document.getElementById('asset-notes'),
        dependencies: document.getElementById('asset-dependencies'),
        cost: document.getElementById('asset-cost'),
        eta: document.getElementById('asset-eta'),
        supplier: document.getElementById('asset-supplier')
    };

    function openModal(prefill) {
        if (prefill) {
            title.textContent = 'Edit Asset';
            fields.id.value = prefill.id || '';
            fields.name.value = prefill.name || '';
            fields.category.value = prefill.category || 'EQUIPMENT';
            fields.quantity.value = prefill.quantity || '';
            fields.unit.value = prefill.unit || '';
            fields.location.value = prefill.location || '';
            fields.notes.value = prefill.notes || '';
            fields.dependencies.value = (prefill.dependencies || '').replace(/^\[|\]$/g, '');
            fields.cost.value = prefill.cost || '';
            fields.eta.value = prefill.eta || '';
            fields.supplier.value = prefill.supplier || '';
        } else {
            title.textContent = 'Add Asset';
            form.reset();
            fields.id.value = '';
            fields.category.value = 'EQUIPMENT';
        }
        modal.style.display = 'flex';
    }

    function closeModal() {
        modal.style.display = 'none';
    }

    cancel.addEventListener('click', () => closeModal());
    modal.addEventListener('click', (e) => {
        if (e.target === modal || e.target.classList.contains('asset-modal-backdrop')) closeModal();
    });

    // Add button
    const addButton = document.getElementById('add-asset-button');
    if (addButton) {
        addButton.addEventListener('click', () => openModal(null));
    }

    // Edit buttons - read data attributes from the row
    document.querySelectorAll('.open-asset-modal').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const tr = e.target.closest('tr');
            if (!tr) return;
            const data = {
                id: tr.getAttribute('data-id') || '',
                name: tr.getAttribute('data-name') || '',
                category: tr.getAttribute('data-category') || 'EQUIPMENT',
                quantity: tr.getAttribute('data-quantity') || '',
                unit: tr.getAttribute('data-unit') || '',
                cost: tr.getAttribute('data-cost') || '',
                eta: tr.getAttribute('data-eta') || '',
                supplier: tr.getAttribute('data-supplier') || '',
                dependencies: tr.getAttribute('data-dependencies') || '',
                location: tr.getAttribute('data-location') || '',
                notes: tr.getAttribute('data-notes') || ''
            };
            openModal(data);
        });
    });
});
