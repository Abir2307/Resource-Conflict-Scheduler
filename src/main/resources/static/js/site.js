document.addEventListener('DOMContentLoaded', () => {
    const pathname = window.location.pathname;
    if (pathname === '/login') {
        window.location.hash = '#login-modal';
    } else if (pathname === '/register') {
        window.location.hash = '#register-modal';
    }

    const syncModals = () => {
        const hash = window.location.hash;
        document.querySelectorAll('.auth-modal').forEach((modal) => {
            const shouldOpen = hash && `#${modal.id}` === hash;
            modal.classList.toggle('is-open', shouldOpen);
        });
    };

    syncModals();
    window.addEventListener('hashchange', syncModals);

    document.querySelectorAll('.auth-modal').forEach((modal) => {
        modal.addEventListener('click', (event) => {
            if (event.target === modal) {
                window.location.hash = '';
            }
        });
    });
});