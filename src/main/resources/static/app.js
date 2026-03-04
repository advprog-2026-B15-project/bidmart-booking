const userSelect = document.getElementById("user-id");
const loadBookingsBtn = document.getElementById("load-bookings");
const loadNotificationsBtn = document.getElementById("load-notifications");
const bookingsContainer = document.getElementById("bookings");
const notificationsContainer = document.getElementById("notifications");
const bookingDetailEl = document.getElementById("booking-detail");
const apiLogEl = document.getElementById("api-log");
const eventForm = document.getElementById("event-form");

loadBookingsBtn.addEventListener("click", loadBookings);
loadNotificationsBtn.addEventListener("click", loadNotifications);
eventForm.addEventListener("submit", simulateWinnerEvent);

function currentUserId() {
    return userSelect.value;
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

function logApi(message, payload) {
    const payloadText = payload ? "\n" + JSON.stringify(payload, null, 2) : "";
    apiLogEl.textContent = "[" + new Date().toISOString() + "] " + message + payloadText;
}

async function loadBookings() {
    try {
        const userId = currentUserId();
        const data = await apiFetch("/api/bookings/me", {
            headers: { "X-User-Id": userId },
        });
        renderBookings(data);
        logApi("Loaded bookings for " + userId, data);
    } catch (error) {
        bookingsContainer.innerHTML = "<p>Error: " + error.message + "</p>";
        logApi("Load bookings failed", { error: error.message });
    }
}

function renderBookings(bookings) {
    if (!Array.isArray(bookings) || bookings.length === 0) {
        bookingsContainer.innerHTML = "<p>No bookings found for selected user.</p>";
        return;
    }

    const rows = bookings
        .map((booking) => {
            return `
                <tr>
                    <td>${booking.id}</td>
                    <td>${booking.auctionId}</td>
                    <td>${booking.status}</td>
                    <td>${booking.totalAmount} ${booking.currency}</td>
                    <td>
                        <button class="inline-btn" onclick="loadBookingDetail(${booking.id})">
                            View
                        </button>
                    </td>
                </tr>
            `;
        })
        .join("");

    bookingsContainer.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Auction</th>
                    <th>Status</th>
                    <th>Total</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>
    `;
}

window.loadBookingDetail = async function loadBookingDetail(bookingId) {
    try {
        const userId = currentUserId();
        const data = await apiFetch("/api/bookings/" + bookingId, {
            headers: { "X-User-Id": userId },
        });
        bookingDetailEl.textContent = JSON.stringify(data, null, 2);
        logApi("Loaded booking detail " + bookingId, data);
    } catch (error) {
        bookingDetailEl.textContent = "Error: " + error.message;
        logApi("Load booking detail failed", { error: error.message });
    }
};

async function loadNotifications() {
    try {
        const userId = currentUserId();
        const data = await apiFetch("/api/notifications/me", {
            headers: { "X-User-Id": userId },
        });
        renderNotifications(data);
        logApi("Loaded notifications for " + userId, data);
    } catch (error) {
        notificationsContainer.innerHTML = "<p>Error: " + error.message + "</p>";
        logApi("Load notifications failed", { error: error.message });
    }
}

function renderNotifications(notifications) {
    if (!Array.isArray(notifications) || notifications.length === 0) {
        notificationsContainer.innerHTML = "<p>No notifications.</p>";
        return;
    }

    const rows = notifications
        .map((notif) => {
            const button = notif.isRead
                ? "<span>Read</span>"
                : `<button class="inline-btn" onclick="markRead(${notif.id})">Mark Read</button>`;
            return `
                <tr>
                    <td>${notif.id}</td>
                    <td>${notif.type}</td>
                    <td>${notif.title}</td>
                    <td>${notif.isRead}</td>
                    <td>${button}</td>
                </tr>
            `;
        })
        .join("");

    notificationsContainer.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Type</th>
                    <th>Title</th>
                    <th>isRead</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>
    `;
}

window.markRead = async function markRead(notificationId) {
    try {
        const userId = currentUserId();
        const data = await apiFetch("/api/notifications/" + notificationId + "/read", {
            method: "PATCH",
            headers: { "X-User-Id": userId },
            body: JSON.stringify({ read: true }),
        });
        logApi("Marked notification as read: " + notificationId, data);
        await loadNotifications();
    } catch (error) {
        logApi("Mark read failed", { error: error.message });
    }
};

async function simulateWinnerEvent(event) {
    event.preventDefault();

    const losersRaw = document.getElementById("losers").value.trim();
    const loserUserIds = losersRaw
        ? losersRaw.split(",").map((it) => it.trim()).filter((it) => it.length > 0)
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
        const data = await apiFetch("/api/dev/events/winner-determined", {
            method: "POST",
            body: JSON.stringify(payload),
        });
        logApi("Simulated WinnerDetermined event", data);
        await loadBookings();
        await loadNotifications();
    } catch (error) {
        logApi("Simulate event failed", { error: error.message });
    }
}

loadBookings();
loadNotifications();
