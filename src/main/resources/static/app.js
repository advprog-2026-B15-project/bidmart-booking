const userSelect = document.getElementById("user-id");
const refreshAllBtn = document.getElementById("refresh-all");
const loadBookingsBtn = document.getElementById("load-bookings");
const loadNotificationsBtn = document.getElementById("load-notifications");
const fillRandomBtn = document.getElementById("fill-random");
const clearLogBtn = document.getElementById("clear-log");

const bookingsListEl = document.getElementById("bookings-list");
const notificationsListEl = document.getElementById("notifications-list");
const bookingEmptyEl = document.getElementById("booking-empty");
const bookingDetailEl = document.getElementById("booking-detail");
const apiLogEl = document.getElementById("api-log");
const syncStatusEl = document.getElementById("sync-status");
const eventForm = document.getElementById("event-form");

const detailIdEl = document.getElementById("detail-id");
const detailStatusEl = document.getElementById("detail-status");
const detailAuctionEl = document.getElementById("detail-auction");
const detailListingEl = document.getElementById("detail-listing");
const detailBuyerEl = document.getElementById("detail-buyer");
const detailSellerEl = document.getElementById("detail-seller");
const detailTotalEl = document.getElementById("detail-total");
const detailCreatedEl = document.getElementById("detail-created");
const detailItemsEl = document.getElementById("detail-items");
const detailShipmentEl = document.getElementById("detail-shipment");

const metricBookingsEl = document.getElementById("metric-bookings");
const metricUnreadEl = document.getElementById("metric-unread");
const metricValueEl = document.getElementById("metric-value");

const state = {
    bookings: [],
    notifications: [],
    selectedBookingId: null,
};

refreshAllBtn.addEventListener("click", () => {
    void refreshAll();
});
loadBookingsBtn.addEventListener("click", () => {
    void loadBookings();
});
loadNotificationsBtn.addEventListener("click", () => {
    void loadNotifications();
});
userSelect.addEventListener("change", () => {
    state.selectedBookingId = null;
    void refreshAll();
});
fillRandomBtn.addEventListener("click", fillRandomEvent);
eventForm.addEventListener("submit", (event) => {
    void simulateWinnerEvent(event);
});
clearLogBtn.addEventListener("click", () => {
    apiLogEl.textContent = "Log cleared.";
});

bookingsListEl.addEventListener("click", (event) => {
    const card = event.target.closest("[data-booking-id]");
    if (!card) {
        return;
    }
    const bookingId = Number(card.getAttribute("data-booking-id"));
    void loadBookingDetail(bookingId);
});

notificationsListEl.addEventListener("click", (event) => {
    const button = event.target.closest("[data-notification-id]");
    if (!button) {
        return;
    }
    const notificationId = Number(button.getAttribute("data-notification-id"));
    void markRead(notificationId);
});

function currentUserId() {
    return userSelect.value;
}

function setSyncStatus(message) {
    syncStatusEl.textContent = message;
}

async function apiFetch(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {}),
    };

    const response = await fetch(path, { ...options, headers });
    const text = await response.text();
    const body = text ? JSON.parse(text) : null;

    if (!response.ok) {
        const code = body && body.code ? body.code : "HTTP_" + response.status;
        const message = body && body.message ? body.message : "Request failed";
        throw new Error(code + ": " + message);
    }

    return body;
}

function logApi(label, payload) {
    const entry = "[" + new Date().toISOString() + "] " + label;
    const json = payload ? "\n" + JSON.stringify(payload, null, 2) : "";
    apiLogEl.textContent = entry + json + "\n\n" + apiLogEl.textContent;
}

function formatCurrency(amount, currency) {
    if (typeof amount !== "number") {
        return "-";
    }
    return new Intl.NumberFormat("id-ID", {
        style: "currency",
        currency: currency || "IDR",
        maximumFractionDigits: 0,
    }).format(amount);
}

