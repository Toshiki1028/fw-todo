
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-priority-picker]').forEach(picker => {
        const select = picker.querySelector('select');
        const trigger = picker.querySelector('.priority-trigger');
        const label = picker.querySelector('[data-priority-label]');
        const menu = picker.querySelector('.priority-menu');
        const options = [...menu.querySelectorAll('[data-priority-value]')];

        const getLabel = () => {
            const selected = select.options[select.selectedIndex];
            return selected ? selected.textContent : '選択してください';
        };

        const closeMenu = (returnFocus = false) => {
            menu.hidden = true;
            trigger.setAttribute('aria-expanded', 'false');
            if (returnFocus) trigger.focus();
        };

        const openMenu = () => {
            menu.hidden = false;
            trigger.setAttribute('aria-expanded', 'true');

            const current = options.find(
                option => option.dataset.priorityValue === select.value
            );
            (current || options[0])?.focus();
        };

        const choose = (value) => {
            select.value = value;
            label.textContent = getLabel();
            options.forEach(option => {
                option.setAttribute(
                    'aria-selected',
                    String(option.dataset.priorityValue === value)
                );
            });
            select.dispatchEvent(new Event('change', { bubbles: true }));
            closeMenu(true);
        };

        label.textContent = getLabel();

        trigger.addEventListener('click', () => {
            if (menu.hidden) {
                openMenu();
            } else {
                closeMenu();
            }
        });

        trigger.addEventListener('keydown', (event) => {
            if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(event.key)) {
                event.preventDefault();
                openMenu();
            }
        });

        options.forEach((option, index) => {
            option.addEventListener('click', () => {
                choose(option.dataset.priorityValue);
            });

            option.addEventListener('keydown', (event) => {
                if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                    event.preventDefault();
                    const step = event.key === 'ArrowDown' ? 1 : -1;
                    options[(index + step + options.length) % options.length].focus();
                } else if (event.key === 'Home') {
                    event.preventDefault();
                    options[0].focus();
                } else if (event.key === 'End') {
                    event.preventDefault();
                    options[options.length - 1].focus();
                } else if (event.key === 'Escape') {
                    event.preventDefault();
                    closeMenu(true);
                }
            });
        });

        document.addEventListener('click', (event) => {
            if (!picker.contains(event.target)) closeMenu();
        });

        picker.addEventListener('focusout', (event) => {
            if (!picker.contains(event.relatedTarget)) closeMenu();
        });

        picker.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !menu.hidden) {
                event.preventDefault();
                closeMenu(true);
            }
        });

        select.addEventListener('change', () => {
            label.textContent = getLabel();
        });
    });
});
