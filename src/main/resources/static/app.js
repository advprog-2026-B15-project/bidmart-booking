const apiBaseUrl = "/api/orders";
const orderForm = document.getElementById("order-form");
const formMessage = document.getElementById("form-message");
const ordersContainer = document.getElementById("orders-container");
const refreshButton = document.getElementById("refresh-button");

orderForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    formMessage.classList.remove("error");
    formMessage.textContent = "Saving order...";

    const formData = new FormData(orderForm);
    const payload = {
        itemName: String(formData.get("itemName") || "").trim(),
        quantity: Number(formData.get("quantity")),
        notes: String(formData.get("notes") || "").trim()
    };

    try {
        const response = await fetch(apiBaseUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "Failed to create order");
        }

        orderForm.reset();
        formMessage.textContent = "Order created.";
        await loadOrders();
    } catch (error) {
        formMessage.classList.add("error");
        formMessage.textContent = error.message;
    }
});

refreshButton.addEventListener("click", async () => {
    await loadOrders();
});

async function loadOrders() {
    ordersContainer.innerHTML = "<p>Loading orders...</p>";
    try {
        const response = await fetch(apiBaseUrl);
        if (!response.ok) {
            throw new Error("Failed to load orders");
        }

        const orders = await response.json();
        renderOrders(orders);
    } catch (error) {
        ordersContainer.innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

function renderOrders(orders) {
    if (!Array.isArray(orders) || orders.length === 0) {
        ordersContainer.innerHTML = "<p class=\"empty-state\">No orders yet.</p>";
        return;
    }

    const sorted = [...orders].sort((a, b) => b.id - a.id);
    ordersContainer.innerHTML = sorted.map((order) => buildOrderCard(order)).join("");

    const updateButtons = document.querySelectorAll("[data-update-status]");
    updateButtons.forEach((button) => {
        button.addEventListener("click", async () => {
            const orderId = button.getAttribute("data-order-id");
            const select = document.querySelector(`[data-status-select="${orderId}"]`);
            await updateOrderStatus(orderId, select.value);
        });
    });
}

function buildOrderCard(order) {
    const createdAt = order.createdAt
        ? new Date(order.createdAt).toLocaleString()
        : "-";

    const notes = order.notes ? escapeHtml(order.notes) : "-";

    return `
        <article class="order-card">
            <div class="order-row">
                <h3>${escapeHtml(order.itemName)} x${order.quantity}</h3>
                <span>#${order.id}</span>
            </div>
            <p class="order-meta">Status: <strong>${escapeHtml(order.status)}</strong></p>
            <p class="order-meta">Created: ${escapeHtml(createdAt)}</p>
            <p class="order-meta">Notes: ${notes}</p>
            <div class="status-tools">
                <select data-status-select="${order.id}">
                    ${buildStatusOption(order.status, "PENDING")}
                    ${buildStatusOption(order.status, "CONFIRMED")}
                    ${buildStatusOption(order.status, "CANCELED")}
                </select>
                <button type="button" data-update-status data-order-id="${order.id}">
                    Update Status
                </button>
            </div>
        </article>
    `;
}

function buildStatusOption(currentStatus, optionStatus) {
    const selected = currentStatus === optionStatus ? "selected" : "";
    return `<option value="${optionStatus}" ${selected}>${optionStatus}</option>`;
}

async function updateOrderStatus(orderId, status) {
    try {
        const response = await fetch(`${apiBaseUrl}/${orderId}/status`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ status })
        });

        if (!response.ok) {
            throw new Error("Failed to update order status");
        }

        await loadOrders();
    } catch (error) {
        alert(error.message);
    }
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

loadOrders();