function formatDate(iso) {
    if (!iso) {
        return "-";
    }
    return new Date(iso).toLocaleString("id-ID");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function renderMetrics() {
    const totalBookings = state.bookings.length;
    const unreadCount = state.notifications.filter((notification) => !notification.isRead).length;
    const totalValue = state.bookings.reduce((sum, booking) => sum + (booking.totalAmount || 0), 0);

    metricBookingsEl.textContent = String(totalBookings);
    metricUnreadEl.textContent = String(unreadCount);
    metricValueEl.textContent = new Intl.NumberFormat("id-ID").format(totalValue);
}

async function refreshAll() {
    setSyncStatus("Syncing...");
    try {
        await Promise.all([loadBookings(), loadNotifications()]);
        setSyncStatus("Synced for " + currentUserId());
    } catch (error) {
        setSyncStatus("Sync failed");
        logApi("Refresh failed", { error: error.message });
    }
}

async function loadBookings() {
    const userId = currentUserId();
    const data = await apiFetch("/api/bookings/me", {
        headers: { "X-User-Id": userId },
    });

    state.bookings = Array.isArray(data) ? data : [];
    renderBookings();
    renderMetrics();
    logApi("Loaded bookings for " + userId, state.bookings);

    if (state.selectedBookingId) {
        const exists = state.bookings.some((booking) => booking.id === state.selectedBookingId);
        if (!exists) {
            state.selectedBookingId = null;
            renderBookingDetail(null);
        }
    }
}

function renderBookings() {
    if (state.bookings.length === 0) {
        bookingsListEl.innerHTML = "<div class=\"empty-state\">No bookings for this user.</div>";
        return;
    }

    bookingsListEl.innerHTML = state.bookings
        .map((booking) => {
            const activeClass = booking.id === state.selectedBookingId ? "active" : "";
            return `
                <article class="booking-card ${activeClass}" data-booking-id="${booking.id}">
                    <div class="card-row">
                        <strong>#${booking.id}</strong>
                        <span class="badge">${escapeHtml(booking.status)}</span>
                    </div>
                    <div class="card-row">
                        <span>Auction</span>
                        <strong>${escapeHtml(booking.auctionId)}</strong>
                    </div>
                    <div class="card-row">
                        <span>Total</span>
                        <strong>${formatCurrency(booking.totalAmount, booking.currency)}</strong>
                    </div>
                    <div class="card-row">
                        <span>Created</span>
                        <strong>${formatDate(booking.createdAt)}</strong>
                    </div>
                </article>
            `;
        })
        .join("");
}

async function loadBookingDetail(bookingId) {
    try {
        const userId = currentUserId();
        const data = await apiFetch("/api/bookings/" + bookingId, {
            headers: { "X-User-Id": userId },
        });
        state.selectedBookingId = bookingId;
        renderBookings();
        renderBookingDetail(data);
        logApi("Loaded booking detail #" + bookingId, data);
    } catch (error) {
        renderBookingDetail(null);
        logApi("Load booking detail failed", { error: error.message });
    }
}

function renderBookingDetail(booking) {
    if (!booking) {
        bookingEmptyEl.hidden = false;
        bookingDetailEl.hidden = true;
        return;
    }

    bookingEmptyEl.hidden = true;
    bookingDetailEl.hidden = false;

    detailIdEl.textContent = String(booking.id);
    detailStatusEl.textContent = booking.status || "-";
    detailAuctionEl.textContent = booking.auctionId || "-";
    detailListingEl.textContent = booking.listingId || "-";
    detailBuyerEl.textContent = booking.buyerUserId || "-";
    detailSellerEl.textContent = booking.sellerUserId || "-";
    detailTotalEl.textContent = formatCurrency(booking.totalAmount, booking.currency);
    detailCreatedEl.textContent = formatDate(booking.createdAt);

    const items = Array.isArray(booking.items) ? booking.items : [];
    if (items.length === 0) {
        detailItemsEl.innerHTML = "<div class=\"empty-state\">No items found.</div>";
    } else {
        detailItemsEl.innerHTML = items
            .map((item) => {
                return `
                    <div class="item-row">
                        <strong>${escapeHtml(item.itemName)}</strong><br>
                        Listing: ${escapeHtml(item.listingId)}<br>
                        Qty: ${item.quantity} · Unit: ${formatCurrency(item.unitPrice, booking.currency)} · Subtotal: ${formatCurrency(item.subtotalAmount, booking.currency)}
                    </div>
                `;
            })
            .join("");
    }

    if (!booking.shipment) {
        detailShipmentEl.textContent = "No shipment data";
    } else {
        detailShipmentEl.innerHTML = `
            <strong>Status:</strong> ${escapeHtml(booking.shipment.status || "-")}<br>
            <strong>Courier:</strong> ${escapeHtml(booking.shipment.courierName || "-")}<br>
            <strong>Tracking:</strong> ${escapeHtml(booking.shipment.trackingNumber || "-")}<br>
            <strong>Shipped:</strong> ${formatDate(booking.shipment.shippedAt)}<br>
            <strong>Delivered:</strong> ${formatDate(booking.shipment.deliveredAt)}
        `;
    }
}

async function loadNotifications() {
    const userId = currentUserId();
    const data = await apiFetch("/api/notifications/me", {
        headers: { "X-User-Id": userId },
    });

    state.notifications = Array.isArray(data) ? data : [];
    renderNotifications();
    renderMetrics();
    logApi("Loaded notifications for " + userId, state.notifications);
}

function renderNotifications() {
    if (state.notifications.length === 0) {
        notificationsListEl.innerHTML = "<div class=\"empty-state\">No notifications.</div>";
        return;
    }

    notificationsListEl.innerHTML = state.notifications
        .map((notification) => {
            const badgeClass = notification.isRead ? "badge read" : "badge warn";
            const action = notification.isRead
                ? "<span class=\"badge read\">Read</span>"
                : `<button class=\"inline-btn\" data-notification-id=\"${notification.id}\">Mark as Read</button>`;

            return `
                <article class="notification-card">
                    <div class="head">
                        <strong>${escapeHtml(notification.title)}</strong>
                        <span class="${badgeClass}">${notification.type}</span>
                    </div>
                    <p>${escapeHtml(notification.message)}</p>
                    <div class="card-row">
                        <small>${formatDate(notification.createdAt)}</small>
                        ${action}
                    </div>
                </article>
            `;
        })
        .join("");
}

async function markRead(notificationId) {
    try {
        const data = await apiFetch("/api/notifications/" + notificationId + "/read", {
            method: "PATCH",
            headers: { "X-User-Id": currentUserId() },
            body: JSON.stringify({ read: true }),
        });
        logApi("Marked notification #" + notificationId + " as read", data);
        await loadNotifications();
    } catch (error) {
        logApi("Mark read failed", { error: error.message });
    }
}

function fillRandomEvent() {
    const now = Date.now();
    document.getElementById("auction-id").value = "auc-ui-" + now;
    document.getElementById("listing-id").value = "lst-ui-" + now;
    document.getElementById("seller-user-id").value = "usr-seller-" + Math.floor(Math.random() * 90 + 10);
    document.getElementById("winner-user-id").value = currentUserId();
    document.getElementById("final-price").value = String(Math.floor(Math.random() * 2500000) + 350000);
    document.getElementById("item-name").value = "Flash Deal Item " + Math.floor(Math.random() * 90 + 10);
    document.getElementById("quantity").value = "1";
}

async function simulateWinnerEvent(event) {
    event.preventDefault();

    const losersRaw = document.getElementById("losers").value.trim();
    const loserUserIds = losersRaw
        ? losersRaw.split(",").map((item) => item.trim()).filter((item) => item.length > 0)
        : [];

    const payload = {
        auctionId: document.getElementById("auction-id").value.trim(),
        listingId: document.getElementById("listing-id").value.trim(),
        sellerUserId: document.getElementById("seller-user-id").value.trim(),
        winnerUserId: document.getElementById("winner-user-id").value.trim(),
        finalPrice: Number(document.getElementById("final-price").value),
        currency: document.getElementById("currency").value.trim() || "IDR",
        itemName: document.getElementById("item-name").value.trim(),
        quantity: Number(document.getElementById("quantity").value),
        loserUserIds,
    };

    try {
        setSyncStatus("Submitting event...");
        const data = await apiFetch("/api/dev/events/winner-determined", {
            method: "POST",
            body: JSON.stringify(payload),
        });
        logApi("Simulated WinnerDetermined", data);
        setSyncStatus("Event accepted");
        await refreshAll();
    } catch (error) {
        setSyncStatus("Event failed");
        logApi("Simulate event failed", { error: error.message });
    }
}

void refreshAll();
