/*
 * 娯楽用の簡易三柱計算。節入り時刻は同梱の fortune-terms.json を参照する。
 * Chujia Kuang, Chinese Calendar Open Datasets v1.1.0 (CC BY 4.0)
 * https://doi.org/10.5281/zenodo.22788686
 */
(function () {
    'use strict';

    const stems = ['甲', '乙', '丙', '丁', '戊', '己', '庚', '辛', '壬', '癸'];
    const branches = ['子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥'];
    const stemElements = [0, 0, 1, 1, 2, 2, 3, 3, 4, 4]; // 木、火、土、金、水
    const branchElements = [4, 2, 0, 0, 2, 1, 1, 2, 3, 3, 2, 4];
    const colors = ['緑', '赤', '黄', '白', '青'];
    const comments = [
        '今日は立ち止まって流れを整える日。急がず身近なことから片づけると、気持ちが軽くなりそう。',
        '小さな工夫が役立つ日。予定に余白を残し、無理のない順番で進めると落ち着いて過ごせそう。',
        '今日は足元を確かめる日。周りの声を聞きつつ、自分のペースを守ると良い流れを作れそう。',
        '新しい一歩を試すのに向く日。準備してきたことを一つ形にすると、次の見通しが立ちそう。',
        '積み重ねが実りやすい日。気になっていたことに手を伸ばすと、思いがけない手応えがありそう。'
    ];

    function mod(value, divisor) {
        return ((value % divisor) + divisor) % divisor;
    }

    function parseDate(value) {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) throw new Error('日付を正しく入力してください。');
        const year = Number(value.slice(0, 4));
        const month = Number(value.slice(5, 7));
        const day = Number(value.slice(8, 10));
        const utc = new Date(Date.UTC(year, month - 1, day));
        if (utc.getUTCFullYear() !== year || utc.getUTCMonth() + 1 !== month || utc.getUTCDate() !== day) {
            throw new Error('存在しない日付です。');
        }
        return { year, month, day, value, epochDay: Date.UTC(year, month - 1, day) / 86400000 };
    }

    function todayInJapan() {
        const parts = new Intl.DateTimeFormat('en-US', {
            timeZone: 'Asia/Tokyo', year: 'numeric', month: '2-digit', day: '2-digit'
        }).formatToParts(new Date());
        const values = Object.fromEntries(parts.map(part => [part.type, part.value]));
        return values.year + '-' + values.month + '-' + values.day;
    }

    function pillar(stemIndex, branchIndex) {
        return { stemIndex, branchIndex, label: stems[stemIndex] + branches[branchIndex] };
    }

    function jieDates(year, termsJst) {
        const values = termsJst[String(year)];
        if (!Array.isArray(values) || values.length !== 12) return [];
        return values.map((value, index) => ({
            date: year + '-' + value.slice(0, 2) + '-' + value.slice(2, 4),
            time: value.slice(4, 6) + ':' + value.slice(6, 8) + ':' + value.slice(8, 10),
            monthIndex: mod(index + 11, 12)
        }));
    }

    function calculatePillars(birthDate, termsJst) {
        const birth = parseDate(birthDate);
        if (birth.value < '1900-01-07' || birth.year > 2052) {
            throw new Error('計算できる生年月日は1900年1月7日から2052年12月31日までです。');
        }
        const currentTerms = jieDates(birth.year, termsJst);
        if (currentTerms.length !== 12) throw new Error('節入りデータを読み込めません。');
        const terms = [...jieDates(birth.year - 1, termsJst), ...currentTerms];
        if (terms.some(term => term.date === birth.value)) {
            throw new Error('節入り当日は出生時刻がないため年柱・月柱を確定できません。');
        }
        const latestTerm = terms.filter(term => term.date < birth.value).at(-1);
        if (!latestTerm) throw new Error('この日付は節入りデータの対応範囲外です。');

        const solarYear = birth.value < currentTerms[1].date ? birth.year - 1 : birth.year;
        const yearStem = mod(solarYear - 1984, 10);
        const yearBranch = mod(solarYear - 1984, 12);
        const monthIndex = latestTerm.monthIndex;
        const monthStem = mod((yearStem % 5) * 2 + 2 + monthIndex, 10);
        const monthBranch = mod(monthIndex + 2, 12);
        // 1949-10-01 は甲子日。日付差を六十干支に巡らせる。
        const dayIndex = mod(birth.epochDay - Date.UTC(1949, 9, 1) / 86400000, 60);
        return {
            year: pillar(yearStem, yearBranch),
            month: pillar(monthStem, monthBranch),
            day: pillar(dayIndex % 10, dayIndex % 12)
        };
    }

    function relation(from, to) {
        if (from === to) return 1;
        if (mod(from + 1, 5) === to || mod(to + 1, 5) === from) return 2; // 相生
        return -2; // 相剋
    }

    function calculateFortune(pillars, todayDate) {
        const today = parseDate(todayDate);
        const todayIndex = mod(today.epochDay - Date.UTC(1949, 9, 1) / 86400000, 60);
        const todayStem = stemElements[todayIndex % 10];
        const todayBranch = branchElements[todayIndex % 12];
        const natalDay = stemElements[pillars.day.stemIndex];
        const score = 2 * relation(natalDay, todayStem)
            + relation(stemElements[pillars.month.stemIndex], todayStem)
            + relation(stemElements[pillars.year.stemIndex], todayStem)
            + relation(branchElements[pillars.day.branchIndex], todayBranch);
        const stars = Math.max(1, Math.min(5, 3 + Math.round(score / 4)));

        let luckyElement = 0;
        let best = -Infinity;
        for (let offset = 0; offset < 5; offset++) {
            const element = (todayIndex + offset) % 5;
            const support = 2 * relation(element, natalDay) + relation(element, todayStem);
            if (support > best) {
                best = support;
                luckyElement = element;
            }
        }
        return { stars, comment: comments[stars - 1], color: colors[luckyElement] };
    }

    window.FortuneCalculator = Object.freeze({ calculatePillars, calculateFortune, todayInJapan, parseDate });
}());
