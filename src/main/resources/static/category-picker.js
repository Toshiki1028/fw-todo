(function () {
    'use strict';

    const picker = document.querySelector('[data-category-picker]');
    if (!picker) return;

    const selected = picker.querySelector('#category');
    const label = picker.querySelector('[data-category-label]');
    const trigger = picker.querySelector('.category-trigger');
    const menu = picker.querySelector('.category-menu');
    const list = picker.querySelector('[data-category-list]');
    const addButton = picker.querySelector('[data-category-add]');
    const message = picker.querySelector('[data-category-message]');

    function showMessage(text) {
        message.textContent = text;
        message.hidden = !text;
    }

    function choose(name) {
        selected.value = name;
        label.textContent = name || '選択してください';
        menu.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        trigger.focus();
    }

    async function request(path, method, name) {
        const options = { method, headers: { Accept: 'application/json' } };
        if (name !== undefined) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify({ name });
        }
        const response = await fetch(path, options);
        const data = response.status === 204 ? null : await response.json();
        if (!response.ok) throw new Error(data?.message || 'ジャンルを更新できませんでした');
        return data;
    }

    function makeButton(text, action, ariaLabel) {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = text;
        button.dataset[action] = '';
        if (ariaLabel) button.setAttribute('aria-label', ariaLabel);
        return button;
    }

    function renderRow(row) {
        const name = row.dataset.categoryName;
        row.replaceChildren(
            makeButton(name, 'categorySelect'),
            makeButton('✎', 'categoryEdit', name + 'の名前を変更'),
            makeButton('×', 'categoryDelete', name + 'を削除')
        );
    }

    function editInRow(row, initialName, save, cancel) {
        const editor = document.createElement('div');
        editor.className = 'category-editor';
        const input = document.createElement('input');
        input.type = 'text';
        input.maxLength = 255;
        input.setAttribute('aria-label', 'ジャンル名');
        input.value = initialName;
        const saveButton = makeButton('保存', 'categorySave');
        const cancelButton = makeButton('キャンセル', 'categoryCancel');
        editor.append(input, saveButton, cancelButton);
        row.replaceChildren(editor);
        input.focus();
        input.select();

        saveButton.addEventListener('click', async function () {
            const name = input.value.trim();
            if (!name) {
                showMessage('ジャンル名を入力してください');
                input.focus();
                return;
            }
            showMessage('');
            saveButton.disabled = true;
            try {
                await save(name);
            } catch (error) {
                showMessage(error.message || 'ジャンルを更新できませんでした');
                saveButton.disabled = false;
            }
        });
        cancelButton.addEventListener('click', cancel);
        input.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                saveButton.click();
            } else if (event.key === 'Escape') {
                event.stopPropagation();
                cancel();
            }
        });
    }

    trigger.addEventListener('click', function () {
        menu.hidden = !menu.hidden;
        trigger.setAttribute('aria-expanded', String(!menu.hidden));
        if (!menu.hidden) showMessage('');
    });

    addButton.addEventListener('click', function () {
        showMessage('');
        addButton.hidden = true;
        const row = document.createElement('div');
        row.className = 'category-option';
        list.prepend(row);
        const cancel = function () {
            row.remove();
            addButton.hidden = false;
            showMessage('');
            addButton.focus();
        };
        editInRow(row, '', async function (name) {
            const category = await request('/api/categories', 'POST', name);
            row.dataset.categoryId = category.id;
            row.dataset.categoryName = category.name;
            renderRow(row);
            addButton.hidden = false;
            choose(category.name);
        }, cancel);
    });

    list.addEventListener('click', async function (event) {
        const button = event.target.closest('button');
        const row = button?.closest('.category-option');
        if (!row || !row.dataset.categoryId) return;
        const oldName = row.dataset.categoryName;
        const id = encodeURIComponent(row.dataset.categoryId);
        if (button.hasAttribute('data-category-select')) {
            choose(oldName);
        } else if (button.hasAttribute('data-category-edit')) {
            showMessage('');
            editInRow(row, oldName, async function (name) {
                const category = await request('/api/categories/' + id, 'PUT', name);
                row.dataset.categoryName = category.name;
                renderRow(row);
                if (selected.value === oldName) {
                    selected.value = category.name;
                    label.textContent = category.name;
                }
            }, function () {
                renderRow(row);
                showMessage('');
            });
        } else if (button.hasAttribute('data-category-delete')) {
            if (!window.confirm('「' + oldName + '」を削除しますか？')) return;
            showMessage('');
            button.disabled = true;
            try {
                await request('/api/categories/' + id, 'DELETE');
                row.remove();
                if (selected.value === oldName) {
                    selected.value = '';
                    label.textContent = '選択してください';
                }
            } catch (error) {
                showMessage(error.message || 'ジャンルを削除できませんでした');
                button.disabled = false;
            }
        }
    });

    document.addEventListener('click', function (event) {
        if (!picker.contains(event.target)) {
            menu.hidden = true;
            trigger.setAttribute('aria-expanded', 'false');
        }
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !menu.hidden) {
            menu.hidden = true;
            trigger.setAttribute('aria-expanded', 'false');
            trigger.focus();
        }
    });
}());
