(() => {
    const body = document.querySelector('.todo-table tbody');
    if (!body) return;
    let dragged = null;
    let target = null;
    let after = false;

    function clearTarget() {
        target?.classList.remove('todo-drop-before', 'todo-drop-after');
        target = null;
    }

    body.querySelectorAll('.todo-drag-handle').forEach(handle => {
        handle.draggable = true;
        handle.addEventListener('dragstart', event => {
            dragged = handle.closest('tr');
            dragged.classList.add('todo-dragging');
            event.dataTransfer.effectAllowed = 'move';
            event.dataTransfer.setData('text/plain', dragged.dataset.todoId);
        });
        handle.addEventListener('dragend', () => {
            dragged?.classList.remove('todo-dragging');
            clearTarget();
            dragged = null;
        });
    });

    body.addEventListener('dragover', event => {
        const row = event.target.closest('tr[data-todo-id]');
        clearTarget();
        if (!dragged || !row || row === dragged || row.dataset.pinned !== dragged.dataset.pinned) return;
        event.preventDefault();
        event.dataTransfer.dropEffect = 'move';
        target = row;
        after = event.clientY > row.getBoundingClientRect().top + row.offsetHeight / 2;
        row.classList.add(after ? 'todo-drop-after' : 'todo-drop-before');
    });

    body.addEventListener('drop', async event => {
        if (!dragged || !target) return;
        event.preventDefault();
        const destination = target;
        const insertAfter = after;
        clearTarget();
        body.insertBefore(dragged, insertAfter ? destination.nextSibling : destination);
        const query = new URLSearchParams(location.search);
        const form = new URLSearchParams();
        body.querySelectorAll('tr[data-todo-id]').forEach(row => form.append('ids', row.dataset.todoId));
        for (const key of ['keyword', 'category', 'hideCompleted', 'page']) {
            if (query.has(key)) form.set(key, query.get(key));
        }
        try {
            const response = await fetch('/todos/reorder', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: form
            });
            if (!response.ok) throw new Error('並び替えを保存できませんでした');
        } catch (error) {
            alert(error.message);
        } finally {
            location.reload();
        }
    });
})();
