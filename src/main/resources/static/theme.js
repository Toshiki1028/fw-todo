(function () {
    'use strict';

    let timer;
    let testTheme = null;

    function applyTheme() {
        const now = new Date();
        const hour = now.getHours();
        const night = testTheme === 'night' || (testTheme === null && (hour < 5 || hour >= 17));
        document.documentElement.dataset.theme = night ? 'night' : 'day';
        document.documentElement.style.backgroundColor = night ? '#06152c' : '#7ac5e9';
        document.documentElement.style.colorScheme = 'light';

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

    // Run in <head> before the stylesheet is parsed to avoid a wrong-color first paint.
    applyTheme();
    window.addEventListener('focus', applyTheme);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) applyTheme();
    });
}());
