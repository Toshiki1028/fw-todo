(function () {
    'use strict';

    let timer;
    let testTheme = null;
    const storageKey = 'todo.wallpapers.v1';
    const wallpaperPath = '/images/wallpapers/';
    const wallpapers = [
        ['day-default.jpg', '昼のデフォルト'],
        ['night-default.jpg', '夜のデフォルト'],
        ['cyber.png', 'サイバー'],
        ['nature.jpg', '自然'],
        ['ocean.jpg', '海中'],
        ['library.jpg', '図書館']
    ];
    const validNames = new Set(wallpapers.map(([name]) => name));
    const defaults = { day: 'day-default.jpg', night: 'night-default.jpg' };

    function readWallpapers() {
        try {
            const saved = JSON.parse(localStorage.getItem(storageKey) || '{}');
            return {
                day: validNames.has(saved?.day) ? saved.day : defaults.day,
                night: validNames.has(saved?.night) ? saved.night : defaults.night
            };
        } catch (_) {
            return { ...defaults };
        }
    }

    let selected = readWallpapers();

    function applyWallpaper() {
        const period = document.documentElement.dataset.theme === 'night' ? 'night' : 'day';
        document.documentElement.style.setProperty('--wallpaper-image',
            `url("${wallpaperPath}${selected[period]}")`);
    }

    function applyTheme() {
        const now = new Date();
        const hour = now.getHours();
        const night = testTheme === 'night' || (testTheme === null && (hour < 5 || hour >= 17));
        document.documentElement.dataset.theme = night ? 'night' : 'day';
        document.documentElement.style.backgroundColor = night ? '#06152c' : '#7ac5e9';
        document.documentElement.style.colorScheme = 'light';
        applyWallpaper();

        clearTimeout(timer);
        if (testTheme !== null) return;

        const next = new Date(now);
        if (hour < 5) {
            next.setHours(5, 0, 0, 0);
        } else if (hour < 17) {
            next.setHours(17, 0, 0, 0);
        } else {
            next.setDate(next.getDate() + 1);
            next.setHours(5, 0, 0, 0);
        }

        timer = setTimeout(applyTheme, Math.max(1, next - now));
    }

    // Console-only preview. It is kept in memory and never changes saved settings.
    window.todoThemeTest = Object.freeze({
        forceNight: function () {
            testTheme = 'night';
            applyTheme();
        },
        reset: function () {
            testTheme = null;
            applyTheme();
        }
    });

    function setupWallpaperPicker() {
        const openButton = document.querySelector('[data-wallpaper-open]');
        if (!openButton) return;

        const dialog = document.createElement('dialog');
        dialog.className = 'wallpaper-dialog';
        dialog.setAttribute('aria-label', '壁紙を選択');
        dialog.innerHTML = '<div class="wallpaper-dialog-header"><strong>壁紙を選択</strong>'
            + '<button type="button" class="wallpaper-close" aria-label="閉じる">×</button></div>'
            + '<div class="wallpaper-grid"></div>'
            + '<div class="wallpaper-assign" hidden>'
            + '<button type="button" class="btn" data-wallpaper-period="day">☀ 朝・昼に設定</button>'
            + '<button type="button" class="btn sub" data-wallpaper-period="night">☾ 夕方・夜に設定</button>'
            + '</div>';
        document.body.append(dialog);

        const grid = dialog.querySelector('.wallpaper-grid');
        const assign = dialog.querySelector('.wallpaper-assign');
        let chosenName = null;
        for (const [name, label] of wallpapers) {
            const card = document.createElement('button');
            card.type = 'button';
            card.className = 'wallpaper-card';
            card.dataset.wallpaper = name;
            card.setAttribute('aria-label', `${label}の壁紙を選択`);
            const img = document.createElement('img');
            img.src = wallpaperPath + name;
            img.alt = '';
            const badges = document.createElement('span');
            badges.className = 'wallpaper-badges';
            card.append(img, badges);
            grid.append(card);
            card.addEventListener('click', () => {
                chosenName = name;
                assign.hidden = false;
                renderSelection();
                assign.querySelector('button').focus();
            });
        }

        function renderSelection() {
            grid.querySelectorAll('.wallpaper-card').forEach(card => {
                const name = card.dataset.wallpaper;
                const day = selected.day === name;
                const night = selected.night === name;
                card.classList.toggle('is-day', day);
                card.classList.toggle('is-night', night);
                card.classList.toggle('is-both', day && night);
                card.classList.toggle('is-chosen', chosenName === name);
                const badges = card.querySelector('.wallpaper-badges');
                badges.replaceChildren();
                if (day) {
                    const badge = document.createElement('span');
                    badge.className = 'wallpaper-badge wallpaper-badge-day';
                    badge.textContent = '☀ 朝・昼 選択中';
                    badges.append(badge);
                }
                if (night) {
                    const badge = document.createElement('span');
                    badge.className = 'wallpaper-badge wallpaper-badge-night';
                    badge.textContent = '☾ 夕方・夜 選択中';
                    badges.append(badge);
                }
            });
        }

        openButton.addEventListener('click', () => {
            selected = readWallpapers();
            chosenName = null;
            assign.hidden = true;
            renderSelection();
            dialog.showModal();
        });
        dialog.querySelector('.wallpaper-close').addEventListener('click', () => dialog.close());
        dialog.addEventListener('click', event => {
            if (event.target !== dialog) return;
            const bounds = dialog.getBoundingClientRect();
            if (event.clientX < bounds.left || event.clientX > bounds.right
                || event.clientY < bounds.top || event.clientY > bounds.bottom) dialog.close();
        });
        assign.querySelectorAll('[data-wallpaper-period]').forEach(button => {
            button.addEventListener('click', () => {
                selected[button.dataset.wallpaperPeriod] = chosenName;
                try { localStorage.setItem(storageKey, JSON.stringify(selected)); } catch (_) { /* storage unavailable */ }
                applyWallpaper();
                renderSelection();
            });
        });
        window.addEventListener('storage', event => {
            if (event.key !== storageKey) return;
            selected = readWallpapers();
            applyWallpaper();
            renderSelection();
        });
    }

    // Run in <head> before the stylesheet is parsed to avoid a wrong-color first paint.
    applyTheme();
    window.addEventListener('focus', applyTheme);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) applyTheme();
    });
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setupWallpaperPicker);
    } else {
        setupWallpaperPicker();
    }
}());
